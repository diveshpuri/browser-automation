package com.browserautomation.cli;

import com.browserautomation.BrowserAutomation;
import com.browserautomation.agent.AgentConfig;
import com.browserautomation.agent.AgentResult;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.config.BrowserAutomationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Command-line interface for the browser-automation library.
 *
 * <p>Provides an interactive REPL for running browser automation tasks,
 * and a single-command mode for scripting.</p>
 *
 * <pre>
 * Usage:
 *   java -cp browser-automation.jar com.browserautomation.cli.BrowserAutomationCli [options]
 *
 * Options:
 *   --task "task"        Run a single task and exit
 *   --provider name      LLM provider (openai, anthropic, gemini, deepseek, ollama, azure)
 *   --model name         Model name (e.g., gpt-4o, claude-sonnet-4-20250514)
 *   --headless           Run browser in headless mode (default: true)
 *   --no-headless        Run browser with visible UI
 *   --vision             Enable vision/screenshots (default: true)
 *   --no-vision          Disable vision/screenshots
 *   --max-steps N        Maximum steps per task (default: 50)
 *   --interactive        Start interactive REPL mode
 *   --help               Show this help message
 * </pre>
 */
public class BrowserAutomationCli {

    private static final Logger logger = LoggerFactory.getLogger(BrowserAutomationCli.class);

    private String provider = "openai";
    private String model = "gpt-4o";
    private boolean headless = true;
    private boolean useVision = true;
    private int maxSteps = 50;
    private String task = null;
    private boolean interactive = false;

    public static void main(String[] args) {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(args);
        cli.run();
    }

    void parseArgs(String[] args) {
        List<String> argList = Arrays.asList(args);
        for (int i = 0; i < argList.size(); i++) {
            String arg = argList.get(i);
            switch (arg) {
                case "--task" -> {
                    if (i + 1 < argList.size()) task = argList.get(++i);
                }
                case "--provider" -> {
                    if (i + 1 < argList.size()) provider = argList.get(++i);
                }
                case "--model" -> {
                    if (i + 1 < argList.size()) model = argList.get(++i);
                }
                case "--headless" -> headless = true;
                case "--no-headless" -> headless = false;
                case "--vision" -> useVision = true;
                case "--no-vision" -> useVision = false;
                case "--max-steps" -> {
                    if (i + 1 < argList.size()) maxSteps = Integer.parseInt(argList.get(++i));
                }
                case "--interactive" -> interactive = true;
                case "--help", "-h" -> {
                    printHelp();
                    System.exit(0);
                }
                default -> {
                    if (!arg.startsWith("--")) {
                        // Treat as task if no --task flag was given
                        if (task == null) task = arg;
                    }
                }
            }
        }

        // Default to interactive if no task given
        if (task == null && !interactive) {
            interactive = true;
        }
    }

    void run() {
        printBanner();

        if (task != null) {
            runTask(task);
        }

        if (interactive) {
            runInteractive();
        }
    }

