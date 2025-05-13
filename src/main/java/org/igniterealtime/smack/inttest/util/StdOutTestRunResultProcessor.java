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

import org.igniterealtime.smack.inttest.*;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class StdOutTestRunResultProcessor implements SmackIntegrationTestFramework.TestRunResultProcessor
{
    @Override
    public void process(final SmackIntegrationTestFramework.TestRunResult testRunResult)
    {
        final int successfulTests = testRunResult.getSuccessfulTests().size();
        final int failedTests = testRunResult.getFailedTests().size();
        final int impossibleTests = testRunResult.getNotPossibleTests().size() + getMethodsInImpossibleTestClasses(testRunResult.getImpossibleTestClasses().keySet()).size();

        System.out.println();
        System.out.println("Test run (id: " + testRunResult.testRunId + ") finished! " + successfulTests + " tests were successful (✔), " + failedTests + " failed (\uD83D\uDC80), and " + impossibleTests + " were impossible to run (✖).");
        System.out.println();
        System.out.println("Results aggregated by specification:");

        final Properties specTitles = new Properties();
        try {
            specTitles.load(JUnitXmlTestRunResultProcessor.class.getResourceAsStream("/specifications.properties"));
        } catch (IOException e) {
            System.err.println("Unable to load specifications.properties");
        }

        final SortedMap<String, Collection<SuccessfulTest>> successFulTestsBySpec = aggregateBySpecification(testRunResult.getSuccessfulTests());
        final SortedMap<String, Collection<FailedTest>> failedTestsBySpec = aggregateBySpecification(testRunResult.getFailedTests());
        final SortedMap<String, Collection<TestNotPossible>> impossibleTestsBySpec = aggregateBySpecification(testRunResult.getNotPossibleTests());
        final SortedMap<String, Collection<Class<? extends AbstractSmackIntTest>>> impossibleTestClassesBySpec = aggregateBySpecification(testRunResult.getImpossibleTestClasses().keySet());

        final SortedSet<String> specifications = new TreeSet<>();
        specifications.addAll(successFulTestsBySpec.keySet());
        specifications.addAll(failedTestsBySpec.keySet());
        specifications.addAll(impossibleTestsBySpec.keySet());
        specifications.addAll(impossibleTestClassesBySpec.keySet());
        final Map<String, String> titleBySpec = new HashMap<>();
        titleBySpec.put("", "(noname)");
        for (final String specification : specifications) {
            titleBySpec.put(specification, findTitle(specTitles, specification, 81));
        }
        final int longestSpecCharCount = titleBySpec.values().stream().map(String::length).max(Integer::compareTo).orElse(0);
        final int longestSuccCharCount = successFulTestsBySpec.values().stream().map(Collection::size).map(i->i.toString().length()).max(Integer::compareTo).orElse(1);
        final int longestFailCharCount = failedTestsBySpec.values().stream().map(Collection::size).map(i->i.toString().length()).max(Integer::compareTo).orElse(1);
        for (final String specification : specifications) {
            final int success = successFulTestsBySpec.getOrDefault(specification, Collections.emptySet()).size();
            final int fail = failedTestsBySpec.getOrDefault(specification, Collections.emptySet()).size();
            final int impossible = impossibleTestsBySpec.getOrDefault(specification, Collections.emptySet()).size() + getMethodsInImpossibleTestClasses(impossibleTestClassesBySpec.getOrDefault(specification, Collections.emptySet())).size();
            final String title = titleBySpec.get(specification);
            System.out.println("• " + String.format("%-" + Math.max(1, longestSpecCharCount)+"s", title) + " " + String.format("%"+longestSuccCharCount+"s", success) + " ✔  " + String.format("%"+longestFailCharCount+"s", fail) + " \uD83D\uDC80 " + String.format("%3s", impossible) + " ✖");
        }

        if (!impossibleTestsBySpec.isEmpty() || !impossibleTestClassesBySpec.isEmpty()) {
            System.out.println();
            System.out.println("✖ The following tests were impossible to run! ✖");

            for (final Map.Entry<String, Collection<TestNotPossible>> entry : impossibleTestsBySpec.entrySet()) {
                final String title = (entry.getKey().isEmpty() ? "(noname)" : entry.getKey());
                final Map<String, Long> reasonCount = entry.getValue().stream().collect(Collectors.groupingBy(t -> t.testNotPossibleException.getMessage(), Collectors.counting()));
                for (final Map.Entry<String, Long> reasonEntry : reasonCount.entrySet()) {
                    System.out.println("• " + title + ": could not run " + reasonEntry.getValue() + " test(s) because: " + reasonEntry.getKey());
                }
            }
            for (final Map.Entry<String, Collection<Class<? extends AbstractSmackIntTest>>> entry : impossibleTestClassesBySpec.entrySet()) {
                final String title = (entry.getKey().isEmpty() ? "(noname)" : entry.getKey());
                final Collection<Class<? extends AbstractSmackIntTest>> classes = entry.getValue();
                final SortedMap<String, Integer> counts = new TreeMap<>();
                for (final Class<? extends AbstractSmackIntTest> clazz : classes) {
                    final String reason = testRunResult.getImpossibleTestClasses().get(clazz).getLocalizedMessage();
                    final int count = getMethodsInImpossibleTestClass(clazz).size();
                    int c = counts.getOrDefault(reason, 0);
                    c += count;
                    counts.put(reason, c);
                }
                for (Map.Entry<String, Integer> e : counts.entrySet()) {
                    System.out.println("• " + title + ": could not run " + e.getValue() + " test(s) because: " + e.getKey());
                }
            }
        }


        if (!failedTestsBySpec.isEmpty()) {
            System.out.println();
            System.out.println("💀 The following " + failedTests + " tests failed! 💀");

            final SortedMap<String, Collection<FailedTest>> sortedFailedTestBySpec = new TreeMap<>(failedTestsBySpec);
            for (final Map.Entry<String, Collection<FailedTest>> entry : sortedFailedTestBySpec.entrySet()) {
                final String title = entry.getKey();

                final SortedMap<String, String> sortedBlobs = new TreeMap<>();
                for (final FailedTest failedTest : entry.getValue()) {
                    final String sectionReference = JUnitXmlTestRunResultProcessor.getSpecificationSection(failedTest.concreteTest.getMethod());
                    final String quote = JUnitXmlTestRunResultProcessor.getSpecificationQuote(failedTest.concreteTest.getMethod());
                    final Path logPath = FileLogger.getLog(System.getProperty("logDir") == null ? null : Paths.get(System.getProperty("logDir")), failedTest.concreteTest);
                    final StringBuilder blob = new StringBuilder();
                    blob.append("• ").append(findTitle(specTitles, title, -1)).append(sectionReference != null ? ", Section " + sectionReference : "").append(System.lineSeparator());
                    blob.append("      \"" + quote + "\"").append(System.lineSeparator());
                    blob.append("  Failure reason  : " + failedTest.failureReason.getMessage()).append(System.lineSeparator());
                    blob.append("  Stanza log file : " + logPath).append(System.lineSeparator());
                    blob.append("  Test class      : " + failedTest.concreteTest.getMethod().getDeclaringClass()).append(System.lineSeparator());
                    blob.append("  Test method     : " + failedTest.concreteTest.getMethod().getName()).append(System.lineSeparator());
                    blob.append(System.lineSeparator());

                    // The key in this map is to force a repeatable order, but is not otherwise used in the output.
                    sortedBlobs.put(sectionReference + '|' + failedTest.concreteTest.getMethod().getDeclaringClass() + '#' + failedTest.concreteTest.getMethod().getName(), blob.toString());
                }

                sortedBlobs.values().forEach(System.out::print);
            }
        }
    }

    public static List<Method> getMethodsInImpossibleTestClasses(Collection<Class<? extends AbstractSmackIntTest>> testClasses)
    {
        final List<Method> result = new ArrayList<>();
        for (final Class<? extends AbstractSmackIntTest> testClass : testClasses) {
            result.addAll(getMethodsInImpossibleTestClass(testClass));
        }
        return result;
    }

    public static List<Method> getMethodsInImpossibleTestClass(Class<? extends AbstractSmackIntTest> testClass) {
        Method[] testClassMethods = testClass.getMethods();
        List<Method> smackIntegrationTestMethods = new ArrayList<>(testClassMethods.length);
        for (Method method : testClassMethods) {
            if (!method.isAnnotationPresent(SmackIntegrationTest.class)) {
                continue;
            }
            smackIntegrationTestMethods.add(method);
        }
        return smackIntegrationTestMethods;
    }

    public static <T extends TestResult> SortedMap<String, Collection<T>> aggregateBySpecification(final Collection<T> testResults) {
        final ConcurrentSkipListMap<String, Collection<T>> result = new ConcurrentSkipListMap<>();
        for (final T testResult : testResults) {
            String specificationReference = JUnitXmlTestRunResultProcessor.getSpecificationReference(testResult.concreteTest.getMethod());
            specificationReference = humanReadibleSpec(specificationReference);
            result.computeIfAbsent(specificationReference, s -> new LinkedList<>()).add(testResult);
        }
        return result;
    }

    private SortedMap<String, Collection<Class<? extends AbstractSmackIntTest>>> aggregateBySpecification(Set<Class<? extends AbstractSmackIntTest>> classes)
    {
        final SortedMap<String, Collection<Class<? extends AbstractSmackIntTest>>> result = new ConcurrentSkipListMap<>();
        for (final Class<? extends AbstractSmackIntTest> clazz : classes) {
            String specificationReference = JUnitXmlTestRunResultProcessor.getSpecificationReference(clazz);
            specificationReference = humanReadibleSpec(specificationReference);
            result.computeIfAbsent(specificationReference, s -> new LinkedList<>()).add(clazz);
        }
        return result;
    }

    public static String humanReadibleSpec(final String spec) {
        return spec.replaceFirst("^XEP", "XEP-");
    }


    public static String findTitle(final Properties specs, final String key, final int maxLength) {
        final String title = specs.getProperty(key);
        String result = key;
        if (title != null) {
            result += ": " + title;
        }

        if (maxLength > 0 && result.length() > maxLength) {
            result = result.substring(0, maxLength-3) + "...";
        }
        return result;
    }
}
