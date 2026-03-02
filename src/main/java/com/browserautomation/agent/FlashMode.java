package com.browserautomation.agent;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flash mode for stripped-down agent execution.
 * Disables evaluation, thinking, and other overhead for maximum speed.
 *
 * In flash mode:
 * - No evaluation/self-assessment steps
 * - No thinking output (direct action only)
 * - Simplified system prompt
 * - No vision processing (text-only DOM)
 * - Reduced token usage
 */
public class FlashMode {

    private static final Logger logger = LoggerFactory.getLogger(FlashMode.class);

    private static final String FLASH_SYSTEM_PROMPT =
            "You are a fast browser automation agent. Respond ONLY with tool calls. " +
            "Do not explain your reasoning. Execute actions directly and efficiently. " +
            "Focus on completing the task with minimum steps.";

    private final LlmProvider llmProvider;
    private final boolean disableVision;
    private final boolean disableThinking;
    private final boolean disableEvaluation;
    private final int maxActionsPerStep;

    private FlashMode(Builder builder) {
        this.llmProvider = builder.llmProvider;
        this.disableVision = builder.disableVision;
        this.disableThinking = builder.disableThinking;
        this.disableEvaluation = builder.disableEvaluation;
        this.maxActionsPerStep = builder.maxActionsPerStep;
    }

    /**
     * Create a flash-mode AgentConfig from an existing config.
     *
     * @param baseConfig the base configuration
     * @return a new AgentConfig optimized for flash mode
     */
    public AgentConfig toFlashConfig(AgentConfig baseConfig) {
        AgentConfig flashConfig = new AgentConfig()
                .maxSteps(baseConfig.getMaxSteps())
                .maxFailures(baseConfig.getMaxFailures())
                .maxActionsPerStep(maxActionsPerStep)
                .useVision(!disableVision)
                .useThinking(!disableThinking)
                .llmTimeoutSeconds(baseConfig.getLlmTimeoutSeconds())
                .stepTimeoutSeconds(baseConfig.getStepTimeoutSeconds());

        if (disableThinking) {
            flashConfig.overrideSystemMessage(FLASH_SYSTEM_PROMPT);
        }

        return flashConfig;
    }

    /**
     * Optimize messages for flash mode by stripping unnecessary content.
     *
     * @param messages the original messages
     * @return optimized messages
     */
    public List<ChatMessage> optimizeMessages(List<ChatMessage> messages) {
        List<ChatMessage> optimized = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (disableThinking && msg.getRole() == ChatMessage.Role.ASSISTANT) {
                // Skip assistant thinking messages in flash mode
                String text = msg.getText();
                if (text != null && !text.isEmpty() && !text.startsWith("{")) {
                    continue;
                }
            }
            if (disableEvaluation && msg.getRole() == ChatMessage.Role.USER) {
                String text = msg.getText();
                if (text != null && text.contains("Evaluate")) {
                    continue;
                }
            }
            optimized.add(msg);
        }
        return optimized;
    }

    /**
     * Strip thinking content from an LLM response in flash mode.
     *
     * @param response the original response
     * @return the response with thinking stripped
     */
    public LlmResponse stripThinking(LlmResponse response) {
        if (!disableThinking) {
            return response;
        }
        // In flash mode, we only care about tool calls
        if (response.hasToolCalls()) {
            return new LlmResponse(null, response.getToolCalls(),
                    response.getPromptTokens(), response.getCompletionTokens());
        }
        return response;
    }

    /**
     * Get the flash mode system prompt.
     */
    public String getSystemPrompt() {
        return FLASH_SYSTEM_PROMPT;
    }

    /**
     * Check if vision is disabled.
     */
    public boolean isVisionDisabled() {
        return disableVision;
    }

    /**
     * Check if thinking is disabled.
     */
    public boolean isThinkingDisabled() {
        return disableThinking;
    }

    /**
     * Check if evaluation is disabled.
     */
    public boolean isEvaluationDisabled() {
        return disableEvaluation;
    }

    /**
     * Get max actions per step in flash mode.
     */
    public int getMaxActionsPerStep() {
        return maxActionsPerStep;
    }

    /**
     * Builder for FlashMode.
     */
    public static class Builder {
        private LlmProvider llmProvider;
        private boolean disableVision = true;
        private boolean disableThinking = true;
        private boolean disableEvaluation = true;
        private int maxActionsPerStep = 10;

        public Builder llmProvider(LlmProvider llmProvider) {
            this.llmProvider = llmProvider;
            return this;
        }

        public Builder disableVision(boolean disable) {
            this.disableVision = disable;
            return this;
        }

        public Builder disableThinking(boolean disable) {
            this.disableThinking = disable;
            return this;
        }

        public Builder disableEvaluation(boolean disable) {
            this.disableEvaluation = disable;
            return this;
        }

        public Builder maxActionsPerStep(int max) {
            this.maxActionsPerStep = max;
            return this;
        }

        public FlashMode build() {
            return new FlashMode(this);
        }
    }
}
