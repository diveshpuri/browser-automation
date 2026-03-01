package com.browserautomation.scriptgen;

import com.browserautomation.agent.AgentResult;
import com.browserautomation.agent.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates Playwright scripts in TypeScript format from agent execution history.
 *
 * Converts the recorded browser automation steps into a standalone TypeScript
 * Playwright script with all required dynamic waits to ensure successful execution.
 *
 * Features:
 * - Generates TypeScript Playwright test scripts
 * - Adds dynamic waits (waitForSelector, waitForLoadState, waitForNavigation, etc.)
 * - Handles navigation, clicks, typing, scrolling, tab management
 * - Includes error handling and retry logic
 * - Configurable timeouts and wait strategies
 */
public class PlaywrightScriptGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightScriptGenerator.class);

    private final ScriptGeneratorConfig config;
    private final List<RecordedAction> recordedActions;

    public PlaywrightScriptGenerator() {
        this(new ScriptGeneratorConfig());
    }

    public PlaywrightScriptGenerator(ScriptGeneratorConfig config) {
        this.config = config;
        this.recordedActions = new ArrayList<>();
    }

    /**
     * Record a browser action for later script generation.
     *
     * @param actionName the name of the action
     * @param params     the action parameters
     * @param selector   the CSS selector (if applicable)
     * @param url        the current page URL
     */
    public void recordAction(String actionName, Map<String, Object> params,
                             String selector, String url) {
        recordedActions.add(new RecordedAction(actionName, params, selector, url, System.currentTimeMillis()));
    }

    /**
     * Generate a TypeScript Playwright script from the recorded actions.
     *
     * @return the generated TypeScript code
     */
    public String generateScript() {
        StringBuilder script = new StringBuilder();

        // Generate imports and setup
        appendImports(script);
        appendTestSetup(script);

        // Generate action steps
        for (int i = 0; i < recordedActions.size(); i++) {
            RecordedAction action = recordedActions.get(i);
            appendAction(script, action, i + 1);
        }

        // Generate teardown
        appendTestTeardown(script);

        return script.toString();
    }

    /**
     * Generate a TypeScript Playwright script from an AgentResult's execution history.
     *
     * @param result the agent result with execution history
     * @param task   the original task description
     * @return the generated TypeScript code
     */
    public String generateFromAgentResult(AgentResult result, String task) {
        StringBuilder script = new StringBuilder();

        appendImports(script);
        appendHeader(script, task, result);
        appendTestSetupFromResult(script, task);

        List<AgentState.AgentStep> steps = result.getHistory();
        for (int i = 0; i < steps.size(); i++) {
            AgentState.AgentStep step = steps.get(i);
            appendStepFromHistory(script, step, i + 1);
        }

        appendTestTeardown(script);
        return script.toString();
    }

    /**
     * Generate the script and save to a file.
     *
     * @param outputPath the file path to save the script
     * @return the path to the saved file
     */
    public Path generateAndSave(Path outputPath) throws IOException {
        String script = generateScript();
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, script, StandardCharsets.UTF_8);
        logger.info("Playwright script saved to: {}", outputPath);
        return outputPath;
    }

    /**
     * Generate the script from agent result and save to a file.
     */
    public Path generateAndSave(AgentResult result, String task, Path outputPath) throws IOException {
        String script = generateFromAgentResult(result, task);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, script, StandardCharsets.UTF_8);
        logger.info("Playwright script saved to: {}", outputPath);
        return outputPath;
    }

    /**
     * Clear all recorded actions.
     */
    public void clearRecordedActions() {
        recordedActions.clear();
    }

    /**
     * Get the number of recorded actions.
     */
    public int getRecordedActionCount() {
        return recordedActions.size();
    }

    // --- Private script generation methods ---

    private void appendImports(StringBuilder sb) {
        sb.append("import { test, expect, Page, BrowserContext } from '@playwright/test';\n\n");
    }

    private void appendHeader(StringBuilder sb, String task, AgentResult result) {
        sb.append("/**\n");
        sb.append(" * Auto-generated Playwright TypeScript script\n");
        sb.append(" * Generated by browser-automation library\n");
        sb.append(" * Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        sb.append(" *\n");
        sb.append(" * Original task: ").append(task != null ? task : "N/A").append("\n");
        sb.append(" * Steps: ").append(result.getTotalSteps()).append("\n");
        sb.append(" * Success: ").append(result.isSuccess()).append("\n");
        sb.append(" */\n\n");
    }

    private void appendTestSetup(StringBuilder sb) {
        sb.append("test.describe('Browser Automation Script', () => {\n");
        sb.append("  let page: Page;\n");
        sb.append("  let context: BrowserContext;\n\n");
        sb.append("  test.beforeEach(async ({ browser }) => {\n");
        sb.append("    context = await browser.newContext({\n");
        sb.append("      viewport: { width: ").append(config.getViewportWidth())
                .append(", height: ").append(config.getViewportHeight()).append(" },\n");
        if (config.getUserAgent() != null) {
            sb.append("      userAgent: '").append(escapeString(config.getUserAgent())).append("',\n");
        }
        sb.append("    });\n");
        sb.append("    page = await context.newPage();\n");
        sb.append("    page.setDefaultTimeout(").append(config.getDefaultTimeoutMs()).append(");\n");
        sb.append("    page.setDefaultNavigationTimeout(").append(config.getNavigationTimeoutMs()).append(");\n");
        sb.append("  });\n\n");
        sb.append("  test.afterEach(async () => {\n");
        sb.append("    await context.close();\n");
        sb.append("  });\n\n");
        sb.append("  test('automated browser task', async () => {\n");
    }

    private void appendTestSetupFromResult(StringBuilder sb, String task) {
        sb.append("test.describe('").append(escapeString(truncate(task, 80))).append("', () => {\n");
        sb.append("  let page: Page;\n");
        sb.append("  let context: BrowserContext;\n\n");
        sb.append("  test.beforeEach(async ({ browser }) => {\n");
        sb.append("    context = await browser.newContext({\n");
        sb.append("      viewport: { width: ").append(config.getViewportWidth())
                .append(", height: ").append(config.getViewportHeight()).append(" },\n");
        sb.append("    });\n");
        sb.append("    page = await context.newPage();\n");
        sb.append("    page.setDefaultTimeout(").append(config.getDefaultTimeoutMs()).append(");\n");
        sb.append("    page.setDefaultNavigationTimeout(").append(config.getNavigationTimeoutMs()).append(");\n");
        sb.append("  });\n\n");
        sb.append("  test.afterEach(async () => {\n");
        sb.append("    await context.close();\n");
        sb.append("  });\n\n");
        sb.append("  test('execute task', async () => {\n");
    }

    private void appendTestTeardown(StringBuilder sb) {
        sb.append("  });\n");
        sb.append("});\n\n");
        appendHelperFunctions(sb);
    }

    private void appendAction(StringBuilder sb, RecordedAction action, int stepNumber) {
        sb.append("\n    // Step ").append(stepNumber).append(": ").append(action.actionName).append("\n");

        switch (action.actionName) {
            case "go_to_url", "navigate" -> appendNavigateAction(sb, action);
            case "click_element", "click" -> appendClickAction(sb, action);
            case "input_text", "type" -> appendTypeAction(sb, action);
            case "scroll_down" -> appendScrollAction(sb, action, true);
            case "scroll_up" -> appendScrollAction(sb, action, false);
            case "go_back" -> appendGoBackAction(sb);
            case "send_keys" -> appendSendKeysAction(sb, action);
            case "switch_tab" -> appendSwitchTabAction(sb, action);
            case "open_tab" -> appendOpenTabAction(sb, action);
            case "close_tab" -> appendCloseTabAction(sb);
            case "hover_element", "hover" -> appendHoverAction(sb, action);
            case "drag_and_drop" -> appendDragDropAction(sb, action);
            case "mouse_move" -> appendMouseMoveAction(sb, action);
            case "select_dropdown_option" -> appendSelectAction(sb, action);
            case "upload_file" -> appendUploadAction(sb, action);
            case "extract_content" -> appendExtractAction(sb);
            case "done" -> appendDoneAction(sb, action);
            default -> appendGenericAction(sb, action);
        }
    }

    private void appendStepFromHistory(StringBuilder sb, AgentState.AgentStep step, int stepNumber) {
        String actionName = step.getActionName();
        if (actionName == null || actionName.equals("none") || actionName.equals("error")) {
            sb.append("\n    // Step ").append(stepNumber).append(": ").append(actionName != null ? actionName : "skipped")
                    .append(" (no browser action)\n");
            return;
        }

        sb.append("\n    // Step ").append(stepNumber).append(": ").append(actionName).append("\n");
        if (step.getLlmThinking() != null) {
            sb.append("    // LLM reasoning: ").append(truncate(step.getLlmThinking(), 100)).append("\n");
        }

        // We generate a best-effort action from the step summary
        sb.append("    // Action result: ").append(step.getSummary()).append("\n");
        sb.append("    // TODO: Fill in the exact selector and parameters from your application\n");

        if (step.hasError()) {
            sb.append("    // NOTE: This step had an error: ").append(step.getError()).append("\n");
        }
    }

    private void appendNavigateAction(StringBuilder sb, RecordedAction action) {
        String url = getStringParam(action.params, "url");
        if (url == null) {
            url = action.url;
        }
        sb.append("    await page.goto('").append(escapeString(url)).append("', {\n");
        sb.append("      waitUntil: 'domcontentloaded',\n");
        sb.append("      timeout: ").append(config.getNavigationTimeoutMs()).append(",\n");
        sb.append("    });\n");
        sb.append("    await page.waitForLoadState('networkidle', { timeout: ")
                .append(config.getNetworkIdleTimeoutMs()).append(" }).catch(() => {});\n");
        appendStabilityWait(sb);
    }

    private void appendClickAction(StringBuilder sb, RecordedAction action) {
        String selector = action.selector;
        if (selector == null) {
            int index = getIntParam(action.params, "index", -1);
            selector = "[data-ba-index='" + index + "']";
            sb.append("    // Original element index: ").append(index).append("\n");
        }

        sb.append("    await page.waitForSelector('").append(escapeString(selector)).append("', {\n");
        sb.append("      state: 'visible',\n");
        sb.append("      timeout: ").append(config.getDefaultTimeoutMs()).append(",\n");
        sb.append("    });\n");
        sb.append("    await page.locator('").append(escapeString(selector)).append("').first().click({\n");
        sb.append("      timeout: ").append(config.getDefaultTimeoutMs()).append(",\n");
        sb.append("    });\n");
        appendNavigationWaitIfNeeded(sb);
        appendStabilityWait(sb);
    }

    private void appendTypeAction(StringBuilder sb, RecordedAction action) {
        String selector = action.selector;
        String text = getStringParam(action.params, "text");
        if (selector == null) {
            int index = getIntParam(action.params, "index", -1);
            selector = "[data-ba-index='" + index + "']";
            sb.append("    // Original element index: ").append(index).append("\n");
        }

        sb.append("    await page.waitForSelector('").append(escapeString(selector)).append("', {\n");
        sb.append("      state: 'visible',\n");
        sb.append("      timeout: ").append(config.getDefaultTimeoutMs()).append(",\n");
        sb.append("    });\n");
        sb.append("    const inputEl = page.locator('").append(escapeString(selector)).append("').first();\n");
        sb.append("    await inputEl.click();\n");
        sb.append("    await inputEl.fill('").append(escapeString(text != null ? text : "")).append("');\n");
        appendStabilityWait(sb);
    }

    private void appendScrollAction(StringBuilder sb, RecordedAction action, boolean down) {
        int amount = getIntParam(action.params, "amount", 500);
        sb.append("    await page.evaluate(() => window.scrollBy(0, ").append(down ? amount : -amount).append("));\n");
        sb.append("    await waitForDomStable(page, ").append(config.getDomStableTimeoutMs()).append(");\n");
    }

    private void appendGoBackAction(StringBuilder sb) {
        sb.append("    await page.goBack({ waitUntil: 'domcontentloaded', timeout: ")
                .append(config.getNavigationTimeoutMs()).append(" });\n");
        sb.append("    await page.waitForLoadState('networkidle', { timeout: ")
                .append(config.getNetworkIdleTimeoutMs()).append(" }).catch(() => {});\n");
        appendStabilityWait(sb);
    }

    private void appendSendKeysAction(StringBuilder sb, RecordedAction action) {
        String keys = getStringParam(action.params, "keys");
        sb.append("    await page.keyboard.press('").append(escapeString(keys != null ? keys : "")).append("');\n");
        appendStabilityWait(sb);
    }

    private void appendSwitchTabAction(StringBuilder sb, RecordedAction action) {
        int tabIndex = getIntParam(action.params, "tab_index", 0);
        sb.append("    const pages = context.pages();\n");
        sb.append("    if (pages.length > ").append(tabIndex).append(") {\n");
        sb.append("      page = pages[").append(tabIndex).append("];\n");
        sb.append("      await page.bringToFront();\n");
        sb.append("      await page.waitForLoadState('domcontentloaded');\n");
        sb.append("    }\n");
    }

    private void appendOpenTabAction(StringBuilder sb, RecordedAction action) {
        String url = getStringParam(action.params, "url");
        sb.append("    const newPage = await context.newPage();\n");
        if (url != null && !url.isEmpty()) {
            sb.append("    await newPage.goto('").append(escapeString(url)).append("', {\n");
            sb.append("      waitUntil: 'domcontentloaded',\n");
            sb.append("      timeout: ").append(config.getNavigationTimeoutMs()).append(",\n");
            sb.append("    });\n");
        }
        sb.append("    page = newPage;\n");
    }

    private void appendCloseTabAction(StringBuilder sb) {
        sb.append("    await page.close();\n");
        sb.append("    const remainingPages = context.pages();\n");
        sb.append("    if (remainingPages.length > 0) {\n");
        sb.append("      page = remainingPages[remainingPages.length - 1];\n");
        sb.append("      await page.bringToFront();\n");
        sb.append("    }\n");
    }

    private void appendHoverAction(StringBuilder sb, RecordedAction action) {
        String selector = action.selector;
        if (selector == null) {
            int index = getIntParam(action.params, "index", -1);
            selector = "[data-ba-index='" + index + "']";
        }
        sb.append("    await page.waitForSelector('").append(escapeString(selector)).append("', { state: 'visible' });\n");
        sb.append("    await page.locator('").append(escapeString(selector)).append("').first().hover();\n");
        appendStabilityWait(sb);
    }

    private void appendDragDropAction(StringBuilder sb, RecordedAction action) {
        sb.append("    // Drag and drop action\n");
        sb.append("    // TODO: Update selectors for source and target elements\n");
        sb.append("    // await page.locator('SOURCE_SELECTOR').dragTo(page.locator('TARGET_SELECTOR'));\n");
        appendStabilityWait(sb);
    }

    private void appendMouseMoveAction(StringBuilder sb, RecordedAction action) {
        double x = getDoubleParam(action.params, "x", 0);
        double y = getDoubleParam(action.params, "y", 0);
        sb.append("    await page.mouse.move(").append(x).append(", ").append(y).append(");\n");
    }

    private void appendSelectAction(StringBuilder sb, RecordedAction action) {
        String selector = action.selector;
        String value = getStringParam(action.params, "value");
        if (selector == null) {
            int index = getIntParam(action.params, "index", -1);
            selector = "[data-ba-index='" + index + "']";
        }
        sb.append("    await page.waitForSelector('").append(escapeString(selector)).append("', { state: 'visible' });\n");
        sb.append("    await page.locator('").append(escapeString(selector))
                .append("').first().selectOption('").append(escapeString(value != null ? value : "")).append("');\n");
        appendStabilityWait(sb);
    }

    private void appendUploadAction(StringBuilder sb, RecordedAction action) {
        String selector = action.selector;
        String filePath = getStringParam(action.params, "file_path");
        if (selector == null) {
            int index = getIntParam(action.params, "index", -1);
            selector = "[data-ba-index='" + index + "']";
        }
        sb.append("    await page.waitForSelector('").append(escapeString(selector)).append("', { state: 'attached' });\n");
        sb.append("    await page.locator('").append(escapeString(selector))
                .append("').first().setInputFiles('").append(escapeString(filePath != null ? filePath : "path/to/file")).append("');\n");
        appendStabilityWait(sb);
    }

    private void appendExtractAction(StringBuilder sb) {
        sb.append("    const extractedContent = await page.locator('body').innerText();\n");
        sb.append("    console.log('Extracted content:', extractedContent.substring(0, 500));\n");
    }

    private void appendDoneAction(StringBuilder sb, RecordedAction action) {
        String text = getStringParam(action.params, "text");
        sb.append("    // Task completed\n");
        if (text != null) {
            sb.append("    console.log('Task result: ").append(escapeString(text)).append("');\n");
        }
    }

    private void appendGenericAction(StringBuilder sb, RecordedAction action) {
        sb.append("    // Unknown action: ").append(action.actionName).append("\n");
        sb.append("    // Parameters: ").append(action.params).append("\n");
        sb.append("    // TODO: Implement this action manually\n");
    }

    private void appendStabilityWait(StringBuilder sb) {
        sb.append("    await waitForDomStable(page, ").append(config.getDomStableTimeoutMs()).append(");\n");
    }

    private void appendNavigationWaitIfNeeded(StringBuilder sb) {
        sb.append("    // Wait for potential navigation triggered by click\n");
        sb.append("    await page.waitForLoadState('domcontentloaded', { timeout: ")
                .append(config.getNavigationTimeoutMs() / 2).append(" }).catch(() => {});\n");
    }

    private void appendHelperFunctions(StringBuilder sb) {
        sb.append("/**\n");
        sb.append(" * Wait for the DOM to stabilize (no mutations for the specified duration).\n");
        sb.append(" * This ensures dynamic content has finished loading/rendering.\n");
        sb.append(" */\n");
        sb.append("async function waitForDomStable(page: Page, timeoutMs: number = 2000): Promise<void> {\n");
        sb.append("  await page.evaluate((timeout) => {\n");
        sb.append("    return new Promise<void>((resolve) => {\n");
        sb.append("      let timer: ReturnType<typeof setTimeout>;\n");
        sb.append("      const stableDelay = Math.min(timeout, 500);\n");
        sb.append("      const observer = new MutationObserver(() => {\n");
        sb.append("        clearTimeout(timer);\n");
        sb.append("        timer = setTimeout(() => {\n");
        sb.append("          observer.disconnect();\n");
        sb.append("          resolve();\n");
        sb.append("        }, stableDelay);\n");
        sb.append("      });\n");
        sb.append("      observer.observe(document.body, {\n");
        sb.append("        childList: true,\n");
        sb.append("        subtree: true,\n");
        sb.append("        attributes: true,\n");
        sb.append("        characterData: true,\n");
        sb.append("      });\n");
        sb.append("      // Resolve after timeout even if DOM keeps changing\n");
        sb.append("      timer = setTimeout(() => {\n");
        sb.append("        observer.disconnect();\n");
        sb.append("        resolve();\n");
        sb.append("      }, timeout);\n");
        sb.append("    });\n");
        sb.append("  }, timeoutMs);\n");
        sb.append("}\n\n");

        sb.append("/**\n");
        sb.append(" * Retry a function with exponential backoff.\n");
        sb.append(" */\n");
        sb.append("async function retryWithBackoff<T>(\n");
        sb.append("  fn: () => Promise<T>,\n");
        sb.append("  maxRetries: number = 3,\n");
        sb.append("  baseDelay: number = 1000\n");
        sb.append("): Promise<T> {\n");
        sb.append("  for (let attempt = 0; attempt <= maxRetries; attempt++) {\n");
        sb.append("    try {\n");
        sb.append("      return await fn();\n");
        sb.append("    } catch (error) {\n");
        sb.append("      if (attempt === maxRetries) throw error;\n");
        sb.append("      const delay = baseDelay * Math.pow(2, attempt);\n");
        sb.append("      console.log(`Retry ${attempt + 1}/${maxRetries} after ${delay}ms`);\n");
        sb.append("      await new Promise(r => setTimeout(r, delay));\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("  throw new Error('Should not reach here');\n");
        sb.append("}\n\n");

        sb.append("/**\n");
        sb.append(" * Wait for network to be idle (no pending requests for the specified duration).\n");
        sb.append(" */\n");
        sb.append("async function waitForNetworkIdle(page: Page, timeoutMs: number = 5000): Promise<void> {\n");
        sb.append("  try {\n");
        sb.append("    await page.waitForLoadState('networkidle', { timeout: timeoutMs });\n");
        sb.append("  } catch {\n");
        sb.append("    // Network may not fully idle, continue anyway\n");
        sb.append("  }\n");
        sb.append("}\n");
    }

    // --- Utility methods ---

    private String getStringParam(Map<String, Object> params, String key) {
        if (params == null) return null;
        Object val = params.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultVal) {
        if (params == null) return defaultVal;
        Object val = params.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(String.valueOf(val)); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    private double getDoubleParam(Map<String, Object> params, String key, double defaultVal) {
        if (params == null) return defaultVal;
        Object val = params.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try { return Double.parseDouble(String.valueOf(val)); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    private String escapeString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * Represents a recorded browser action.
     */
    public static class RecordedAction {
        private final String actionName;
        private final Map<String, Object> params;
        private final String selector;
        private final String url;
        private final long timestamp;

        public RecordedAction(String actionName, Map<String, Object> params,
                              String selector, String url, long timestamp) {
            this.actionName = actionName;
            this.params = params;
            this.selector = selector;
            this.url = url;
            this.timestamp = timestamp;
        }

        public String getActionName() { return actionName; }
        public Map<String, Object> getParams() { return params; }
        public String getSelector() { return selector; }
        public String getUrl() { return url; }
        public long getTimestamp() { return timestamp; }
    }
}
