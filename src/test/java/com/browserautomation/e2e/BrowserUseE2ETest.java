package com.browserautomation.e2e;

import com.browserautomation.BrowserAutomation;
import com.browserautomation.agent.AgentConfig;
import com.browserautomation.agent.AgentResult;
import com.browserautomation.agent.AgentState;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;
import com.browserautomation.scriptgen.PlaywrightScriptGenerator;
import com.browserautomation.scriptgen.ScriptGeneratorConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End integration tests for browser-automation.
 *
 * These tests mirror the default browser-use Python example:
 * <pre>
 * async def main():
 *     browser = Browser(use_cloud=False)
 *     agent = Agent(
 *         task="Find the number of stars of the browser-use repo",
 *         llm=ChatGoogle(model='gemini-3-flash-preview'),
 *         browser=browser,
 *     )
 *     await agent.run()
 * </pre>
 *
 * These tests are tagged with "e2e" and only run when the corresponding
 * API key environment variable is set. They are excluded from regular CI builds.
 *
 * To run manually:
 * <pre>
 *   # With Gemini
 *   GEMINI_API_KEY=your-key mvn test -Dgroups=e2e -pl .
 *
 *   # With OpenAI
 *   OPENAI_API_KEY=your-key mvn test -Dgroups=e2e -pl .
 *
 *   # With Anthropic
 *   ANTHROPIC_API_KEY=your-key mvn test -Dgroups=e2e -pl .
 *
 *   # With DeepSeek
 *   DEEPSEEK_API_KEY=your-key mvn test -Dgroups=e2e -pl .
 * </pre>
 */
@Tag("e2e")
class BrowserUseE2ETest {

    private static final String TASK = "Find the number of stars of the browser-use repo on github";

    /** Artifact output directory — configurable via E2E_ARTIFACTS_DIR env var. */
    private static final Path ARTIFACTS_DIR = Path.of(
            System.getenv("E2E_ARTIFACTS_DIR") != null
                    ? System.getenv("E2E_ARTIFACTS_DIR")
                    : "target/e2e-artifacts");

