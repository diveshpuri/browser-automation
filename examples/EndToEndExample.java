import com.browserautomation.BrowserAutomation;
import com.browserautomation.agent.AgentConfig;
import com.browserautomation.agent.AgentResult;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;

/**
 * End-to-End example demonstrating browser-automation usage.
 *
 * This is the Java equivalent of the Python browser-use example:
 *
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
 * Before running, set one of these environment variables:
 *   export GEMINI_API_KEY=your-gemini-key
 *   export OPENAI_API_KEY=your-openai-key
 *   export ANTHROPIC_API_KEY=your-anthropic-key
 */
public class EndToEndExample {

    /**
     * Example 1: Using Google Gemini (mirrors the Python example exactly).
     *
     * Python equivalent:
     *   agent = Agent(
     *       task="Find the number of stars of the browser-use repo",
     *       llm=ChatGoogle(model='gemini-3-flash-preview'),
     *       browser=browser,
     *   )
     */
    public static void withGemini() {
        // Browser(use_cloud=False) equivalent - local browser
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            // Agent(task=..., llm=ChatGoogle(...), browser=browser)
            AgentResult result = BrowserAutomation.agent()
                    .task("Find the number of stars of the browser-use repo")
                    .gemini("gemini-2.0-flash-exp")
                    .browserSession(browser)
                    .run();

            System.out.println("Success: " + result.isSuccess());
            System.out.println("Result: " + result.getResult());
            System.out.println("Steps taken: " + result.getTotalSteps());
            System.out.println("Total tokens: " + result.getTotalTokensUsed());
            System.out.println("Duration: " + result.getTotalDurationMs() + "ms");
        } finally {
            browser.close();
        }
    }

    /**
     * Example 2: Same task using OpenAI (alternative LLM).
     *
     * Python equivalent:
     *   agent = Agent(
     *       task="Find the number of stars of the browser-use repo",
     *       llm=ChatOpenAI(model='gpt-4o'),
     *       browser=browser,
     *   )
     */
    public static void withOpenAi() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task("Find the number of stars of the browser-use repo")
                    .openAi("gpt-4o")
                    .browserSession(browser)
                    .run();

            System.out.println("Success: " + result.isSuccess());
            System.out.println("Result: " + result.getResult());
        } finally {
            browser.close();
        }
    }

    /**
     * Example 3: Same task using Anthropic Claude.
     *
     * Python equivalent:
     *   agent = Agent(
     *       task="Find the number of stars of the browser-use repo",
     *       llm=ChatAnthropic(model='claude-sonnet-4-6'),
     *       browser=browser,
     *   )
     */
    public static void withAnthropic() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task("Find the number of stars of the browser-use repo")
                    .anthropic("claude-sonnet-4-20250514")
                    .browserSession(browser)
                    .run();

            System.out.println("Success: " + result.isSuccess());
            System.out.println("Result: " + result.getResult());
        } finally {
            browser.close();
        }
    }

    /**
     * Example 4: Same task using DeepSeek (cost-effective alternative).
     */
    public static void withDeepSeek() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task("Find the number of stars of the browser-use repo")
                    .deepSeek("deepseek-chat")
                    .browserSession(browser)
                    .config(new AgentConfig().useVision(false))
                    .run();

            System.out.println("Success: " + result.isSuccess());
            System.out.println("Result: " + result.getResult());
        } finally {
            browser.close();
        }
    }

    /**
     * Example 5: Same task using Ollama (fully local, no API key needed).
     */
    public static void withOllama() {
        BrowserSession browser = BrowserAutomation.createBrowserSession(
                new BrowserProfile().headless(true));
        browser.start();

        try {
            AgentResult result = BrowserAutomation.agent()
                    .task("Find the number of stars of the browser-use repo")
                    .ollama("qwen2.5")
                    .browserSession(browser)
                    .run();

            System.out.println("Success: " + result.isSuccess());
            System.out.println("Result: " + result.getResult());
        } finally {
            browser.close();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Browser Automation E2E Example ===");
        System.out.println("Task: Find the number of stars of the browser-use repo\n");

        // Detect which LLM provider is available and run with it
        String geminiKey = System.getenv("GEMINI_API_KEY");
        String openaiKey = System.getenv("OPENAI_API_KEY");
        String anthropicKey = System.getenv("ANTHROPIC_API_KEY");
        String deepseekKey = System.getenv("DEEPSEEK_API_KEY");

        if (geminiKey != null && !geminiKey.isEmpty()) {
            System.out.println("Using Google Gemini...\n");
            withGemini();
        } else if (openaiKey != null && !openaiKey.isEmpty()) {
            System.out.println("Using OpenAI GPT-4o...\n");
            withOpenAi();
        } else if (anthropicKey != null && !anthropicKey.isEmpty()) {
            System.out.println("Using Anthropic Claude...\n");
            withAnthropic();
        } else if (deepseekKey != null && !deepseekKey.isEmpty()) {
            System.out.println("Using DeepSeek...\n");
            withDeepSeek();
        } else {
            System.out.println("No API key found. Set one of:");
            System.out.println("  export GEMINI_API_KEY=your-key");
            System.out.println("  export OPENAI_API_KEY=your-key");
            System.out.println("  export ANTHROPIC_API_KEY=your-key");
            System.out.println("  export DEEPSEEK_API_KEY=your-key");
            System.out.println("\nOr use Ollama (no key needed):");
            System.out.println("  ollama serve && ollama pull qwen2.5");
        }
    }
}
