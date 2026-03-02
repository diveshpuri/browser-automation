package com.browserautomation.agent;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates agent performance and task completion quality.
 *
 * Uses an LLM to assess whether the agent successfully completed
 * the given task based on the execution history and final state.
 */
public class AgentJudge {

    private static final Logger logger = LoggerFactory.getLogger(AgentJudge.class);

    private final LlmProvider llmProvider;

    public AgentJudge(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    /**
     * Evaluate whether the agent successfully completed its task.
     *
     * @param task   the original task description
     * @param result the agent's execution result
     * @return evaluation result with score and feedback
     */
    public Evaluation evaluate(String task, AgentResult result) {
        logger.info("Evaluating agent performance for task: {}", task);

        String prompt = buildEvaluationPrompt(task, result);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(JUDGE_SYSTEM_PROMPT));
        messages.add(ChatMessage.user(prompt));

        try {
            LlmResponse response = llmProvider.chatCompletion(messages, null);
            return parseEvaluation(response.getContent());
        } catch (Exception e) {
            logger.error("Failed to evaluate agent performance: {}", e.getMessage());
            return new Evaluation(0.0, false, "Evaluation failed: " + e.getMessage(),
                    List.of("Could not complete evaluation"));
        }
    }

    /**
     * Evaluate a specific step in the agent's execution.
     *
     * @param task       the original task
     * @param step       the step to evaluate
     * @param stepNumber the step number
     * @return evaluation of the individual step
     */
    public StepEvaluation evaluateStep(String task, AgentState.AgentStep step, int stepNumber) {
        String reasoning = step.getLlmThinking();
        String action = step.getActionName();
        String actionResult = step.getActionResult();
        String error = step.getError();

        boolean wasRelevant = action != null && !action.equals("none");
        boolean wasSuccessful = error == null || error.isEmpty();

        String feedback;
        if (!wasSuccessful) {
            feedback = "Step failed with error: " + error;
        } else if (!wasRelevant) {
            feedback = "No action was taken in this step";
        } else {
            feedback = "Action " + action + " executed successfully";
        }

        return new StepEvaluation(stepNumber, wasRelevant, wasSuccessful, feedback);
    }

    private String buildEvaluationPrompt(String task, AgentResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Task\n").append(task).append("\n\n");
        sb.append("## Execution Summary\n");
        sb.append("- Success: ").append(result.isSuccess()).append("\n");
        sb.append("- Total Steps: ").append(result.getTotalSteps()).append("\n");
        sb.append("- Duration: ").append(result.getTotalDurationMs()).append("ms\n");
        sb.append("- Final Result: ").append(result.getResult()).append("\n\n");

        sb.append("## Step History\n");
        List<AgentState.AgentStep> history = result.getHistory();
        for (int i = 0; i < history.size(); i++) {
            AgentState.AgentStep step = history.get(i);
            sb.append("Step ").append(i + 1).append(": ").append(step.getSummary()).append("\n");
        }

        sb.append("\nPlease evaluate this execution. Respond with:\n");
        sb.append("SCORE: (0.0 to 1.0)\n");
        sb.append("COMPLETED: (true/false)\n");
        sb.append("FEEDBACK: (your assessment)\n");
        sb.append("ISSUES: (comma-separated list of issues, or 'none')\n");

        return sb.toString();
    }

    private Evaluation parseEvaluation(String response) {
        if (response == null || response.isEmpty()) {
            return new Evaluation(0.0, false, "Empty evaluation response", List.of());
        }

        double score = 0.0;
        boolean completed = false;
        String feedback = response;
        List<String> issues = new ArrayList<>();

        for (String line : response.split("\n")) {
            line = line.trim();
            if (line.startsWith("SCORE:")) {
                try {
                    score = Double.parseDouble(line.substring(6).trim());
                } catch (NumberFormatException ignored) {
                }
            } else if (line.startsWith("COMPLETED:")) {
                completed = Boolean.parseBoolean(line.substring(10).trim());
            } else if (line.startsWith("FEEDBACK:")) {
                feedback = line.substring(9).trim();
            } else if (line.startsWith("ISSUES:")) {
                String issuesStr = line.substring(7).trim();
                if (!issuesStr.equalsIgnoreCase("none")) {
                    for (String issue : issuesStr.split(",")) {
                        String trimmed = issue.trim();
                        if (!trimmed.isEmpty()) {
                            issues.add(trimmed);
                        }
                    }
                }
            }
        }

        return new Evaluation(score, completed, feedback, issues);
    }

    /**
     * Result of evaluating an agent's overall execution.
     */
    public static class Evaluation {
        private final double score;
        private final boolean taskCompleted;
        private final String feedback;
        private final List<String> issues;

        public Evaluation(double score, boolean taskCompleted, String feedback, List<String> issues) {
            this.score = Math.max(0.0, Math.min(1.0, score));
            this.taskCompleted = taskCompleted;
            this.feedback = feedback;
            this.issues = issues != null ? List.copyOf(issues) : List.of();
        }

        public double getScore() { return score; }
        public boolean isTaskCompleted() { return taskCompleted; }
        public String getFeedback() { return feedback; }
        public List<String> getIssues() { return issues; }

        @Override
        public String toString() {
            return String.format("Evaluation[score=%.2f, completed=%s, issues=%d, feedback=%s]",
                    score, taskCompleted, issues.size(), feedback);
        }
    }

    /**
     * Result of evaluating a single agent step.
     */
    public static class StepEvaluation {
        private final int stepNumber;
        private final boolean relevant;
        private final boolean successful;
        private final String feedback;

        public StepEvaluation(int stepNumber, boolean relevant, boolean successful, String feedback) {
            this.stepNumber = stepNumber;
            this.relevant = relevant;
            this.successful = successful;
            this.feedback = feedback;
        }

        public int getStepNumber() { return stepNumber; }
        public boolean isRelevant() { return relevant; }
        public boolean isSuccessful() { return successful; }
        public String getFeedback() { return feedback; }

        @Override
        public String toString() {
            return String.format("StepEvaluation[step=%d, relevant=%s, success=%s]",
                    stepNumber, relevant, successful);
        }
    }

    private static final String JUDGE_SYSTEM_PROMPT = """
            You are an AI agent evaluator. Your job is to assess whether a browser automation agent \
            successfully completed its assigned task. Analyze the execution history and provide a fair evaluation.
            
            Scoring guidelines:
            - 1.0: Task perfectly completed
            - 0.8-0.9: Task mostly completed with minor issues
            - 0.5-0.7: Task partially completed
            - 0.2-0.4: Task attempted but not completed
            - 0.0-0.1: Task not attempted or completely failed
            
            Always respond in the exact format requested.""";
}
