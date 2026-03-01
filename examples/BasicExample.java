import com.browserautomation.BrowserAutomation;
import com.browserautomation.agent.Agent;
import com.browserautomation.agent.AgentConfig;
import com.browserautomation.agent.AgentResult;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;
import com.browserautomation.llm.OpenAiProvider;

/**
 * Basic examples demonstrating how to use the browser-automation library.
 *
 * Before running, set the OPENAI_API_KEY environment variable:
 *   export OPENAI_API_KEY=your-api-key
 */
public class BasicExample {

    /**
     * Example 1: Simplest usage with the fluent API.
     */
    public static void simpleAgent() {
        AgentResult result = BrowserAutomation.agent()
                .task("Go to google.com and search for 'Java browser automation'")
                .openAi("gpt-4o")
                .run();

        System.out.println("Success: " + result.isSuccess());
        System.out.println("Result: " + result.getResult());
        System.out.println("Steps: " + result.getTotalSteps());
    }

    /**
     * Example 2: Custom configuration.
     */
    public static void customConfig() {
        AgentResult result = BrowserAutomation.agent()
                .task("Find the top 5 trending repositories on GitHub")
                .openAi("gpt-4o")
                .browserProfile(new BrowserProfile()
                        .headless(true)
                        .viewportSize(1920, 1080))
                .config(new AgentConfig()
                        .maxSteps(30)
                        .maxFailures(3)
                        .useVision(true))
                .run();

        System.out.println("Result: " + result.getResult());
    }

    /**
     * Example 3: Using Anthropic Claude.
     */
    public static void withAnthropic() {
        AgentResult result = BrowserAutomation.agent()
                .task("Go to wikipedia.org and find the current population of Tokyo")
                .anthropic("claude-sonnet-4-20250514")
                .run();

        System.out.println("Result: " + result.getResult());
    }

    /**
     * Example 4: Manual browser session control.
     */
    public static void manualBrowserControl() {
        try (BrowserSession session = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true))) {
            session.start();

            // Navigate manually
            session.navigateTo("https://example.com");
            System.out.println("Page title: " + session.getCurrentPage().title());

            // Extract content
            String content = session.extractContent();
            System.out.println("Content: " + content.substring(0, Math.min(content.length(), 200)));

            // Take a screenshot
            String screenshot = session.takeScreenshotBase64();
            System.out.println("Screenshot base64 length: " + screenshot.length());
        }
    }

    /**
     * Example 5: Shared browser session with agent.
     */
    public static void sharedBrowserSession() {
        try (BrowserSession session = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true))) {
            session.start();

            // Pre-navigate to a page
            session.navigateTo("https://example.com");

            // Run agent with existing session
            AgentResult result = BrowserAutomation.agent()
                    .task("Extract the main heading from this page")
                    .openAi("gpt-4o")
                    .browserSession(session)
                    .config(new AgentConfig().maxSteps(5))
                    .run();

            System.out.println("Result: " + result.getResult());
        }
    }

    /**
     * Example 6: Custom LLM provider (OpenAI-compatible).
     */
    public static void customLlmProvider() {
        OpenAiProvider provider = new OpenAiProvider(
                "your-api-key",
                "https://your-openai-compatible-endpoint/v1",
                "your-model",
                0.0,
                4096
        );

        AgentResult result = BrowserAutomation.agent()
                .task("Search for the latest news")
                .llmProvider(provider)
                .run();

        System.out.println("Result: " + result.getResult());
    }

    public static void main(String[] args) {
        // Run the simple example (requires OPENAI_API_KEY env var)
        System.out.println("=== Browser Automation Examples ===\n");

        System.out.println("--- Example 4: Manual Browser Control ---");
        manualBrowserControl();
    }
}
