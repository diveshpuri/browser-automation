# Browser Automation

A Java library for AI-driven browser automation, inspired by [browser-use](https://github.com/browser-use/browser-use). Makes websites accessible for AI agents using [Playwright for Java](https://playwright.dev/java/) and LLM providers (OpenAI, Anthropic, Azure OpenAI, Gemini, DeepSeek, Ollama, Groq, Mistral, AWS Bedrock).

## Features

- **AI Agent** - Give a task in natural language and let the AI agent control the browser to complete it
- **DOM Extraction** - Automatically extracts interactive elements from web pages with Shadow DOM support and selector scoring
- **Vision Support** - Optionally sends screenshots to vision-capable LLMs for better understanding
- **10 LLM Providers** - OpenAI, Anthropic, Azure OpenAI, Google Gemini, DeepSeek, Ollama, Groq, Mistral, AWS Bedrock with extensible provider interface
- **18 Built-in Actions** - Click, type, navigate, scroll, hover, drag-and-drop, mouse move, tabs, screenshots, and more
- **Event-Driven Architecture** - Event bus with 40+ typed events for decoupled components
- **CDP Integration** - Direct Chrome DevTools Protocol access for enhanced element interaction
- **Agent Intelligence** - Loop detection, message compaction, fallback LLM, planning system, flash mode
- **Shadow DOM & Selector Scoring** - Robust support for Angular/React with 16-strategy selector scoring
- **Playwright Script Generator** - Generates TypeScript Playwright scripts from agent execution
- **Comprehensive Logging** - Full SLF4J execution trace across all layers (Agent, Browser, LLM, DOM, Actions)
- **Fluent API** - Clean builder pattern for easy configuration
- **Manual Control** - Use `BrowserSession` directly for programmatic browser automation without an LLM

## Requirements

- Java 17+
- Maven 3.6+
- Chromium browser (installed automatically by setup script)

## Quick Setup

### macOS / Linux (one command)

```bash
git clone https://github.com/diveshpuri/browser-automation.git
cd browser-automation
chmod +x scripts/setup.sh
./scripts/setup.sh
```

### Windows (PowerShell)

```powershell
git clone https://github.com/diveshpuri/browser-automation.git
cd browser-automation
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\setup.ps1
```

The setup script will:
1. Check and install Java 17+ (via Homebrew/apt/dnf/winget/Chocolatey)
2. Check and install Maven 3.6+
3. Install Playwright Chromium browser with system dependencies
4. Build the project
5. Run the full test suite (695 tests)

See [Setup Scripts](#setup-scripts) for details.

## Manual Installation

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
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"
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
├── BrowserAutomation              # Main entry point / facade
├── agent/
│   ├── Agent                      # AI agent orchestrator (with comprehensive logging)
│   ├── AgentConfig                # Agent settings
│   ├── AgentState                 # Execution state tracking
│   ├── AgentResult                # Final result
│   ├── SystemPrompt               # LLM system prompt generator
│   ├── FlashMode                  # Stripped-down agent mode for speed
│   ├── AgentJudge                 # Agent self-evaluation
│   ├── VariableDetector           # Variable detection in conversations
│   ├── GifRecorder                # GIF recording of sessions
│   ├── compaction/                # LLM-driven history summarization
│   ├── loop/                      # Action loop detection
│   ├── planning/                  # Multi-step planning with replan-on-stall
│   ├── history/                   # Rerun history (JSON trace replay)
│   ├── sensitive/                 # Domain-scoped secret masking
│   ├── output/                    # StructuredOutputAction<T>
│   └── url/                       # URL shortening for token savings
├── browser/
│   ├── BrowserSession             # Playwright browser lifecycle (with logging)
│   ├── BrowserProfile             # Browser configuration
│   ├── BrowserState               # Current browser state snapshot
│   ├── SessionManager             # Multi-session management
│   ├── DemoMode                   # Demo mode visualization
│   ├── VideoRecorder              # Video recording
│   └── watchdog/                  # 11 specialized watchdogs
│       ├── CaptchaWatchdog        # Captcha detection + auto-wait
│       ├── DownloadsWatchdog      # Download lifecycle via CDP
│       ├── SecurityWatchdog       # Browser security policies
│       ├── StorageStateWatchdog   # Cookie/localStorage save/restore
│       ├── PermissionsWatchdog    # Auto-grant browser permissions
│       └── ...
├── dom/
│   ├── DomService                 # DOM extraction (with logging)
│   ├── DomElement                 # Single DOM element
│   ├── DomState                   # Serialized DOM state
│   ├── ShadowDomService           # Shadow DOM traversal (Angular/React)
│   ├── SelectorScorer             # 16-strategy selector scoring
│   ├── compound/                  # <select>, date/time, <details> support
│   ├── markdown/                  # Structure-aware markdown extraction
│   ├── serializer/                # 5 DOM serialization strategies
│   ├── snapshot/                  # Enhanced CDP-based DOM snapshots
│   └── tracking/                  # New node marking / DOM diffing
├── action/
│   ├── ActionRegistry             # Action registration & lookup (with logging)
│   ├── BrowserAction              # Action interface
│   ├── ActionResult               # Action execution result
│   ├── ActionParameters           # Parsed action parameters
│   └── actions/                   # 18 built-in actions (all with logging)
│       ├── NavigateAction, ClickElementAction, InputTextAction
│       ├── ScrollAction, SendKeysAction, GoBackAction
│       ├── SwitchTabAction, OpenTabAction, CloseTabAction
│       ├── ExtractContentAction, ScreenshotAction, DoneAction
│       ├── SelectDropdownAction, GetDropdownOptionsAction, WaitAction
│       └── HoverAction, DragAndDropAction, MouseMoveAction
├── llm/
│   ├── LlmProvider               # LLM provider interface
│   ├── ChatMessage                # Chat message types
│   ├── LlmResponse               # Structured LLM response
│   ├── OpenAiProvider             # OpenAI (with logging)
│   ├── AnthropicProvider          # Anthropic (with logging)
│   ├── AzureOpenAiProvider        # Azure OpenAI
│   ├── GeminiProvider             # Google Gemini (with logging)
│   ├── DeepSeekProvider           # DeepSeek (V3/R1)
│   ├── OllamaProvider             # Ollama local models
│   ├── GroqProvider               # Groq
│   ├── MistralProvider            # Mistral
│   └── AwsBedrockProvider         # AWS Bedrock
├── cdp/                           # Chrome DevTools Protocol
│   ├── CdpConnection             # Direct CDP access
│   ├── CdpElementInteractor      # Multi-fallback click strategies
│   ├── CdpDomSnapshot            # Enhanced DOM snapshots
│   └── CdpNetworkMonitor         # Network request tracking
├── event/                         # Event-driven architecture
│   └── EventBus                   # 40+ typed events, wildcard subscriptions
├── scriptgen/                     # Playwright TypeScript generator
│   ├── PlaywrightScriptGenerator  # Script generation from agent history
│   ├── ScriptGeneratorConfig      # Generator settings
│   └── ScriptRecordingAgent       # Recording agent wrapper
├── token/                         # Token management
│   ├── TokenCounter               # Token counting
│   ├── TokenPricing               # Per-model cost tracking
│   └── TokenUsageTracker          # Usage aggregation
├── concurrent/                    # Multi-agent concurrency
│   └── ParallelAgentRunner        # ExecutorService thread pool
├── observability/                 # Distributed tracing
├── codeuse/                       # Jupyter-notebook-like code execution
├── skill/                         # Skill system with JSON loading
├── cli/                           # Interactive CLI / REPL
├── telemetry/                     # Telemetry data collection
├── sandbox/                       # Sandboxed code execution
├── filesystem/                    # File system abstraction
├── exception/                     # Custom exception hierarchy
└── config/
    └── BrowserAutomationConfig    # Global configuration
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
| `GROQ_API_KEY` | - | Groq API key |
| `MISTRAL_API_KEY` | - | Mistral API key |
| `AWS_ACCESS_KEY_ID` | - | AWS Bedrock access key |
| `AWS_SECRET_ACCESS_KEY` | - | AWS Bedrock secret key |
| `AWS_REGION` | `us-east-1` | AWS Bedrock region |
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
| `hover` | Hover over an element |
| `drag_and_drop` | Drag from one element to another |
| `mouse_move` | Move mouse to coordinates |

## Logging

The library uses SLF4J with Logback for comprehensive execution logging. All critical paths are instrumented with consistent prefixed tags:

| Tag | Component | What's Logged |
|-----|-----------|---------------|
| `[AGENT]` / `[STEP N]` | Agent | Initialization, step lifecycle, completion |
| `[STATE]` | AgentState | State transitions, metrics |
| `[SESSION]` / `[NAV]` / `[CLICK]` / `[TYPE]` | BrowserSession | Session lifecycle, navigation, interactions |
| `[GEMINI]` / `[OPENAI]` / `[ANTHROPIC]` | LLM Providers | Request/response, tokens, tool calls |
| `[DOM]` | DomService | Extraction timing, element counts |
| `[ACTION:name]` | Actions | Execution with parameters, timing |
| `[REGISTRY]` | ActionRegistry | Registration, lookup failures |

### E2E Test Log Capture

The E2E test (`BrowserUseE2ETest`) automatically captures all SLF4J logs into `execution-log.txt` using a programmatic logback `FileAppender`. The log file contains:

```
=== E2E Test Execution Log ===
Timestamp: 2026-03-01 12:00:00
Task: Find the number of stars of the browser-use repo on github
Provider: Google Gemini (gemini-3-flash-preview)
==============================

--- Comprehensive SLF4J Execution Trace ---

04:37:00.123 [main] INFO  c.b.agent.Agent - ========================================
04:37:00.124 [main] INFO  c.b.agent.Agent - AGENT STARTING
04:37:00.124 [main] INFO  c.b.agent.Agent -   Task: Find the number of stars...
04:37:00.125 [main] INFO  c.b.agent.Agent -   LLM Provider: gemini (model=gemini-3-flash-preview)
04:37:00.500 [main] INFO  c.b.browser.BrowserSession - [SESSION] Starting browser session
04:37:01.200 [main] INFO  c.b.dom.DomService - [DOM] Extracted 42 interactive elements in 150ms
04:37:02.000 [main] INFO  c.b.llm.GeminiProvider - [GEMINI] API responded in 800ms
...

--- End of SLF4J Execution Trace ---

--- Execution Summary ---
Status: SUCCESS
Result: The browser-use repo has 65.2k stars
Total Steps: 3
Total Tokens Used: 4521
```

## Setup Scripts

### `scripts/setup.sh` (macOS / Linux)

Works on macOS, Ubuntu/Debian, Fedora/RHEL/CentOS, Arch, openSUSE, and any Linux with SDKMAN fallback.

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

**What it does:**

| Step | Action | macOS | Ubuntu/Debian | Fedora/RHEL | Arch |
|------|--------|-------|---------------|-------------|------|
| 1 | Java 17+ | `brew install openjdk@17` | `apt install openjdk-17-jdk` | `dnf install java-17-openjdk-devel` | `pacman -S jdk17-openjdk` |
| 2 | Maven 3.6+ | `brew install maven` | `apt install maven` | `dnf install maven` | `pacman -S maven` |
| 3 | Playwright | `mvn exec:java ... install chromium` | Same + system deps (libnss3, libatk, etc.) | Same | Same |
| 4 | Build | `mvn clean compile test-compile` | Same | Same | Same |
| 5 | Tests | `mvn test` (695 tests) | Same | Same | Same |

Falls back to SDKMAN for Java and manual download for Maven if the package manager version is too old.

### `scripts/setup.ps1` (Windows)

Requires PowerShell. Supports winget and Chocolatey.

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\setup.ps1
```

**What it does:**

| Step | Action | winget | Chocolatey |
|------|--------|--------|------------|
| 1 | Java 17+ | `winget install Microsoft.OpenJDK.17` | `choco install openjdk17` |
| 2 | Maven 3.6+ | `winget install Apache.Maven` | `choco install maven` |
| 3 | Playwright | `mvn exec:java ... install chromium` | Same |
| 4 | Build | `mvn clean compile test-compile` | Same |
| 5 | Tests | `mvn test` (695 tests) | Same |

Falls back to manual Maven download if neither package manager is available.

## Running E2E Tests

The E2E tests require an LLM API key. They are excluded from the default `mvn test` run.

```bash
# With Google Gemini
GEMINI_API_KEY=your-key mvn test -Dgroups=e2e

# With OpenAI
OPENAI_API_KEY=your-key mvn test -Dgroups=e2e

# With Anthropic
ANTHROPIC_API_KEY=your-key mvn test -Dgroups=e2e
```

### E2E Artifacts

Each E2E run generates artifacts in `target/e2e-artifacts/`:

| File | Description |
|------|-------------|
| `execution-log.txt` | Full SLF4J execution trace + summary + step details |
| `execution-summary.json` | Machine-readable JSON (result, steps, tokens, duration) |
| `generated-test.spec.ts` | Playwright TypeScript script with dynamic waits |
| `videos/*.webm` | Video recording of the browser session |

### Scheduled CI Pipeline

The GitHub Actions workflow (`.github/workflows/e2e-scheduled.yml`) runs E2E tests every hour with email notifications. Configure these secrets in your repo:

| Secret | Description |
|--------|-------------|
| `GEMINI_API_KEY` | Google Gemini API key |
| `EMAIL_USERNAME` | Gmail address for notifications |
| `EMAIL_PASSWORD` | Gmail App Password ([generate here](https://myaccount.google.com/apppasswords)) |
| `NOTIFICATION_EMAIL` | Email to receive notifications |

## Building

```bash
# Full build with tests
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run only unit tests (excludes E2E)
mvn test

# Run E2E tests
GEMINI_API_KEY=your-key mvn test -Dgroups=e2e
```

## Project Structure

```
browser-automation/
├── scripts/
│   ├── setup.sh               # macOS/Linux setup script
│   └── setup.ps1              # Windows setup script
├── src/
│   ├── main/java/com/browserautomation/
│   │   ├── agent/             # AI agent orchestration
│   │   ├── browser/           # Browser session & watchdogs
│   │   ├── dom/               # DOM extraction & processing
│   │   ├── action/            # Action registry & implementations
│   │   ├── llm/               # 10 LLM provider implementations
│   │   ├── cdp/               # Chrome DevTools Protocol
│   │   ├── event/             # Event bus architecture
│   │   ├── scriptgen/         # Playwright script generator
│   │   └── ...
│   ├── main/resources/
│   │   └── logback.xml        # Logging configuration
│   └── test/java/com/browserautomation/
│       ├── e2e/               # End-to-end tests
│       └── ...                # 695 unit tests
├── examples/
│   ├── BasicExample.java      # Simple usage example
│   └── EndToEndExample.java   # Full E2E example with all providers
├── .github/workflows/
│   ├── ci.yml                 # CI build on push/PR
│   └── e2e-scheduled.yml      # Hourly E2E tests with notifications
├── pom.xml                    # Maven configuration
└── README.md
```

## License

MIT
