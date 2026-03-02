# Browser Automation

A Java library for AI-driven browser automation, inspired by [browser-use](https://github.com/browser-use/browser-use). Makes websites accessible for AI agents using [Playwright for Java](https://playwright.dev/java/) and LLM providers (OpenAI, Anthropic, Azure OpenAI, Gemini, DeepSeek, Ollama).

## Features

- **AI Agent** - Give a task in natural language and let the AI agent control the browser to complete it
- **DOM Extraction** - Automatically extracts interactive elements from web pages and presents them to the LLM
- **Vision Support** - Optionally sends screenshots to vision-capable LLMs for better understanding
- **Multiple LLM Providers** - Built-in support for OpenAI, Anthropic, Azure OpenAI, Google Gemini, DeepSeek (V3/R1), and Ollama (local models), with an extensible provider interface
- **Action Registry** - 15+ built-in browser actions (click, type, navigate, scroll, etc.) with support for custom actions
- **Fluent API** - Clean builder pattern for easy configuration
- **Manual Control** - Use `BrowserSession` directly for programmatic browser automation without an LLM

## Requirements

- Java 17+
- Maven 3.6+

## Installation

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.browserautomation</groupId>
    <artifactId>browser-automation</artifactId>
    <version>0.1.0</version>
</dependency>
```

Then install Playwright browsers:

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

## Quick Start

### AI Agent (Simplest Usage)

```java
import com.browserautomation.BrowserAutomation;
import com.browserautomation.agent.AgentResult;

// Set OPENAI_API_KEY environment variable
AgentResult result = BrowserAutomation.agent()
    .task("Go to google.com and search for 'Java browser automation'")
    .openAi("gpt-4o")
    .run();

System.out.println("Result: " + result.getResult());
```

### With Anthropic Claude

```java
AgentResult result = BrowserAutomation.agent()
    .task("Find the current population of Tokyo on Wikipedia")
    .anthropic("claude-sonnet-4-20250514")
    .run();
```

### With Azure OpenAI

```java
// Set AZURE_OPENAI_KEY and AZURE_OPENAI_ENDPOINT environment variables
AgentResult result = BrowserAutomation.agent()
    .task("Find the latest stock price of AAPL")
    .azureOpenAi("gpt-4o")
    .run();

// Or provide credentials explicitly
AgentResult result = BrowserAutomation.agent()
    .task("Find the latest stock price of AAPL")
    .azureOpenAi("gpt-4o", "your-api-key", "https://your-endpoint.openai.azure.com/")
    .run();
```

### With Google Gemini

```java
// Set GEMINI_API_KEY environment variable
AgentResult result = BrowserAutomation.agent()
    .task("Summarize the top news on BBC")
    .gemini("gemini-3-flash-preview")
    .run();
```

### With DeepSeek

```java
// Set DEEPSEEK_API_KEY environment variable
// DeepSeek-V3 (30x cheaper than GPT-4o, no rate limits)
AgentResult result = BrowserAutomation.agent()
    .task("Compare prices of flights from NYC to London")
    .deepSeek("deepseek-chat")
    .config(new AgentConfig().useVision(false)) // DeepSeek doesn't support vision
    .run();

// DeepSeek-R1 (reasoning model)
AgentResult result = BrowserAutomation.agent()
    .task("Analyze the pricing table on this page")
    .deepSeek("deepseek-reasoner")
    .config(new AgentConfig().useVision(false))
    .run();
```

### With Ollama (Local Models)

```java
// No API key needed - runs locally
// 1. Install Ollama: https://ollama.ai
// 2. Pull a model: ollama pull qwen2.5
// 3. Start Ollama: ollama serve
AgentResult result = BrowserAutomation.agent()
    .task("Extract the main content from this page")
    .ollama("qwen2.5")
    .run();

// With custom server URL
AgentResult result = BrowserAutomation.agent()
    .task("Extract the main content from this page")
    .ollama("llama3.1", "http://my-server:11434")
    .run();
```

### Custom Configuration

```java
import com.browserautomation.agent.AgentConfig;
import com.browserautomation.browser.BrowserProfile;

AgentResult result = BrowserAutomation.agent()
    .task("Find the top 5 trending repositories on GitHub")
    .openAi("gpt-4o")
    .browserProfile(new BrowserProfile()
        .headless(true)
        .viewportSize(1920, 1080)
        .disableSecurity(false))
    .config(new AgentConfig()
        .maxSteps(30)
        .maxFailures(3)
        .useVision(true)
        .maxActionsPerStep(5))
    .run();
```

### Manual Browser Control (No LLM)

```java
import com.browserautomation.browser.BrowserSession;
import com.browserautomation.browser.BrowserProfile;

try (BrowserSession session = BrowserAutomation.createBrowserSession(
        new BrowserProfile().headless(true))) {
    session.start();

    session.navigateTo("https://example.com");
    System.out.println("Title: " + session.getCurrentPage().title());

    String content = session.extractContent();
    System.out.println("Content: " + content);

    String screenshot = session.takeScreenshotBase64();
}
```

### Shared Browser Session

```java
try (BrowserSession session = BrowserAutomation.createBrowserSession()) {
    session.start();
    session.navigateTo("https://example.com");

    // Run agent with existing session
    AgentResult result = BrowserAutomation.agent()
        .task("Extract the main heading from this page")
        .openAi("gpt-4o")
        .browserSession(session)
        .config(new AgentConfig().maxSteps(5))
        .run();
}
```

### Custom Actions

```java
import com.browserautomation.action.*;

ActionRegistry registry = new ActionRegistry();
registry.register(new BrowserAction() {
    @Override public String getName() { return "highlight"; }
    @Override public String getDescription() { return "Highlight an element on the page"; }
    @Override public String getParameterSchema() { return "{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\"}}}"; }
    @Override public ActionResult execute(BrowserSession session, ActionParameters params) {
        int index = params.getInt("index", 0);
        session.executeJavaScript("document.querySelector('*')?.style.border = '3px solid red'");
        return ActionResult.success("Element highlighted");
    }
});

AgentResult result = BrowserAutomation.agent()
    .task("Highlight the search box on google.com")
    .openAi("gpt-4o")
    .actionRegistry(registry)
    .run();
```

### Custom LLM Provider

```java
import com.browserautomation.llm.OpenAiProvider;

// Works with any OpenAI-compatible API
OpenAiProvider provider = new OpenAiProvider(
    "your-api-key",
    "https://your-endpoint/v1",
    "your-model",
    0.0,   // temperature
    4096   // max tokens
);

AgentResult result = BrowserAutomation.agent()
    .task("Search for the latest news")
    .llmProvider(provider)
    .run();
```

## Architecture

```
com.browserautomation
├── BrowserAutomation          # Main entry point / facade
├── agent/
│   ├── Agent                  # AI agent orchestrator
│   ├── AgentConfig            # Agent settings
│   ├── AgentState             # Execution state tracking
│   ├── AgentResult            # Final result
│   └── SystemPrompt           # LLM system prompt generator
├── browser/
│   ├── BrowserSession         # Playwright browser lifecycle
│   ├── BrowserProfile         # Browser configuration
│   └── BrowserState           # Current browser state snapshot
├── dom/
│   ├── DomService             # DOM extraction from pages
│   ├── DomElement             # Single DOM element
│   └── DomState               # Serialized DOM state
├── action/
│   ├── ActionRegistry         # Action registration & lookup
│   ├── BrowserAction          # Action interface
│   ├── ActionResult           # Action execution result
│   ├── ActionParameters       # Parsed action parameters
│   └── actions/               # Built-in action implementations
│       ├── NavigateAction
│       ├── ClickElementAction
│       ├── InputTextAction
│       ├── ScrollAction
│       ├── SendKeysAction
│       ├── GoBackAction
│       ├── SwitchTabAction
│       ├── OpenTabAction
│       ├── CloseTabAction
│       ├── ExtractContentAction
│       ├── ScreenshotAction
│       ├── SelectDropdownAction
│       ├── GetDropdownOptionsAction
│       ├── WaitAction
│       └── DoneAction
├── llm/
│   ├── LlmProvider            # LLM provider interface
│   ├── ChatMessage             # Chat message types
│   ├── LlmResponse            # Structured LLM response
│   ├── OpenAiProvider         # OpenAI implementation
│   ├── AnthropicProvider      # Anthropic implementation
│   ├── AzureOpenAiProvider    # Azure OpenAI implementation
│   ├── GeminiProvider         # Google Gemini implementation
│   ├── DeepSeekProvider       # DeepSeek (V3/R1) implementation
│   └── OllamaProvider         # Ollama local model implementation
└── config/
    └── BrowserAutomationConfig # Global configuration
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | - | OpenAI API key |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | OpenAI API base URL |
| `ANTHROPIC_API_KEY` | - | Anthropic API key |
| `ANTHROPIC_BASE_URL` | `https://api.anthropic.com/v1` | Anthropic API base URL |
| `AZURE_OPENAI_KEY` | - | Azure OpenAI API key |
| `AZURE_OPENAI_ENDPOINT` | - | Azure OpenAI endpoint URL |
| `AZURE_OPENAI_API_VERSION` | `2024-10-21` | Azure OpenAI API version |
| `GEMINI_API_KEY` | - | Google Gemini API key |
| `DEEPSEEK_API_KEY` | - | DeepSeek API key |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `BROWSER_AUTOMATION_HEADLESS` | `true` | Run browser in headless mode |
| `BROWSER_AUTOMATION_TIMEOUT` | `30000` | Default timeout in milliseconds |
| `BROWSER_AUTOMATION_MODEL` | `gpt-4o` | Default LLM model |

## Built-in Actions

| Action | Description |
|--------|-------------|
| `navigate` | Navigate to a URL |
| `click` | Click an element by index |
| `input_text` | Type text into an element |
| `scroll` | Scroll up or down |
| `go_back` | Go back in history |
| `switch_tab` | Switch to a tab |
| `close_tab` | Close a tab |
| `open_tab` | Open a new tab |
| `send_keys` | Send keyboard keys |
| `extract_content` | Extract page text |
| `screenshot` | Take a screenshot |
| `select_dropdown_option` | Select from dropdown |
| `get_dropdown_options` | List dropdown options |
| `wait` | Wait for seconds |
| `done` | Signal task completion |

## Building

```bash
mvn clean install
```

## License

MIT
