package com.browserautomation.agent;

import java.util.List;

/**
 * Final result of an agent run.
 */
public class AgentResult {

    private final boolean success;
    private final String result;
    private final int totalSteps;
    private final int totalTokensUsed;
    private final long totalDurationMs;
    private final List<AgentState.AgentStep> history;

    public AgentResult(boolean success, String result, int totalSteps,
                       int totalTokensUsed, long totalDurationMs, List<AgentState.AgentStep> history) {
        this.success = success;
        this.result = result;
        this.totalSteps = totalSteps;
        this.totalTokensUsed = totalTokensUsed;
        this.totalDurationMs = totalDurationMs;
        this.history = history;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResult() {
        return result;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public int getTotalTokensUsed() {
        return totalTokensUsed;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public List<AgentState.AgentStep> getHistory() {
        return history;
    }

    @Override
    public String toString() {
        return String.format("AgentResult[success=%s, steps=%d, tokens=%d, duration=%dms, result=%s]",
                success, totalSteps, totalTokensUsed, totalDurationMs,
                result != null ? result.substring(0, Math.min(result.length(), 100)) : "null");
    }
}
