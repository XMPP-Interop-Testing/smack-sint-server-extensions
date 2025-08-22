package org.igniterealtime.smack.inttest.util;

import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework;
import org.igniterealtime.smack.inttest.debugger.SinttestDebugger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.SimpleAbstractDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.util.ExceptionUtil;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an exact copy of {@link org.igniterealtime.smack.inttest.debugger.StandardSinttestDebugger} with the fixes
 * for https://github.com/igniterealtime/Smack/pull/656
 *
 * Additional configuration has been hard-coded, as https://github.com/igniterealtime/Smack/pull/657 prevents options
 * from being specified.
 *
 * It also appends to pre-existing files, rather than failing to run if those already exist.
 *
 * Ideally, this entire implementation is replaced with usage of the StandardSinttestDebugger once its bugs are fixed.
 */
public class ModifiedStandardSinttestDebugger implements SinttestDebugger
{
    protected static final Logger LOGGER = Logger.getLogger(ModifiedStandardSinttestDebugger.class.getName());

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final Object currentWriterLock = new Object();
    private Writer currentWriter;

    private Path currentTestMethodDirectory;

    private final Path basePath;
    private final Writer completeWriter;
    private final Writer outsideTestWriter;
    private final Writer testsWriter;
    private final boolean console;

    public ModifiedStandardSinttestDebugger(ZonedDateTime restRunStart, String testRunId, String options) {
        String tmpdir = System.getProperty("java.io.tmpdir");
        if ("/tmp".equals(tmpdir)) {
            // We don't want to fill up the memory.
            tmpdir = "/var/tmp";
        }
        String basePath = tmpdir
            + File.separator
            + "sinttest-" + System.getProperty("user.name")
            + File.separator
            + DATE_TIME_FORMATTER.format(restRunStart) + "-" + testRunId
            ;

        basePath = System.getProperty("logDir");

        boolean console = false;

        if (options != null) {
            for (String keyValue : options.split(",")) {
                String[] keyValueArray = keyValue.split("=");
                if (keyValueArray.length != 2) {
                    throw new IllegalArgumentException("Illegal key/value string: " + keyValue);
                }

                String key = keyValueArray[0];
                String value = keyValueArray[1];
                switch (key) {
                    case "console":
                        switch (value) {
                            case "on":
                                console = true;
                                break;
                            case "off":
                                console = false;
                                break;
                            default:
                                throw new IllegalArgumentException(
                                    "Invalid argument console=" + value + ", only off/on are allowed");
                        }
                        break;
                    case "dir":
                        switch (value) {
                            case "off":
                                basePath = null;
                                break;
                            default:
                                basePath = value;
                                break;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown key: " + key);
                }
            }
        }

        if (basePath != null) {
            this.basePath = Path.of(basePath);
            Path completeLogFile = this.basePath.resolve("completeLog");
            Path outsideTestLogFile = this.basePath.resolve("outsideTestLog");
            Path testsFile = this.basePath.resolve("tests");
            try {
                if (!this.basePath.toFile().exists()) {
                    boolean created = this.basePath.toFile().mkdirs();
                    if (!created) {
                        throw new IOException("Could not create directory " + this.basePath);
                    }
                }

                completeWriter = Files.newBufferedWriter(completeLogFile);
                outsideTestWriter = currentWriter = Files.newBufferedWriter(outsideTestLogFile);
                testsWriter = Files.newBufferedWriter(testsFile);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            this.basePath = null;
            completeWriter = null;
            outsideTestWriter = null;
            testsWriter = null;
        }
        this.console = console;
    }

    private class ModifiedStandardSinttestSmackDebugger extends SimpleAbstractDebugger
    {
        ModifiedStandardSinttestSmackDebugger(XMPPConnection connection) {
            super(connection);
        }

        @Override
        protected void logSink(String logMessage) {
            ModifiedStandardSinttestDebugger.this.logSink(logMessage);
        }
    }

    private void logSink(String logMessage) {
        if (basePath != null) {
            try {
                synchronized (currentWriterLock) {
                    currentWriter.append(logMessage).append('\n');

                    /* This is an alternative implementation that generates content that looks like XML, which has the
                       benefit of giving syntax highlighing in many editors, making it easier for humans to read them. */
                    /*
                    boolean isXml = false;
                    for (final String line : logMessage.split("\n")) {
                        isXml = isXml || line.trim().startsWith("<");
                        if (!isXml) {
                            currentWriter.append("<!-- ");
                        }
                        currentWriter.append(line);
                        if (!isXml) {
                            currentWriter.append(" -->");
                        }
                        currentWriter.append('\n');
                    }
                     */
                }

                completeWriter.append(logMessage + "\n");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e + " while appending log message", e);
            }
        }

        if (console) {
            // CHECKSTYLE:OFF
            System.out.println(logMessage);
            // CHECKSTYLE:ON
        }
    }

    @Override
    public SmackDebuggerFactory getSmackDebuggerFactory() {
        return c -> new ModifiedStandardSinttestDebugger.ModifiedStandardSinttestSmackDebugger(c);
    }

    @Override
    public void onTestStart(SmackIntegrationTestFramework.ConcreteTest test, ZonedDateTime startTime) throws IOException {
        if (basePath == null) {
            return;
        }

        Method testMethod = test.getMethod();

        Path testClassDirectory = basePath.resolve(testMethod.getDeclaringClass().getSimpleName());

        StringBuilder testName = new StringBuilder(testMethod.getName());
        for (String subdescription : test.getSubdescriptons()) {
            testName.append('-').append(subdescription);
        }
        currentTestMethodDirectory = testClassDirectory.resolve(testName.toString());

        if (!currentTestMethodDirectory.toFile().exists()) {
            boolean created = currentTestMethodDirectory.toFile().mkdirs();
            if (!created) {
                throw new IOException("Could not create directory " + currentTestMethodDirectory);
            }
        }

        Path logFile = currentTestMethodDirectory.resolve("log");
        Writer newWriter = Files.newBufferedWriter(logFile);

        synchronized (currentWriterLock) {
            currentWriter = newWriter;
        }

        completeWriter.append("START: " + test + "\n");

        testsWriter.append(test.toString());
    }

    private void onTestEnd(Throwable throwable) throws IOException {
        if (basePath == null) {
            return;
        }

        Writer oldWriter;
        synchronized (currentWriterLock) {
            oldWriter = currentWriter;
            currentWriter = outsideTestWriter;
        }
        if (oldWriter != null) {
            oldWriter.close();
        }

        if (throwable == null) {
            testsWriter.append(" ✓");
        } else {
            testsWriter.append(" ✗ [FAILED: ").append(throwable.getClass().getSimpleName()).append(']');
        }
        testsWriter.append('\n');
    }

    private Path createTestMarkerFile(String name) throws IOException {
        if (currentTestMethodDirectory == null) {
            return null;
        }

        Path failedMarker = currentTestMethodDirectory.resolve(name);
        if (Files.notExists(failedMarker)) {
            return Files.createFile(failedMarker);
        }
        return failedMarker;
    }

    @Override
    public void onTestSuccess(SmackIntegrationTestFramework.ConcreteTest test, ZonedDateTime endTime) throws IOException {
        logSink("TEST SUCCESSFUL: " + test);

        createTestMarkerFile("successful");

        onTestEnd(null);
    }

    @Override
    public void onTestFailure(SmackIntegrationTestFramework.ConcreteTest test, ZonedDateTime endTime, Throwable throwable) throws IOException {
        String stacktrace = ExceptionUtil.getStackTrace(throwable);

        logSink("TEST FAILED: " + test + "\n" + stacktrace);

        Path markerFile = createTestMarkerFile("failed");
        if (markerFile != null) {
            Files.writeString(markerFile, stacktrace);
        }
        if (currentTestMethodDirectory != null) {
            if (throwable instanceof ResultSyncPoint.ResultSyncPointTimeoutException) {
                var resultSyncPointTimeoutException = (ResultSyncPoint.ResultSyncPointTimeoutException) throwable;
                var threadDump = resultSyncPointTimeoutException.getThreadDump();
                var threadDumpFile = currentTestMethodDirectory.resolve("thread-dump");
                Files.writeString(threadDumpFile, threadDump);

                logSink("Wrote thread dump to file://" + threadDumpFile);
            }
        }

        onTestEnd(throwable);
    }

    @Override
    public void onSinttestFinished(SmackIntegrationTestFramework.TestRunResult testRunResult) throws IOException {
        if (basePath == null) {
            return;
        }

        outsideTestWriter.close();
        completeWriter.close();
        testsWriter.close();

        LOGGER.info("Test data file://" + basePath);
    }
}