    /**
     * E2E test using Google Gemini — direct equivalent of the Python example.
     *
     * Python:
     *   agent = Agent(task="...", llm=ChatGoogle(model='gemini-3-flash-preview'), browser=browser)
     *   await agent.run()
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void testFindBrowserUseStars_withGemini() throws IOException {
        Files.createDirectories(ARTIFACTS_DIR);

        // Configure video recording directory
        Path videoDir = ARTIFACTS_DIR.resolve("videos");
        Files.createDirectories(videoDir);

        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile()
                        .headless(true)
                        .viewportSize(1280, 720));
        browser.startWithVideoRecording(videoDir);

        StringBuilder executionLog = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        executionLog.append("=== E2E Test Execution Log ===\n");
        executionLog.append("Timestamp: ").append(timestamp).append("\n");
        executionLog.append("Task: ").append(TASK).append("\n");
        executionLog.append("Provider: Google Gemini (gemini-3-flash-preview)\n");
        executionLog.append("Vision: enabled\n");
        executionLog.append("Max Steps: 15\n");
        executionLog.append("==============================\n\n");

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
            executionLog.append("!!! TEST FAILED !!!\n");
            executionLog.append("Error: ").append(errorMessage).append("\n\n");
            throw e;
        } finally {
            long totalElapsed = System.currentTimeMillis() - startTime;

            // --- 1. Write detailed execution log ---
            if (result != null) {
                appendExecutionSummary(executionLog, result, totalElapsed);
                appendStepDetails(executionLog, result);
            } else {
                executionLog.append("Result: UNAVAILABLE (agent did not return a result)\n");
                executionLog.append("Total Elapsed: ").append(totalElapsed).append("ms\n");
            }

            Path logFile = ARTIFACTS_DIR.resolve("execution-log.txt");
            Files.writeString(logFile, executionLog.toString(), StandardCharsets.UTF_8);
            System.out.println("[Gemini] Execution log saved to: " + logFile.toAbsolutePath());

            // --- 2. Write execution summary (machine-readable JSON) ---
            StringBuilder summary = new StringBuilder();
            summary.append("{\n");
            summary.append("  \"task\": \"").append(escapeJson(TASK)).append("\",\n");
            summary.append("  \"provider\": \"gemini-3-flash-preview\",\n");
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
            System.out.println("[Gemini] Execution summary saved to: " + summaryFile.toAbsolutePath());

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
                    System.out.println("[Gemini] Playwright script saved to: " + scriptFile.toAbsolutePath());
                } catch (Exception e) {
                    System.err.println("[Gemini] Failed to generate Playwright script: " + e.getMessage());
                }
            }

            // --- 4. Print console summary ---
            System.out.println("\n========== E2E EXECUTION SUMMARY ==========");
            System.out.println("[Gemini] Status: " + (testPassed ? "PASSED" : "FAILED"));
            if (result != null) {
                System.out.println("[Gemini] Result: " + result.getResult());
                System.out.println("[Gemini] Steps: " + result.getTotalSteps());
                System.out.println("[Gemini] Tokens: " + result.getTotalTokensUsed());
                System.out.println("[Gemini] Duration: " + result.getTotalDurationMs() + "ms");
            }
            System.out.println("[Gemini] Total Elapsed: " + totalElapsed + "ms");
            if (errorMessage != null) {
                System.out.println("[Gemini] Error: " + errorMessage);
            }
            System.out.println("============================================\n");

            // --- 5. Close browser (video is finalized on context close) ---
            browser.close();
            System.out.println("[Gemini] Video recording saved to: " + videoDir.toAbsolutePath());
        }
    }

    /**
     * E2E test using OpenAI GPT-4o.
     *
     * Python equivalent:
     *   agent = Agent(task="...", llm=ChatOpenAI(model='gpt-4o'), browser=browser)
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testFindBrowserUseStars_withOpenAi() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task(TASK)
                    .openAi("gpt-4o")
                    .browserSession(browser)
                    .config(new AgentConfig().maxSteps(15).useVision(true))
                    .run();

            assertNotNull(result);
            assertTrue(result.isSuccess(), "Agent should complete successfully");
            assertNotNull(result.getResult());

            System.out.println("[OpenAI] Result: " + result.getResult());
            System.out.println("[OpenAI] Steps: " + result.getTotalSteps());
        } finally {
            browser.close();
        }
    }

    /**
     * E2E test using Anthropic Claude.
     *
     * Python equivalent:
     *   agent = Agent(task="...", llm=ChatAnthropic(model='claude-sonnet-4-6'), browser=browser)
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    void testFindBrowserUseStars_withAnthropic() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task(TASK)
                    .anthropic("claude-sonnet-4-20250514")
                    .browserSession(browser)
                    .config(new AgentConfig().maxSteps(15).useVision(true))
                    .run();

            assertNotNull(result);
            assertTrue(result.isSuccess(), "Agent should complete successfully");
            assertNotNull(result.getResult());

            System.out.println("[Anthropic] Result: " + result.getResult());
            System.out.println("[Anthropic] Steps: " + result.getTotalSteps());
        } finally {
            browser.close();
        }
    }

    /**
     * E2E test using DeepSeek V3.
     * Note: DeepSeek does not support vision, so useVision is set to false.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void testFindBrowserUseStars_withDeepSeek() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task(TASK)
                    .deepSeek("deepseek-chat")
                    .browserSession(browser)
                    .config(new AgentConfig().maxSteps(15).useVision(false))
                    .run();

            assertNotNull(result);
            assertTrue(result.isSuccess(), "Agent should complete successfully");
            assertNotNull(result.getResult());

            System.out.println("[DeepSeek] Result: " + result.getResult());
            System.out.println("[DeepSeek] Steps: " + result.getTotalSteps());
        } finally {
            browser.close();
        }
    }

    /**
     * E2E test using Azure OpenAI.
     * Requires both AZURE_OPENAI_KEY and AZURE_OPENAI_ENDPOINT env vars.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "AZURE_OPENAI_KEY", matches = ".+")
    void testFindBrowserUseStars_withAzureOpenAi() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task(TASK)
                    .azureOpenAi("gpt-4o")
                    .browserSession(browser)
                    .config(new AgentConfig().maxSteps(15).useVision(true))
                    .run();

            assertNotNull(result);
            assertTrue(result.isSuccess(), "Agent should complete successfully");
            assertNotNull(result.getResult());

            System.out.println("[Azure OpenAI] Result: " + result.getResult());
            System.out.println("[Azure OpenAI] Steps: " + result.getTotalSteps());
        } finally {
            browser.close();
        }
    }

    // --- Helper methods for artifact generation ---

    private void appendExecutionSummary(StringBuilder log, AgentResult result, long totalElapsed) {
        log.append("--- Execution Summary ---\n");
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
