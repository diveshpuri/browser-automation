package com.browserautomation.agent.planning;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multi-step planning system for the agent.
 * Creates plans, detects stalls, and triggers replanning with exploration nudges.
 */
public class PlanningService {

    private static final Logger logger = LoggerFactory.getLogger(PlanningService.class);

    private final LlmProvider llmProvider;
    private final int maxConsecutiveFailuresBeforeReplan;
    private final int maxReplans;
    private final List<PlanStep> currentPlan;
    private int currentStepIndex;
    private int consecutiveFailures;
    private int replanCount;
    private final List<String> explorationNudges;

    public PlanningService(LlmProvider llmProvider) {
        this(llmProvider, 3, 5);
    }

    public PlanningService(LlmProvider llmProvider, int maxConsecutiveFailuresBeforeReplan, int maxReplans) {
        this.llmProvider = llmProvider;
        this.maxConsecutiveFailuresBeforeReplan = maxConsecutiveFailuresBeforeReplan;
        this.maxReplans = maxReplans;
        this.currentPlan = new ArrayList<>();
        this.currentStepIndex = 0;
        this.consecutiveFailures = 0;
        this.replanCount = 0;
        this.explorationNudges = List.of(
                "Try a different approach to achieve the same goal.",
                "Look for alternative UI elements or navigation paths.",
                "Consider scrolling to find hidden elements.",
                "Try using keyboard shortcuts or different input methods.",
                "Check if there are popup dialogs or overlays blocking interaction.",
                "Try refreshing the page and starting this step over.",
                "Look for the element in a different section of the page."
        );
    }

    /**
     * Generate an initial plan for the given task.
     *
     * @param task the task description
     * @return the generated plan steps
     */
    public List<PlanStep> createPlan(String task) {
        logger.info("Creating plan for task: {}", task);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(
                "You are a planning assistant. Break down the given task into clear, actionable steps. " +
                "Each step should be a single browser action or verification. " +
                "Return steps as numbered list, one step per line. " +
                "Format: 1. [action description]"));
        messages.add(ChatMessage.user("Task: " + task));

        try {
            LlmResponse response = llmProvider.chatCompletion(messages);
            String content = response.getContent();
            if (content != null && !content.isEmpty()) {
                currentPlan.clear();
                currentPlan.addAll(parsePlanSteps(content));
                currentStepIndex = 0;
                consecutiveFailures = 0;
                logger.info("Created plan with {} steps", currentPlan.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to create plan: {}", e.getMessage());
        }

        return Collections.unmodifiableList(currentPlan);
    }

    /**
     * Record the outcome of the current step.
     *
     * @param success whether the step succeeded
     * @param details additional details about the outcome
     */
    public void recordStepOutcome(boolean success, String details) {
        if (currentStepIndex < currentPlan.size()) {
            PlanStep step = currentPlan.get(currentStepIndex);
            step.markCompleted(success, details);
        }

        if (success) {
            consecutiveFailures = 0;
            currentStepIndex++;
            logger.debug("Step {} succeeded, advancing to step {}", currentStepIndex, currentStepIndex + 1);
        } else {
            consecutiveFailures++;
            logger.debug("Step {} failed (consecutive failures: {})", currentStepIndex + 1, consecutiveFailures);
        }
    }

    /**
     * Check if replanning is needed due to stall.
     *
     * @return true if the agent should replan
     */
    public boolean shouldReplan() {
        return consecutiveFailures >= maxConsecutiveFailuresBeforeReplan && replanCount < maxReplans;
    }

    /**
     * Replan from the current point, considering previous failures.
     *
     * @param task the original task
     * @param currentState description of current browser state
     * @return the updated plan steps
     */
    public List<PlanStep> replan(String task, String currentState) {
        replanCount++;
        logger.info("Replanning (attempt {}/{}) after {} consecutive failures",
                replanCount, maxReplans, consecutiveFailures);

        String completedSteps = getCompletedStepsSummary();
        String nudge = getExplorationNudge();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(
                "You are a planning assistant. The previous plan stalled. " +
                "Create a new plan considering what has been done and what failed. " +
                "Suggestion: " + nudge + "\n" +
                "Return steps as numbered list, one step per line. " +
                "Format: 1. [action description]"));
        messages.add(ChatMessage.user(
                "Original task: " + task + "\n\n" +
                "Completed steps:\n" + completedSteps + "\n\n" +
                "Current browser state:\n" + currentState + "\n\n" +
                "The agent has failed " + consecutiveFailures + " times on the current step. " +
                "Please suggest an alternative approach."));

        try {
            LlmResponse response = llmProvider.chatCompletion(messages);
            String content = response.getContent();
            if (content != null && !content.isEmpty()) {
                // Keep completed steps, replace remaining
                List<PlanStep> completedPlanSteps = new ArrayList<>();
                for (int i = 0; i < currentStepIndex && i < currentPlan.size(); i++) {
                    completedPlanSteps.add(currentPlan.get(i));
                }
                currentPlan.clear();
                currentPlan.addAll(completedPlanSteps);
                currentPlan.addAll(parsePlanSteps(content));
                consecutiveFailures = 0;
                logger.info("Replanned: {} completed + {} new steps", completedPlanSteps.size(),
                        currentPlan.size() - completedPlanSteps.size());
            }
        } catch (Exception e) {
            logger.warn("Failed to replan: {}", e.getMessage());
        }

        return Collections.unmodifiableList(currentPlan);
    }

