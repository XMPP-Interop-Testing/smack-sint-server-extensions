/*
 * Copyright 2024 Guus der Kinderen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.igniterealtime.smack.inttest.util;

import org.igniterealtime.smack.inttest.FailedTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework;
import org.igniterealtime.smack.inttest.TestNotPossible;
import org.igniterealtime.smack.inttest.TestResult;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a JUnit-compatible XML file based on the test run results.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="https://github.com/testmoapp/junitxml">https://github.com/testmoapp/junitxml</a>
 */
public class JUnitXmlTestRunResultProcessor implements SmackIntegrationTestFramework.TestRunResultProcessor {

    private final Properties specifications;
    private final Path logFile;

    public JUnitXmlTestRunResultProcessor() throws IOException
    {
        final Path logDirPath = StdOutTestRunResultProcessor.getLogFromSmackDebuggerConfig(System.getProperty("sinttest.debugger"));
        this.logFile = logDirPath.resolve("test-results.xml");
        System.out.println("Saving JUnit-compatible XML file with results to " + logFile.toAbsolutePath());

        specifications = new Properties();
        specifications.load(JUnitXmlTestRunResultProcessor.class.getResourceAsStream("/specifications.properties"));
    }
    @Override
    public void process(SmackIntegrationTestFramework.TestRunResult testRunResult)
    {
        // TODO Consider splitting up 'failures' in 'failures' and 'errors', by determining if the corresponding Throwable inherits from AssertionError or not.
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();

            // <testsuites> Usually the root element of a JUnit XML file. Some tools leave out
            // the <testsuites> element if there is only a single top-level <testsuite> element (which
            // is then used as the root element).
            //
            // name        Name of the entire test run
            // tests       Total number of tests in this file
            // failures    Total number of failed tests in this file
            // errors      Total number of errored tests in this file
            // skipped     Total number of skipped tests in this file
            // assertions  Total number of assertions for all tests in this file
            // time        Aggregated time of all tests in this file in seconds
            // timestamp   Date and time of when the test run was executed (in ISO 8601 format)
            final Element rootElement = doc.createElement("testsuites");
            rootElement.setAttribute("name", "XMPP specification test run with ID " + testRunResult.getTestRunId());
            rootElement.setAttribute("tests", String.valueOf(testRunResult.getNumberOfAvailableTests()));
            rootElement.setAttribute("failures", String.valueOf(testRunResult.getFailedTests().size()));
            rootElement.setAttribute("skipped", String.valueOf(testRunResult.getNotPossibleTests().size()));
            rootElement.setAttribute("time", String.valueOf(getAggregatedTime(testRunResult).toMillis() / 1000.0));
            rootElement.setAttribute("timestamp", Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) );
            doc.appendChild(rootElement);