    private void runInteractive() {
        System.out.println("Interactive mode. Type a task and press Enter. Type 'quit' to exit.\n");
        System.out.println("Provider: " + provider + " | Model: " + model
                + " | Headless: " + headless + " | Vision: " + useVision);
        System.out.println("Commands: /provider <name>, /model <name>, /headless, /vision, /help, /quit\n");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while (true) {
                System.out.print("browser-automation> ");
                System.out.flush();
                line = reader.readLine();
                if (line == null) break;
                line = line.trim();

                if (line.isEmpty()) continue;

                if (line.startsWith("/")) {
                    if (!handleCommand(line)) break;
                    continue;
                }

                runTask(line);
                System.out.println();
            }
        } catch (IOException e) {
            logger.error("Error reading input: {}", e.getMessage());
        }
    }

    private boolean handleCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/quit", "/exit", "/q" -> {
                System.out.println("Goodbye!");
                return false;
            }
            case "/provider" -> {
                if (parts.length > 1) {
                    provider = parts[1];
                    System.out.println("Provider set to: " + provider);
                } else {
                    System.out.println("Current provider: " + provider);
                }
            }
            case "/model" -> {
                if (parts.length > 1) {
                    model = parts[1];
                    System.out.println("Model set to: " + model);
                } else {
                    System.out.println("Current model: " + model);
                }
            }
            case "/headless" -> {
                headless = !headless;
                System.out.println("Headless: " + headless);
            }
            case "/vision" -> {
                useVision = !useVision;
                System.out.println("Vision: " + useVision);
            }
            case "/status" -> {
                System.out.println("Provider: " + provider + " | Model: " + model
                        + " | Headless: " + headless + " | Vision: " + useVision
                        + " | Max Steps: " + maxSteps);
            }
            case "/help" -> printInteractiveHelp();
            default -> System.out.println("Unknown command: " + cmd + ". Type /help for available commands.");
        }
        return true;
    }

    private void runTask(String taskDescription) {
        System.out.println("\nRunning task: " + taskDescription);
        System.out.println("Provider: " + provider + " | Model: " + model + "\n");

        long start = System.currentTimeMillis();
        try {
            BrowserAutomation.AgentBuilder builder = BrowserAutomation.agent()
                    .task(taskDescription)
                    .browserProfile(new BrowserProfile().headless(headless))
                    .config(new AgentConfig()
                            .maxSteps(maxSteps)
                            .useVision(useVision));

            // Set provider
            switch (provider.toLowerCase()) {
                case "openai" -> builder.openAi(model);
                case "anthropic" -> builder.anthropic(model);
                case "gemini" -> builder.gemini(model);
                case "deepseek" -> builder.deepSeek(model);
                case "ollama" -> builder.ollama(model);
                case "azure", "azure-openai" -> builder.azureOpenAi(model);
                default -> {
                    System.err.println("Unknown provider: " + provider + ". Using OpenAI.");
                    builder.openAi(model);
                }
            }

            AgentResult result = builder.run();

            long duration = System.currentTimeMillis() - start;
            System.out.println("--- Result ---");
            System.out.println("Success: " + result.isSuccess());
            System.out.println("Output: " + (result.getResult() != null ? result.getResult() : "(no output)"));
            System.out.println("Steps: " + result.getTotalSteps());
            System.out.println("Tokens: " + result.getTotalTokensUsed());
            System.out.println("Duration: " + duration + "ms");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            logger.error("Task execution failed", e);
        }
    }

    private void printBanner() {
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║    Browser Automation CLI v" + BrowserAutomation.VERSION + "  ║");
        System.out.println("║    AI-Driven Browser Control     ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println();
    }

    private void printHelp() {
        System.out.println("Browser Automation CLI - AI-Driven Browser Control\n");
        System.out.println("Usage:");
        System.out.println("  browser-automation [options] [task]\n");
        System.out.println("Options:");
        System.out.println("  --task \"task\"        Run a single task and exit");
        System.out.println("  --provider name      LLM provider (openai, anthropic, gemini, deepseek, ollama, azure)");
        System.out.println("  --model name         Model name (e.g., gpt-4o, claude-sonnet-4-20250514)");
        System.out.println("  --headless           Run browser in headless mode (default)");
        System.out.println("  --no-headless        Run browser with visible UI");
        System.out.println("  --vision             Enable vision/screenshots (default)");
        System.out.println("  --no-vision          Disable vision/screenshots");
        System.out.println("  --max-steps N        Maximum steps per task (default: 50)");
        System.out.println("  --interactive        Start interactive REPL mode");
        System.out.println("  --help, -h           Show this help message\n");
        System.out.println("Examples:");
        System.out.println("  browser-automation --task \"Search for Java tutorials on Google\"");
        System.out.println("  browser-automation --provider gemini --model gemini-3-flash-preview --interactive");
        System.out.println("  browser-automation --provider ollama --model qwen2.5 \"Find the weather in NYC\"");
    }

    private void printInteractiveHelp() {
        System.out.println("Interactive Commands:");
        System.out.println("  /provider <name>  - Switch LLM provider");
        System.out.println("  /model <name>     - Switch model");
        System.out.println("  /headless         - Toggle headless mode");
        System.out.println("  /vision           - Toggle vision support");
        System.out.println("  /status           - Show current settings");
        System.out.println("  /help             - Show this help");
        System.out.println("  /quit             - Exit");
        System.out.println("\nJust type a task description to execute it.");
    }

    // Package-private getters for testing
    String getProvider() { return provider; }
    String getModel() { return model; }
    boolean isHeadless() { return headless; }
    boolean isUseVision() { return useVision; }
    int getMaxSteps() { return maxSteps; }
    String getTask() { return task; }
    boolean isInteractive() { return interactive; }
}