    /**
     * Get an exploration nudge to help the agent try different approaches.
     *
     * @return a suggestion string
     */
    public String getExplorationNudge() {
        int index = (consecutiveFailures + replanCount) % explorationNudges.size();
        return explorationNudges.get(index);
    }

    /**
     * Get the current plan step description.
     *
     * @return the current step or null if plan is complete
     */
    public String getCurrentStepDescription() {
        if (currentStepIndex < currentPlan.size()) {
            return currentPlan.get(currentStepIndex).getDescription();
        }
        return null;
    }

    /**
     * Check if the plan is complete (all steps done).
     */
    public boolean isPlanComplete() {
        return currentStepIndex >= currentPlan.size() && !currentPlan.isEmpty();
    }

    /**
     * Get all plan steps.
     */
    public List<PlanStep> getPlan() {
        return Collections.unmodifiableList(currentPlan);
    }

    /**
     * Get the current step index.
     */
    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    /**
     * Get the number of replans performed.
     */
    public int getReplanCount() {
        return replanCount;
    }

    /**
     * Get the number of consecutive failures.
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    private String getCompletedStepsSummary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentStepIndex && i < currentPlan.size(); i++) {
            PlanStep step = currentPlan.get(i);
            sb.append(String.format("%d. %s [%s]%s\n",
                    i + 1,
                    step.getDescription(),
                    step.isCompleted() ? (step.isSuccess() ? "SUCCESS" : "FAILED") : "PENDING",
                    step.getDetails() != null ? " - " + step.getDetails() : ""));
        }
        return sb.toString();
    }

    private List<PlanStep> parsePlanSteps(String content) {
        List<PlanStep> steps = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // Match numbered steps like "1. description" or "1) description"
            if (trimmed.matches("^\\d+[.)].+")) {
                String description = trimmed.replaceFirst("^\\d+[.)]\\s*", "").trim();
                if (!description.isEmpty()) {
                    steps.add(new PlanStep(steps.size() + 1, description));
                }
            }
        }
        return steps;
    }

    /**
     * Represents a single step in a plan.
     */
    public static class PlanStep {
        private final int stepNumber;
        private final String description;
        private boolean completed;
        private boolean success;
        private String details;

        public PlanStep(int stepNumber, String description) {
            this.stepNumber = stepNumber;
            this.description = description;
            this.completed = false;
            this.success = false;
            this.details = null;
        }

        public void markCompleted(boolean success, String details) {
            this.completed = true;
            this.success = success;
            this.details = details;
        }

        public int getStepNumber() { return stepNumber; }
        public String getDescription() { return description; }
        public boolean isCompleted() { return completed; }
        public boolean isSuccess() { return success; }
        public String getDetails() { return details; }

        @Override
        public String toString() {
            return String.format("Step %d: %s [%s]", stepNumber, description,
                    completed ? (success ? "SUCCESS" : "FAILED") : "PENDING");
        }
    }
}
