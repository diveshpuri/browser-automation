package com.browserautomation.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the execution state of the agent across steps.
 */
public class AgentState {

    private static final Logger logger = LoggerFactory.getLogger(AgentState.class);

    private int currentStep;
    private int consecutiveFailures;
    private int totalTokensUsed;
    private boolean isCompleted;
    private boolean isFailed;
    private String finalResult;
    private final List<AgentStep> history;

    public AgentState() {
        this.currentStep = 0;
        this.consecutiveFailures = 0;
        this.totalTokensUsed = 0;
        this.isCompleted = false;
        this.isFailed = false;
        this.history = new ArrayList<>();
    }

    public void recordStep(AgentStep step) {
        history.add(step);
        currentStep++;
        totalTokensUsed += step.getTokensUsed();

        if (step.hasError()) {
            consecutiveFailures++;
            logger.info("[STATE] Step {} recorded: action={}, ERROR='{}', consecutiveFailures={}, tokens={}, duration={}ms",
                    step.getStepNumber(), step.getActionName(), step.getError(),
                    consecutiveFailures, step.getTokensUsed(), step.getDurationMs());
        } else {
            consecutiveFailures = 0;
            logger.info("[STATE] Step {} recorded: action={}, result='{}', consecutiveFailures=0, tokens={}, duration={}ms",
                    step.getStepNumber(), step.getActionName(),
                    step.getActionResult() != null ? truncateStr(step.getActionResult(), 100) : "OK",
                    step.getTokensUsed(), step.getDurationMs());
        }
    }

    public void markCompleted(String result) {
        this.isCompleted = true;
        this.finalResult = result;
        logger.info("[STATE] Agent COMPLETED after {} steps, {} total tokens — result: {}",
                currentStep, totalTokensUsed, result);
    }

    public void markFailed(String reason) {
        this.isFailed = true;
        this.finalResult = reason;
        logger.warn("[STATE] Agent FAILED after {} steps, {} total tokens — reason: {}",
                currentStep, totalTokensUsed, reason);
    }

    private static String truncateStr(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    // Getters

    public int getCurrentStep() {
        return currentStep;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public int getTotalTokensUsed() {
        return totalTokensUsed;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public List<AgentStep> getHistory() {
        return history;
    }

    public AgentStep getLastStep() {
        if (history.isEmpty()) return null;
        return history.get(history.size() - 1);
    }

    /**
     * Build a summary of recent history for the LLM.
     */
    public String getHistorySummary(int maxSteps) {
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - maxSteps);
        for (int i = start; i < history.size(); i++) {
            AgentStep step = history.get(i);
            sb.append("Step ").append(i + 1).append(": ");
            sb.append(step.getSummary()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Represents a single step in the agent's execution.
     */
    public static class AgentStep {
        private final int stepNumber;
        private final String llmThinking;
        private final String actionName;
        private final String actionResult;
        private final String error;
        private final int tokensUsed;
        private final long durationMs;

        public AgentStep(int stepNumber, String llmThinking, String actionName,
                         String actionResult, String error, int tokensUsed, long durationMs) {
            this.stepNumber = stepNumber;
            this.llmThinking = llmThinking;
            this.actionName = actionName;
            this.actionResult = actionResult;
            this.error = error;
            this.tokensUsed = tokensUsed;
            this.durationMs = durationMs;
        }

        public boolean hasError() {
            return error != null && !error.isEmpty();
        }

        public String getSummary() {
            if (hasError()) {
                return actionName + " -> ERROR: " + error;
            }
            return actionName + " -> " + (actionResult != null ? truncate(actionResult, 100) : "OK");
        }

        private String truncate(String text, int max) {
            if (text.length() <= max) return text;
            return text.substring(0, max) + "...";
        }

        // Getters

        public int getStepNumber() {
            return stepNumber;
        }

        public String getLlmThinking() {
            return llmThinking;
        }

        public String getActionName() {
            return actionName;
        }

        public String getActionResult() {
            return actionResult;
        }

        public String getError() {
            return error;
        }

        public int getTokensUsed() {
            return tokensUsed;
        }

        public long getDurationMs() {
            return durationMs;
        }
    }
}
