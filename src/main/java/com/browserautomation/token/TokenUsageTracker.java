package com.browserautomation.token;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks token usage across agent steps for cost and usage reporting.
 */
public class TokenUsageTracker {

    private final String model;
    private final List<StepUsage> stepUsages;
    private int totalInputTokens;
    private int totalOutputTokens;

    public TokenUsageTracker(String model) {
        this.model = model;
        this.stepUsages = new ArrayList<>();
    }

    /**
     * Record token usage for a step.
     */
    public void recordStep(int stepNumber, int inputTokens, int outputTokens) {
        stepUsages.add(new StepUsage(stepNumber, inputTokens, outputTokens));
        totalInputTokens += inputTokens;
        totalOutputTokens += outputTokens;
    }

    /**
     * Get the total number of input tokens used.
     */
    public int getTotalInputTokens() {
        return totalInputTokens;
    }

    /**
     * Get the total number of output tokens used.
     */
    public int getTotalOutputTokens() {
        return totalOutputTokens;
    }

    /**
     * Get the total number of tokens used (input + output).
     */
    public int getTotalTokens() {
        return totalInputTokens + totalOutputTokens;
    }

    /**
     * Get the estimated total cost in USD.
     */
    public double getTotalCost() {
        return TokenPricing.calculateCost(model, totalInputTokens, totalOutputTokens);
    }

    /**
     * Get usage for all steps.
     */
    public List<StepUsage> getStepUsages() {
        return List.copyOf(stepUsages);
    }

    /**
     * Get the number of steps tracked.
     */
    public int getStepCount() {
        return stepUsages.size();
    }

    /**
     * Get a summary report of token usage.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Token Usage Summary\n");
        sb.append("Model: ").append(model).append("\n");
        sb.append("Steps: ").append(stepUsages.size()).append("\n");
        sb.append("Input tokens: ").append(totalInputTokens).append("\n");
        sb.append("Output tokens: ").append(totalOutputTokens).append("\n");
        sb.append("Total tokens: ").append(getTotalTokens()).append("\n");

        double cost = getTotalCost();
        sb.append("Estimated cost: ").append(TokenPricing.formatCost(cost)).append("\n");

        if (!stepUsages.isEmpty()) {
            sb.append("\nPer-step breakdown:\n");
            for (StepUsage usage : stepUsages) {
                sb.append(String.format("  Step %d: %d in / %d out = %d total\n",
                        usage.getStepNumber(), usage.getInputTokens(),
                        usage.getOutputTokens(), usage.getTotalTokens()));
            }
        }
        return sb.toString();
    }

    /**
     * Token usage for a single step.
     */
    public static class StepUsage {
        private final int stepNumber;
        private final int inputTokens;
        private final int outputTokens;

        public StepUsage(int stepNumber, int inputTokens, int outputTokens) {
            this.stepNumber = stepNumber;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }

        public int getStepNumber() { return stepNumber; }
        public int getInputTokens() { return inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public int getTotalTokens() { return inputTokens + outputTokens; }
    }
}
