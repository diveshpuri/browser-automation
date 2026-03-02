package com.browserautomation.agent;

import com.browserautomation.action.*;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;
import com.browserautomation.browser.BrowserState;
import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * The main agent that orchestrates browser automation using an LLM.
 * Takes a task description, uses the LLM to decide actions, and executes them
 * against the browser via Playwright.
 *
 *
 * Usage:
 * <pre>
 * Agent agent = new Agent.Builder()
 *     .task("Search for flights from NYC to London")
 *     .llmProvider(new OpenAiProvider(apiKey, "gpt-4o"))
 *     .build();
 * AgentResult result = agent.run();
 * </pre>
 */
public class Agent implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Agent.class);

    private final String task;
    private final LlmProvider llmProvider;
    private final AgentConfig config;
    private final BrowserProfile browserProfile;
    private final ActionRegistry actionRegistry;
    private final AgentState state;
    private BrowserSession browserSession;
    private boolean ownsBrowserSession;

    private Agent(Builder builder) {
        this.task = builder.task;
        this.llmProvider = builder.llmProvider;
        this.config = builder.config != null ? builder.config : new AgentConfig();
        this.browserProfile = builder.browserProfile != null ? builder.browserProfile : new BrowserProfile();
        this.actionRegistry = builder.actionRegistry != null ? builder.actionRegistry : new ActionRegistry();
        this.browserSession = builder.browserSession;
        this.ownsBrowserSession = builder.browserSession == null;
        this.state = new AgentState();
    }

    /**
     * Run the agent to completion.
     *
     * @return the final result of the agent's execution
     */
    public AgentResult run() {
        long startTime = System.currentTimeMillis();
        logger.info("========================================");
        logger.info("AGENT STARTING");
        logger.info("  Task: {}", task);
        logger.info("  LLM Provider: {} (model={})", llmProvider.getProviderName(), llmProvider.getModelName());
        logger.info("  Config: maxSteps={}, maxFailures={}, maxActionsPerStep={}, useVision={}, useThinking={}",
                config.getMaxSteps(), config.getMaxFailures(), config.getMaxActionsPerStep(),
                config.isUseVision(), config.isUseThinking());
        logger.info("  Vision support: {} (provider={}, config={})",
                config.isUseVision() && llmProvider.supportsVision(),
                llmProvider.supportsVision(), config.isUseVision());
        logger.info("========================================");

        try {
            // Start browser if we own it
            if (ownsBrowserSession) {
                logger.info("[BROWSER] Creating and starting owned browser session (headless={})",
                        browserProfile.isHeadless());
                browserSession = new BrowserSession(browserProfile);
                browserSession.start();
                logger.info("[BROWSER] Browser session started successfully");
            } else {
                logger.info("[BROWSER] Using externally provided browser session (started={})",
                        browserSession.isStarted());
            }

            // Build system prompt
            SystemPrompt systemPrompt = new SystemPrompt(config, actionRegistry);
            String systemMessage = systemPrompt.build();
            logger.info("[INIT] System prompt built ({} chars), {} actions registered",
                    systemMessage.length(), actionRegistry.getAllActions().size());

            // Initialize conversation
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.system(systemMessage));
            messages.add(ChatMessage.user("Task: " + task));
            logger.info("[INIT] Conversation initialized with {} messages", messages.size());

            // Main agent loop
            logger.info("[LOOP] Entering main agent loop (maxSteps={})", config.getMaxSteps());
            while (!state.isCompleted() && !state.isFailed()) {
                // Check step limit
                if (state.getCurrentStep() >= config.getMaxSteps()) {
                    logger.warn("[LOOP] STOPPING: Reached maximum steps ({}/{})",
                            state.getCurrentStep(), config.getMaxSteps());
                    state.markFailed("Reached maximum steps without completing the task");
                    break;
                }

                // Check failure limit
                if (state.getConsecutiveFailures() >= config.getMaxFailures()) {
                    logger.warn("[LOOP] STOPPING: Reached maximum consecutive failures ({}/{})",
                            state.getConsecutiveFailures(), config.getMaxFailures());
                    state.markFailed("Too many consecutive failures");
                    break;
                }

                logger.info("[LOOP] State: step={}/{}, consecutiveFailures={}/{}, totalTokens={}",
                        state.getCurrentStep(), config.getMaxSteps(),
                        state.getConsecutiveFailures(), config.getMaxFailures(),
                        state.getTotalTokensUsed());

                try {
                    executeStep(messages);
                } catch (Exception e) {
                    logger.error("[LOOP] Step {} EXCEPTION: {} - {}",
                            state.getCurrentStep() + 1, e.getClass().getSimpleName(), e.getMessage());
                    state.recordStep(new AgentState.AgentStep(
                            state.getCurrentStep() + 1, null, "error",
                            null, e.getMessage(), 0, 0));
                }
            }

        } catch (Exception e) {
            logger.error("[FATAL] Agent execution failed with {}: {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            state.markFailed("Agent execution failed: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("========================================");
        logger.info("AGENT FINISHED");
        logger.info("  Success: {}", state.isCompleted());
        logger.info("  Result: {}", state.getFinalResult());
        logger.info("  Total Steps: {}", state.getCurrentStep());
        logger.info("  Total Tokens: {}", state.getTotalTokensUsed());
        logger.info("  Duration: {}ms", duration);
        logger.info("  History: {} steps recorded", state.getHistory().size());
        logger.info("========================================");

        return new AgentResult(
                state.isCompleted(),
                state.getFinalResult(),
                state.getCurrentStep(),
                state.getTotalTokensUsed(),
                duration,
                state.getHistory()
        );
    }

    /**
     * Execute a single step: get browser state, ask LLM, execute actions.
     */
    private void executeStep(List<ChatMessage> messages) {
        long stepStart = System.currentTimeMillis();
        int stepNum = state.getCurrentStep() + 1;
        logger.info("╔══════════════════════════════════════");
        logger.info("║ STEP {}/{}", stepNum, config.getMaxSteps());
        logger.info("╚══════════════════════════════════════");

        // 1. Get current browser state
        boolean includeScreenshot = config.isUseVision() && llmProvider.supportsVision();
        logger.info("[STEP {}] Capturing browser state (includeScreenshot={})", stepNum, includeScreenshot);
        long browserStateStart = System.currentTimeMillis();
        BrowserState browserState = browserSession.getState(includeScreenshot);
        long browserStateDuration = System.currentTimeMillis() - browserStateStart;
        logger.info("[STEP {}] Browser state captured in {}ms — URL: {}, Title: '{}'",
                stepNum, browserStateDuration, browserState.getUrl(), truncate(browserState.getTitle(), 80));
        logger.info("[STEP {}] DOM: {} interactive elements, Tabs: {}, Active tab: {}",
                stepNum,
                browserState.getDomState() != null ? browserState.getDomState().getElements().size() : 0,
                browserState.getTabs().size(), browserState.getActiveTabIndex());
        if (browserState.getPageInfo() != null) {
            BrowserState.PageInfo pi = browserState.getPageInfo();
            logger.info("[STEP {}] Page: scrollY={}, pageHeight={}, viewportHeight={}",
                    stepNum, pi.getScrollY(), pi.getPageHeight(), pi.getViewportHeight());
        }
        if (includeScreenshot && browserState.getScreenshot() != null) {
            logger.info("[STEP {}] Screenshot captured ({} bytes)",
                    stepNum, browserState.getScreenshot().length);
        }

        // 2. Build state message for the LLM
        String stateDescription = buildStateDescription(browserState);
        logger.info("[STEP {}] State description built ({} chars)", stepNum, stateDescription.length());

        if (config.isUseVision() && browserState.getScreenshot() != null && llmProvider.supportsVision()) {
            String base64Screenshot = Base64.getEncoder().encodeToString(browserState.getScreenshot());
            messages.add(ChatMessage.userWithImage(stateDescription, base64Screenshot));
            logger.info("[STEP {}] Added user message with screenshot to conversation", stepNum);
        } else {
            messages.add(ChatMessage.user(stateDescription));
            logger.info("[STEP {}] Added user message (text only) to conversation", stepNum);
        }

        // 3. Call LLM
        logger.info("[STEP {}] Calling LLM ({}/{}) with {} messages and {} tool definitions",
                stepNum, llmProvider.getProviderName(), llmProvider.getModelName(),
                messages.size(), actionRegistry.getToolDefinitions().size());
        long llmStart = System.currentTimeMillis();
        List<Map<String, Object>> toolDefs = actionRegistry.getToolDefinitions();
        LlmResponse llmResponse = llmProvider.chatCompletion(messages, toolDefs);
        long llmDuration = System.currentTimeMillis() - llmStart;
        logger.info("[STEP {}] LLM responded in {}ms — promptTokens={}, completionTokens={}, totalTokens={}",
                stepNum, llmDuration, llmResponse.getPromptTokens(),
                llmResponse.getCompletionTokens(), llmResponse.getTotalTokens());
        logger.info("[STEP {}] LLM response: hasContent={}, hasToolCalls={}, toolCallCount={}",
                stepNum,
                llmResponse.getContent() != null && !llmResponse.getContent().isEmpty(),
                llmResponse.hasToolCalls(),
                llmResponse.getToolCalls() != null ? llmResponse.getToolCalls().size() : 0);

        // 4. Process LLM response
        String thinking = llmResponse.getContent();
        if (thinking != null && !thinking.isEmpty()) {
            logger.info("[STEP {}] LLM thinking: {}", stepNum, truncate(thinking, 300));
            messages.add(ChatMessage.assistant(thinking));
        }

        // 5. Execute tool calls
        if (llmResponse.hasToolCalls()) {
            logger.info("[STEP {}] Executing {} tool call(s) (max {} per step)",
                    stepNum, llmResponse.getToolCalls().size(), config.getMaxActionsPerStep());
            int actionsExecuted = 0;
            for (LlmResponse.ToolCall toolCall : llmResponse.getToolCalls()) {
                if (actionsExecuted >= config.getMaxActionsPerStep()) {
                    logger.warn("[STEP {}] Reached max actions per step ({}), skipping remaining {} tool calls",
                            stepNum, config.getMaxActionsPerStep(),
                            llmResponse.getToolCalls().size() - actionsExecuted);
                    break;
                }

                String actionName = toolCall.getFunctionName();
                Map<String, Object> args = toolCall.getArguments();

                logger.info("[STEP {}] ACTION {}/{}: {}({})",
                        stepNum, actionsExecuted + 1, llmResponse.getToolCalls().size(),
                        actionName, args);

                BrowserAction action = actionRegistry.getAction(actionName);
                if (action == null) {
                    String error = "Unknown action: " + actionName;
                    logger.warn("[STEP {}] ACTION FAILED: {} — action not found in registry", stepNum, actionName);
                    messages.add(ChatMessage.tool(error, toolCall.getId()));
                    state.recordStep(new AgentState.AgentStep(
                            stepNum, thinking, actionName, null, error,
                            llmResponse.getTotalTokens(), System.currentTimeMillis() - stepStart));
                    continue;
                }

                long actionStart = System.currentTimeMillis();
                ActionResult result = action.execute(browserSession, new ActionParameters(args));
                long actionDuration = System.currentTimeMillis() - actionStart;
                actionsExecuted++;

                if (result.isSuccess()) {
                    logger.info("[STEP {}] ACTION SUCCEEDED: {} in {}ms — result: {}",
                            stepNum, actionName, actionDuration,
                            truncate(result.getExtractedContent() != null ? result.getExtractedContent() : "OK", 200));
                } else {
                    logger.warn("[STEP {}] ACTION FAILED: {} in {}ms — error: {}",
                            stepNum, actionName, actionDuration, result.getError());
                }

                // Record step
                state.recordStep(new AgentState.AgentStep(
                        stepNum, thinking, actionName,
                        result.isSuccess() ? result.getExtractedContent() : null,
                        result.isSuccess() ? null : result.getError(),
                        llmResponse.getTotalTokens(),
                        System.currentTimeMillis() - stepStart));

                // Add tool result to messages
                String toolResult = result.isSuccess()
                        ? (result.getExtractedContent() != null ? result.getExtractedContent() : "Action completed successfully")
                        : "Error: " + result.getError();
                messages.add(ChatMessage.tool(toolResult, toolCall.getId()));

                // Check if done
                if (result.isDone()) {
                    logger.info("[STEP {}] TASK COMPLETED — result: {}", stepNum, result.getExtractedContent());
                    state.markCompleted(result.getExtractedContent());
                    long stepDuration = System.currentTimeMillis() - stepStart;
                    logger.info("[STEP {}] Step completed in {}ms (browserState={}ms, llm={}ms, action={}ms)",
                            stepNum, stepDuration, browserStateDuration, llmDuration, actionDuration);
                    return;
                }
            }
        } else {
            // No tool calls - LLM just responded with text
            logger.warn("[STEP {}] LLM responded WITHOUT tool calls — treating as no-op", stepNum);
            state.recordStep(new AgentState.AgentStep(
                    stepNum, thinking, "none", thinking, null,
                    llmResponse.getTotalTokens(), System.currentTimeMillis() - stepStart));
        }

        long stepDuration = System.currentTimeMillis() - stepStart;
        logger.info("[STEP {}] Step completed in {}ms (browserState={}ms, llm={}ms)",
                stepNum, stepDuration, browserStateDuration, llmDuration);
    }

    /**
     * Build a text description of the current browser state for the LLM.
     */
    private String buildStateDescription(BrowserState browserState) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Current Browser State\n\n");
        sb.append("**URL**: ").append(browserState.getUrl()).append("\n");
        sb.append("**Title**: ").append(browserState.getTitle()).append("\n");

        // Tab info
        if (browserState.getTabs().size() > 1) {
            sb.append("**Tabs**: ");
            for (BrowserState.TabInfo tab : browserState.getTabs()) {
                sb.append("[").append(tab.getIndex()).append("] ");
                if (tab.getIndex() == browserState.getActiveTabIndex()) {
                    sb.append("(active) ");
                }
                sb.append(tab.getTitle()).append(" | ");
            }
            sb.append("\n");
        }

        // Page scroll info
        if (browserState.getPageInfo() != null) {
            BrowserState.PageInfo pi = browserState.getPageInfo();
            if (pi.getPagesAbove() > 0 || pi.getPagesBelow() > 0) {
                sb.append("**Scroll**: ");
                sb.append(String.format("%.1f pages above, %.1f pages below\n",
                        pi.getPagesAbove(), pi.getPagesBelow()));
            }
        }

        // DOM elements
        sb.append("\n### Interactive Elements\n\n");
        if (browserState.getDomState() != null) {
            String domRepr = browserState.getDomState().toLlmRepresentation();
            if (domRepr.length() > 40000) {
                domRepr = domRepr.substring(0, 40000) + "\n... (truncated)";
            }
            sb.append(domRepr);
        } else {
            sb.append("(No interactive elements found)\n");
        }

        // History summary
        if (state.getCurrentStep() > 0) {
            sb.append("\n### Recent Action History\n\n");
            sb.append(state.getHistorySummary(5));
        }

        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Get the current agent state.
     */
    public AgentState getState() {
        return state;
    }

    /**
     * Get the browser session.
     */
    public BrowserSession getBrowserSession() {
        return browserSession;
    }

    @Override
    public void close() {
        if (ownsBrowserSession && browserSession != null) {
            browserSession.close();
        }
    }

    /**
     * Builder for creating Agent instances.
     */
    public static class Builder {
        private String task;
        private LlmProvider llmProvider;
        private AgentConfig config;
        private BrowserProfile browserProfile;
        private ActionRegistry actionRegistry;
        private BrowserSession browserSession;

        public Builder task(String task) {
            this.task = task;
            return this;
        }

        public Builder llmProvider(LlmProvider llmProvider) {
            this.llmProvider = llmProvider;
            return this;
        }

        public Builder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        public Builder browserProfile(BrowserProfile browserProfile) {
            this.browserProfile = browserProfile;
            return this;
        }

        public Builder actionRegistry(ActionRegistry actionRegistry) {
            this.actionRegistry = actionRegistry;
            return this;
        }

        public Builder browserSession(BrowserSession browserSession) {
            this.browserSession = browserSession;
            return this;
        }

        public Agent build() {
            if (task == null || task.isEmpty()) {
                throw new IllegalArgumentException("Task is required");
            }
            if (llmProvider == null) {
                throw new IllegalArgumentException("LLM provider is required");
            }
            return new Agent(this);
        }
    }
}