            final Map<String, List<TestResult>> testResultsBySpecification = aggregateTestResultsBySpecification(testRunResult);
            for (final Map.Entry<String, List<TestResult>> entry : testResultsBySpecification.entrySet()) {
                // <testsuite> A test suite usually represents a class, folder or group of tests.
                // There can be many test suites in an XML file, and there can be test suites under other
                // test suites.
                //
                // name        Name of the test suite (e.g. class name or folder name)
                // tests       Total number of tests in this suite
                // failures    Total number of failed tests in this suite
                // errors      Total number of errored tests in this suite
                // skipped     Total number of skipped tests in this suite
                // assertions  Total number of assertions for all tests in this suite
                // time        Aggregated time of all tests in this file in seconds
                // timestamp   Date and time of when the test suite was executed (in ISO 8601 format)
                // file        Source code file of this test suite
                final String specification = entry.getKey();
                final String name;
                final String title;
                if (specification == null || specification.isBlank()) {
                    name = "Without Specification Reference";
                    title = null;
                } else {
                    title = specifications.getProperty(specification);
                    if (title == null) {
                        name = specification;
                    } else {
                        name = specification + ": " + title;
                    }
                }
                final Collection<TestResult> testResults = entry.getValue();
                final long failedTestCount = testResults.stream().filter(testResult -> testResult instanceof FailedTest).count();
                final long notPossibleTestCount = testResults.stream().filter(testResult -> testResult instanceof TestNotPossible).count();

                final Element testsuiteElement = doc.createElement("testsuite");
                testsuiteElement.setAttribute("name", name);
                testsuiteElement.setAttribute("tests", String.valueOf(testResults.size()));
                testsuiteElement.setAttribute("failures", String.valueOf(failedTestCount));
                testsuiteElement.setAttribute("skipped", String.valueOf(notPossibleTestCount));
                testsuiteElement.setAttribute("time", String.valueOf(getAggregatedTime(testResults).toMillis() / 1000.0));
                rootElement.appendChild(testsuiteElement);

                for (final TestResult testResult : testResults) {
                    // <testcase> There are one or more test cases in a test suite. A test passed
                    // if there isn't an additional result element (skipped, failure, error).
                    //
                    // name        The name of this test case, often the method name
                    // classname   The name of the parent class/folder, often the same as the suite's name
                    // assertions  Number of assertions checked during test case execution
                    // time        Execution time of the test in seconds
                    // file        Source code file of this test case
                    // line        Source code line number of the start of this test case
                    final Element testcaseElement = doc.createElement("testcase");
                    testcaseElement.setAttribute("name", testResult.concreteTest.toString());
                    testcaseElement.setAttribute("classname", testResult.concreteTest.getMethod().getDeclaringClass().getName());
                    testcaseElement.setAttribute("time", String.valueOf(testResult.duration.getSeconds()));
                    if (testResult instanceof TestNotPossible) {
                        final TestNotPossible testNotPossible = (TestNotPossible) testResult;
                        final Element skippedElement = doc.createElement("skipped");
                        if (testNotPossible.testNotPossibleException != null && testNotPossible.testNotPossibleException.getMessage() != null && !testNotPossible.testNotPossibleException.getMessage().isBlank()) {
                            skippedElement.setAttribute("message", testNotPossible.testNotPossibleException.getMessage());
                        }
                        testcaseElement.appendChild(skippedElement);
                    }
                    if (testResult instanceof FailedTest) {
                        final FailedTest failedTest = (FailedTest) testResult;
                        final Element failureElement = doc.createElement("failure");
                        final Throwable failureReason = failedTest.failureReason;
                        if (failureReason != null) {
                            failureElement.setAttribute("type", failureReason.getClass().getSimpleName());
                            if (failureReason.getMessage() != null && !failureReason.getMessage().isBlank()) {
                                failureElement.setAttribute("message", failureReason.getMessage());
                            }
                        }
                        testcaseElement.appendChild(failureElement);
                    }
                    final Element propertiesElement = doc.createElement("properties");
                    final Element logfilePropertyElement = doc.createElement("property");
                    logfilePropertyElement.setAttribute("name", "attachment");
                    logfilePropertyElement.setAttribute("value", testResult.concreteTest + ".log"); // This needs to be equal to what a configured debugger is using!
                    propertiesElement.appendChild(logfilePropertyElement);

                    if (specification != null && !specification.isBlank()) {
                        final Element specificationSectionIdentifierElement = doc.createElement("property");
                        specificationSectionIdentifierElement.setAttribute("name", "specification identifier");
                        specificationSectionIdentifierElement.setAttribute("value", specification);
                        propertiesElement.appendChild(specificationSectionIdentifierElement);
                    }

                    if (title != null) {
                        final Element specificationSectionTitleElement = doc.createElement("property");
                        specificationSectionTitleElement.setAttribute("name", "specification title");
                        specificationSectionTitleElement.setAttribute("value", title);
                        propertiesElement.appendChild(specificationSectionTitleElement);
                    }

                    final String specificationSection = getSpecificationSection(testResult.concreteTest.getMethod());
                    if (specificationSection != null) {
                        final Element specificationSectionElement = doc.createElement("property");
                        specificationSectionElement.setAttribute("name", "specification section");
                        specificationSectionElement.setAttribute("value", specificationSection);
                        propertiesElement.appendChild(specificationSectionElement);
                    }

                    final String specificationQuote = getSpecificationQuote(testResult.concreteTest.getMethod());
                    if (specificationQuote != null) {
                        final Element specificationQuoteElement = doc.createElement("property");
                        specificationQuoteElement.setAttribute("name", "specification quote");
                        specificationQuoteElement.setAttribute("value", specificationQuote);
                        propertiesElement.appendChild(specificationQuoteElement);
                    }

                    if (specification != null && !specification.isBlank()) {
                        final Element specificationUrlElement = doc.createElement("property");
                        specificationUrlElement.setAttribute("name", "specification URL");
                        specificationUrlElement.setAttribute("value", generateLink(specification, specificationSection).toString());
                        propertiesElement.appendChild(specificationUrlElement);
                    }
                    testcaseElement.appendChild(propertiesElement);

                    // Seems to always be null.
                    if (testResult.logMessages != null && !testResult.logMessages.isEmpty()) {
                        final Element sysOutElement = doc.createElement("system-out");
                        sysOutElement.setTextContent(String.join(System.lineSeparator(), testResult.logMessages));
                        testcaseElement.appendChild(sysOutElement);
                    }

                    testsuiteElement.appendChild(testcaseElement);
                }
            }

