package com.browserautomation.e2e;

import com.browserautomation.BrowserAutomation;
import com.browserautomation.agent.AgentConfig;
import com.browserautomation.agent.AgentResult;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

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

    /**
     * E2E test using Google Gemini — direct equivalent of the Python example.
     *
     * Python:
     *   agent = Agent(task="...", llm=ChatGoogle(model='gemini-3-flash-preview'), browser=browser)
     *   await agent.run()
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void testFindBrowserUseStars_withGemini() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task(TASK)
                    .gemini("gemini-2.0-flash-exp")
                    .browserSession(browser)
                    .config(new AgentConfig().maxSteps(15).useVision(true))
                    .run();

            assertNotNull(result, "Agent should return a result");
            assertTrue(result.isSuccess(), "Agent should complete the task successfully");
            assertNotNull(result.getResult(), "Result should contain the star count");
            assertTrue(result.getTotalSteps() > 0, "Agent should have taken at least one step");

            System.out.println("[Gemini] Result: " + result.getResult());
            System.out.println("[Gemini] Steps: " + result.getTotalSteps());
            System.out.println("[Gemini] Tokens: " + result.getTotalTokensUsed());
            System.out.println("[Gemini] Duration: " + result.getTotalDurationMs() + "ms");
        } finally {
            browser.close();
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
}
