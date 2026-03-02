package com.browserautomation.e2e;

import com.browserautomation.BrowserAutomation;
import com.browserautomation.agent.AgentConfig;
import com.browserautomation.agent.AgentResult;
import com.browserautomation.agent.AgentState;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;
import com.browserautomation.scriptgen.PlaywrightScriptGenerator;
import com.browserautomation.scriptgen.ScriptGeneratorConfig;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.ConsoleAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End integration tests using Selenium engine with Google Gemini.
 *
 * This test mirrors {@link BrowserUseE2ETest#testFindBrowserUseStars_withGemini()}
 * but uses the Selenium WebDriver engine instead of Playwright. It generates the
 * same set of artifacts (execution log, summary JSON, Playwright script) for
 * comparison and CI notification.
 *
 * To run manually:
 * <pre>
 *   GEMINI_API_KEY=your-key mvn test -Dgroups=e2e -Dtest=SeleniumE2ETest
 * </pre>
 */
@Tag("e2e")
class SeleniumE2ETest {

    private static final String TASK = "Find the current price of Bitcoin on google";

    /** Artifact output directory for Selenium E2E — separate from Playwright artifacts. */
    private static final Path ARTIFACTS_DIR = Path.of(
            System.getenv("E2E_ARTIFACTS_DIR") != null
                    ? System.getenv("E2E_ARTIFACTS_DIR") + "-selenium"
                    : "target/e2e-artifacts-selenium");

    /**
     * Set up a logback FileAppender that captures ALL SLF4J log output from
     * com.browserautomation.* into the given file.
     */
    private FileAppender<ILoggingEvent> setupLogCapture(Path logFile) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        fileEncoder.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("SELENIUM_E2E_FILE");
        fileAppender.setFile(logFile.toAbsolutePath().toString());
        fileAppender.setAppend(true);
        fileAppender.setEncoder(fileEncoder);
        fileAppender.start();

        PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(loggerContext);
        consoleEncoder.setPattern("%d{HH:mm:ss.SSS} %-5level [%logger{20}] %msg%n");
        consoleEncoder.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("SELENIUM_E2E_CONSOLE");
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.setTarget("System.out");
        consoleAppender.start();

        Logger baLogger = loggerContext.getLogger("com.browserautomation");
        baLogger.addAppender(fileAppender);
        baLogger.addAppender(consoleAppender);
        baLogger.setLevel(ch.qos.logback.classic.Level.INFO);
        baLogger.setAdditive(false);

        return fileAppender;
    }

    /**
     * Remove E2E appenders from the logger.
     */
    private void teardownLogCapture(FileAppender<ILoggingEvent> fileAppender) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger baLogger = loggerContext.getLogger("com.browserautomation");
        baLogger.detachAppender("SELENIUM_E2E_FILE");
        baLogger.detachAppender("SELENIUM_E2E_CONSOLE");
        baLogger.setAdditive(true);
        if (fileAppender != null) {
            fileAppender.stop();
        }
    }

    /**
     * E2E test using Google Gemini + Selenium engine.
     *
     * Same task as the Playwright-based Gemini E2E test, but uses Selenium WebDriver
     * for browser automation. Generates execution log, summary JSON, Playwright script,
     * and console output for CI notifications.
     *
     * Note: Selenium does not natively support video recording, so no video artifact
     * is generated. Screenshots are still captured via the engine abstraction.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void testFindBrowserUseStars_withGeminiSelenium() throws IOException {
        Files.createDirectories(ARTIFACTS_DIR);

        // --- Set up log capture BEFORE any automation runs ---
        Path logFile = ARTIFACTS_DIR.resolve("execution-log.txt");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder logHeader = new StringBuilder();
        logHeader.append("=== Selenium E2E Test Execution Log ===\n");
        logHeader.append("Timestamp: ").append(timestamp).append("\n");
        logHeader.append("Task: ").append(TASK).append("\n");
        logHeader.append("Provider: Google Gemini (gemini-3-flash-preview)\n");
        logHeader.append("Engine: Selenium WebDriver\n");
        logHeader.append("Vision: enabled\n");
        logHeader.append("Max Steps: 15\n");
        logHeader.append("==========================================\n\n");
        logHeader.append("--- Comprehensive SLF4J Execution Trace ---\n\n");
        Files.writeString(logFile, logHeader.toString(), StandardCharsets.UTF_8);

        System.out.println(logHeader);

        FileAppender<ILoggingEvent> fileAppender = setupLogCapture(logFile);

        // Create a Selenium-based browser session
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile()
                        .useSelenium()
                        .headless(true)
                        .viewportSize(1280, 720));
        browser.start();

        AgentResult result = null;
        long startTime = System.currentTimeMillis();
        boolean testPassed = false;
        String errorMessage = null;

        try {
            result = BrowserAutomation.agent()
                    .task(TASK)
                    .gemini("gemini-3-flash-preview")
                    .browserSession(browser)
                    .config(new AgentConfig().maxSteps(15).useVision(true))
                    .run();

            assertNotNull(result, "Agent should return a result");
            assertTrue(result.isSuccess(), "Agent should complete the task successfully");
            assertNotNull(result.getResult(), "Result should contain the star count");
            assertTrue(result.getTotalSteps() > 0, "Agent should have taken at least one step");
            testPassed = true;
        } catch (Exception e) {
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw e;
        } finally {
            long totalElapsed = System.currentTimeMillis() - startTime;

            // --- Stop log capture so we can append summary to the file ---
            teardownLogCapture(fileAppender);

            // --- 1. Append execution summary and step details to log file ---
            StringBuilder logFooter = new StringBuilder();
            logFooter.append("\n\n--- End of SLF4J Execution Trace ---\n\n");

            if (!testPassed && errorMessage != null) {
                logFooter.append("!!! TEST FAILED !!!\n");
                logFooter.append("Error: ").append(errorMessage).append("\n\n");
            }

            if (result != null) {
                appendExecutionSummary(logFooter, result, totalElapsed);
                appendStepDetails(logFooter, result);
            } else {
                logFooter.append("Result: UNAVAILABLE (agent did not return a result)\n");
                logFooter.append("Total Elapsed: ").append(totalElapsed).append("ms\n");
            }

            Files.writeString(logFile, logFooter.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
            System.out.println("[Selenium+Gemini] Execution log saved to: " + logFile.toAbsolutePath());

            // --- 2. Write execution summary (machine-readable JSON) ---
            StringBuilder summary = new StringBuilder();
            summary.append("{\n");
            summary.append("  \"task\": \"").append(escapeJson(TASK)).append("\",\n");
            summary.append("  \"provider\": \"gemini-3-flash-preview\",\n");
            summary.append("  \"engine\": \"selenium\",\n");
            summary.append("  \"timestamp\": \"").append(timestamp).append("\",\n");
            summary.append("  \"passed\": ").append(testPassed).append(",\n");
            if (result != null) {
                summary.append("  \"result\": \"").append(escapeJson(result.getResult())).append("\",\n");
                summary.append("  \"steps\": ").append(result.getTotalSteps()).append(",\n");
                summary.append("  \"tokens\": ").append(result.getTotalTokensUsed()).append(",\n");
                summary.append("  \"durationMs\": ").append(result.getTotalDurationMs()).append(",\n");
            }
            summary.append("  \"totalElapsedMs\": ").append(totalElapsed);
            if (errorMessage != null) {
                summary.append(",\n  \"error\": \"").append(escapeJson(errorMessage)).append("\"");
            }
            summary.append("\n}\n");

            Path summaryFile = ARTIFACTS_DIR.resolve("execution-summary.json");
            Files.writeString(summaryFile, summary.toString(), StandardCharsets.UTF_8);
            System.out.println("[Selenium+Gemini] Execution summary saved to: " + summaryFile.toAbsolutePath());

            // --- 3. Generate Playwright TypeScript script ---
            if (result != null) {
                try {
                    PlaywrightScriptGenerator generator = new PlaywrightScriptGenerator(
                            new ScriptGeneratorConfig()
                                    .viewportWidth(1280)
                                    .viewportHeight(720)
                                    .includeComments(true)
                                    .includeRetryLogic(true)
                                    .includeNetworkWaits(true)
                                    .includeDomStabilityWaits(true));

                    Path scriptFile = ARTIFACTS_DIR.resolve("generated-test.spec.ts");
                    generator.generateAndSave(result, TASK, scriptFile);
                    System.out.println("[Selenium+Gemini] Playwright script saved to: " + scriptFile.toAbsolutePath());
                } catch (Exception e) {
                    System.err.println("[Selenium+Gemini] Failed to generate Playwright script: " + e.getMessage());
                }
            }

            // --- 4. Print comprehensive console summary ---
            System.out.println();
            System.out.println("======================================================");
            System.out.println("     SELENIUM E2E EXECUTION SUMMARY");
            System.out.println("======================================================");
            System.out.println("[Selenium+Gemini] Engine:   Selenium WebDriver");
            System.out.println("[Selenium+Gemini] Status:   " + (testPassed ? "PASSED" : "FAILED"));
            if (result != null) {
                System.out.println("[Selenium+Gemini] Result:   " + result.getResult());
                System.out.println("[Selenium+Gemini] Steps:    " + result.getTotalSteps());
                System.out.println("[Selenium+Gemini] Tokens:   " + result.getTotalTokensUsed());
                System.out.println("[Selenium+Gemini] Duration: " + result.getTotalDurationMs() + "ms");
            }
            System.out.println("[Selenium+Gemini] Total Elapsed: " + totalElapsed + "ms");
            if (errorMessage != null) {
                System.out.println("[Selenium+Gemini] Error:    " + errorMessage);
            }

            // Print step-by-step to console
            if (result != null && result.getHistory() != null) {
                System.out.println();
                System.out.println("--- Step-by-Step Console Summary ---");
                List<AgentState.AgentStep> history = result.getHistory();
                for (int i = 0; i < history.size(); i++) {
                    AgentState.AgentStep step = history.get(i);
                    String status = step.hasError() ? "FAIL" : "OK";
                    System.out.printf("[Step %d/%d] %s | action=%s | tokens=%d | duration=%dms%n",
                            i + 1, history.size(), status, step.getActionName(),
                            step.getTokensUsed(), step.getDurationMs());
                    if (step.getLlmThinking() != null && !step.getLlmThinking().isEmpty()) {
                        System.out.println("  Thinking: " + truncate(step.getLlmThinking(), 200));
                    }
                    if (step.getActionResult() != null && !step.getActionResult().isEmpty()) {
                        System.out.println("  Result:   " + truncate(step.getActionResult(), 200));
                    }
                    if (step.hasError()) {
                        System.out.println("  ERROR:    " + step.getError());
                    }
                }
            }

            System.out.println();
            System.out.println("--- Artifacts Generated ---");
            System.out.println("  execution-log.txt:       " + logFile.toAbsolutePath());
            System.out.println("  execution-summary.json:  " + ARTIFACTS_DIR.resolve("execution-summary.json").toAbsolutePath());
            System.out.println("  generated-test.spec.ts:  " + ARTIFACTS_DIR.resolve("generated-test.spec.ts").toAbsolutePath());
            System.out.println("  (no video — Selenium does not support native video recording)");
            System.out.println("======================================================");
            System.out.println();

            // --- 5. Close browser ---
            browser.close();
        }
    }

    // --- Helper methods for artifact generation ---

    private void appendExecutionSummary(StringBuilder log, AgentResult result, long totalElapsed) {
        log.append("--- Execution Summary ---\n");
        log.append("Engine: Selenium WebDriver\n");
        log.append("Status: ").append(result.isSuccess() ? "SUCCESS" : "FAILED").append("\n");
        log.append("Result: ").append(result.getResult()).append("\n");
        log.append("Total Steps: ").append(result.getTotalSteps()).append("\n");
        log.append("Total Tokens Used: ").append(result.getTotalTokensUsed()).append("\n");
        log.append("Agent Duration: ").append(result.getTotalDurationMs()).append("ms\n");
        log.append("Total Elapsed: ").append(totalElapsed).append("ms\n");
        log.append("-------------------------\n\n");
    }

    private void appendStepDetails(StringBuilder log, AgentResult result) {
        log.append("--- Step-by-Step Details ---\n");
        List<AgentState.AgentStep> history = result.getHistory();
        if (history == null || history.isEmpty()) {
            log.append("No step history available.\n");
        } else {
            for (int i = 0; i < history.size(); i++) {
                AgentState.AgentStep step = history.get(i);
                log.append("\n[Step ").append(i + 1).append("/").append(history.size()).append("]\n");
                log.append("  Action: ").append(step.getActionName()).append("\n");
                log.append("  Tokens: ").append(step.getTokensUsed()).append("\n");
                log.append("  Duration: ").append(step.getDurationMs()).append("ms\n");
                if (step.getLlmThinking() != null && !step.getLlmThinking().isEmpty()) {
                    log.append("  LLM Thinking: ").append(truncate(step.getLlmThinking(), 300)).append("\n");
                }
                if (step.getActionResult() != null && !step.getActionResult().isEmpty()) {
                    log.append("  Result: ").append(truncate(step.getActionResult(), 300)).append("\n");
                }
                if (step.hasError()) {
                    log.append("  ERROR: ").append(step.getError()).append("\n");
                }
            }
        }
        log.append("\n--- End of Step Details ---\n");
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
