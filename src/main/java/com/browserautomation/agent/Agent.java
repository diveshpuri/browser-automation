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
 * Equivalent to browser-use's Agent class.
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
        logger.info("Starting agent with task: {}", task);

        try {
            // Start browser if we own it
            if (ownsBrowserSession) {
                browserSession = new BrowserSession(browserProfile);
                browserSession.start();
            }

            // Build system prompt
            SystemPrompt systemPrompt = new SystemPrompt(config, actionRegistry);
            String systemMessage = systemPrompt.build();

            // Initialize conversation
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.system(systemMessage));
            messages.add(ChatMessage.user("Task: " + task));

            // Main agent loop
            while (!state.isCompleted() && !state.isFailed()) {
                // Check step limit
                if (state.getCurrentStep() >= config.getMaxSteps()) {
                    logger.warn("Reached maximum steps ({}), stopping", config.getMaxSteps());
                    state.markFailed("Reached maximum steps without completing the task");
                    break;
                }

                // Check failure limit
                if (state.getConsecutiveFailures() >= config.getMaxFailures()) {
                    logger.warn("Reached maximum consecutive failures ({}), stopping", config.getMaxFailures());
                    state.markFailed("Too many consecutive failures");
                    break;
                }

                try {
                    executeStep(messages);
                } catch (Exception e) {
                    logger.error("Step {} failed with exception: {}", state.getCurrentStep() + 1, e.getMessage());
                    state.recordStep(new AgentState.AgentStep(
                            state.getCurrentStep() + 1, null, "error",
                            null, e.getMessage(), 0, 0));
                }
            }

        } catch (Exception e) {
            logger.error("Agent execution failed: {}", e.getMessage(), e);
            state.markFailed("Agent execution failed: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Agent finished in {}ms after {} steps (success={})",
                duration, state.getCurrentStep(), state.isCompleted());

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
        logger.info("--- Step {} ---", stepNum);

        // 1. Get current browser state
        BrowserState browserState = browserSession.getState(config.isUseVision() && llmProvider.supportsVision());

        // 2. Build state message for the LLM
        String stateDescription = buildStateDescription(browserState);

        if (config.isUseVision() && browserState.getScreenshot() != null && llmProvider.supportsVision()) {
            String base64Screenshot = Base64.getEncoder().encodeToString(browserState.getScreenshot());
            messages.add(ChatMessage.userWithImage(stateDescription, base64Screenshot));
        } else {
            messages.add(ChatMessage.user(stateDescription));
        }

        // 3. Call LLM
        List<Map<String, Object>> toolDefs = actionRegistry.getToolDefinitions();
        LlmResponse llmResponse = llmProvider.chatCompletion(messages, toolDefs);

        // 4. Process LLM response
        String thinking = llmResponse.getContent();
        if (thinking != null && !thinking.isEmpty()) {
            logger.info("LLM thinking: {}", truncate(thinking, 200));
            messages.add(ChatMessage.assistant(thinking));
        }

        // 5. Execute tool calls
        if (llmResponse.hasToolCalls()) {
            int actionsExecuted = 0;
            for (LlmResponse.ToolCall toolCall : llmResponse.getToolCalls()) {
                if (actionsExecuted >= config.getMaxActionsPerStep()) {
                    logger.warn("Reached max actions per step ({}), skipping remaining", config.getMaxActionsPerStep());
                    break;
                }

                String actionName = toolCall.getFunctionName();
                Map<String, Object> args = toolCall.getArguments();

                logger.info("Executing action: {}({})", actionName, args);

                BrowserAction action = actionRegistry.getAction(actionName);
                if (action == null) {
                    String error = "Unknown action: " + actionName;
                    logger.warn(error);
                    messages.add(ChatMessage.tool(error, toolCall.getId()));
                    state.recordStep(new AgentState.AgentStep(
                            stepNum, thinking, actionName, null, error,
                            llmResponse.getTotalTokens(), System.currentTimeMillis() - stepStart));
                    continue;
                }

                ActionResult result = action.execute(browserSession, new ActionParameters(args));
                actionsExecuted++;

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
                    logger.info("Task completed: {}", result.getExtractedContent());
                    state.markCompleted(result.getExtractedContent());
                    return;
                }

                if (!result.isSuccess()) {
                    logger.warn("Action failed: {}", result.getError());
                }
            }
        } else {
            // No tool calls - LLM just responded with text
            // This shouldn't normally happen with tool calling, but handle it gracefully
            logger.debug("LLM responded without tool calls");
            state.recordStep(new AgentState.AgentStep(
                    stepNum, thinking, "none", thinking, null,
                    llmResponse.getTotalTokens(), System.currentTimeMillis() - stepStart));
        }
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