            // write dom document to a file
            try {
                Files.createDirectories(logFile.getParent()); // TODO move creation of this directory back to the constructor when possible. As a work-around, this code delays creating this directory, as its existence will cause StandardSinttestDebugger to fail (With Smack 4.5.0-beta6 it needs to be able to _create_ the directory, see https://github.com/igniterealtime/Smack/pull/656 )
                try (final FileOutputStream output = new FileOutputStream(logFile.toFile())) {
                    writeXml(doc, output);
                } catch (IOException | TransformerException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Logging location does not exist or is not writable: " + logFile.toAbsolutePath(), e);
            }
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static URI generateLink(final String specification, final String specificationSection) {
        if (specification == null || specification.isBlank()) {
            return null;
        }

        String link;
        if (specification.toUpperCase().startsWith("XEP")) {
            String normalizedSpec = specification.substring(0, 3);
            switch (specification.charAt(3)) {
                case ' ':
                    normalizedSpec += '-' + specification.substring(5);
                    break;
                case '-':
                    normalizedSpec += specification.substring(3);
                    break;
                default:
                    normalizedSpec += '-' + specification.substring(3);
                    break;
            }
            link = "https://xmpp.org/extensions/" + URLEncoder.encode(normalizedSpec.toLowerCase(), StandardCharsets.UTF_8) + ".html";
            if (specificationSection != null) {
                link += "#" + URLEncoder.encode(specificationSection, StandardCharsets.UTF_8); // FIXME this is wrong for XEPs, as they use the title of the section, not its number, as the anchor. Maybe convince someone to add both?
            }
        } else if (specification.toUpperCase().startsWith("RFC")) {
            String normalizedSpec = specification.substring(0, 3);
            switch (specification.charAt(3)) {
                case '-': // intended fall-through
                case ' ':
                    normalizedSpec += specification.substring(4);
                    break;
                default:
                    normalizedSpec += specification.substring(3);
                    break;
            }
            link = "https://www.rfc-editor.org/rfc/" + URLEncoder.encode(normalizedSpec.toLowerCase(), StandardCharsets.UTF_8) + ".html";
        } else {
            return null;
        }

        try {
            return URI.create(link);
        } catch (Throwable t) {
            // Fail to provide a link rather than fail to generate a report.
            return null;
        }
    }

    public static void writeXml(Document doc, OutputStream output) throws TransformerException
    {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        final DOMSource source = new DOMSource(doc);
        final StreamResult result = new StreamResult(output);

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    public static Map<String, List<TestResult>> aggregateTestResultsBySpecification(final SmackIntegrationTestFramework.TestRunResult testRunResult) {
        final Collection<TestResult> allTestResults = new ArrayList<>();
        allTestResults.addAll(testRunResult.getFailedTests());
        allTestResults.addAll(testRunResult.getSuccessfulTests());
        allTestResults.addAll(testRunResult.getNotPossibleTests());

        return allTestResults.stream().collect(Collectors.groupingBy(e -> getSpecificationReference(e.concreteTest.getMethod())));
    }

    public static String getSpecificationReference(Class<?> clazz) {
        final SpecificationReference spec = clazz.getAnnotation(SpecificationReference.class);
        if (spec == null || spec.document().isBlank()) {
            return "";
        }
        return normalizeSpecification(spec.document().trim());
    }

    public static String getSpecificationReference(Method method) {
        return getSpecificationReference(method.getDeclaringClass());
    }

    public static String getSpecificationSection(Method method) {
        final SmackIntegrationTest test = method.getAnnotation(SmackIntegrationTest.class);
        if (!test.section().isBlank()) {
            return test.section().trim();
        }
        return null;
    }

    public static String getSpecificationQuote(Method method) {
        final SmackIntegrationTest test = method.getAnnotation(SmackIntegrationTest.class);
        if (!test.quote().isBlank()) {
            return test.quote().trim();
        }
        return null;
    }

    static String normalizeSpecification(String specification) {
        if (specification == null || specification.isBlank()) {
            return "";
        }
        return specification.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static Duration getAggregatedTime(final SmackIntegrationTestFramework.TestRunResult testRunResult) {
        Duration total = Duration.ZERO;
        total = total.plus( getAggregatedTime(testRunResult.getSuccessfulTests()));
        total = total.plus( getAggregatedTime(testRunResult.getFailedTests()));
        total = total.plus( getAggregatedTime(testRunResult.getNotPossibleTests()));
        return total;
    }

    public static Duration getAggregatedTime(final Collection<? extends TestResult> tests) {
        return tests.stream().map(test -> test.duration).reduce(Duration.ZERO, Duration::plus);
    }
}
