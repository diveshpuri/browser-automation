package com.browserautomation.agent.history;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for saving and replaying agent execution history traces.
 * Serializes agent history to JSON and supports replaying against a browser session.
 *
 */
public class RerunHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(RerunHistoryService.class);

    private final ObjectMapper objectMapper;

    public RerunHistoryService() {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Save agent execution history to a file.
     */
    public void saveHistory(AgentHistoryTrace trace, Path filePath) throws IOException {
        String json = objectMapper.writeValueAsString(trace);
        Files.writeString(filePath, json);
        logger.info("Agent history saved to: {} ({} steps)", filePath, trace.getSteps().size());
    }

    /**
     * Load agent execution history from a file.
     */
    public AgentHistoryTrace loadHistory(Path filePath) throws IOException {
        String json = Files.readString(filePath);
        AgentHistoryTrace trace = objectMapper.readValue(json, AgentHistoryTrace.class);
        logger.info("Agent history loaded from: {} ({} steps)", filePath, trace.getSteps().size());
        return trace;
    }

    /**
     * Create an action replay plan from history.
     */
    public List<ReplayAction> createReplayPlan(AgentHistoryTrace trace) {
        List<ReplayAction> actions = new ArrayList<>();
        for (HistoryStep step : trace.getSteps()) {
            for (HistoryAction action : step.getActions()) {
                actions.add(new ReplayAction(
                        action.getActionType(),
                        action.getParameters(),
                        step.getStepNumber(),
                        action.getExpectedResult()
                ));
            }
        }
        return actions;
    }

    /**
     * Complete agent execution history trace.
     */
    public static class AgentHistoryTrace {
        @JsonProperty("task")
        private String task;
        @JsonProperty("startTime")
        private String startTime;
        @JsonProperty("endTime")
        private String endTime;
        @JsonProperty("totalSteps")
        private int totalSteps;
        @JsonProperty("success")
        private boolean success;
        @JsonProperty("result")
        private String result;
        @JsonProperty("steps")
        private List<HistoryStep> steps;

        public AgentHistoryTrace() {
            this.steps = new ArrayList<>();
        }

        public AgentHistoryTrace(String task) {
            this.task = task;
            this.startTime = Instant.now().toString();
            this.steps = new ArrayList<>();
        }

        public void addStep(HistoryStep step) {
            steps.add(step);
            totalSteps = steps.size();
        }

        public void complete(boolean success, String result) {
            this.success = success;
            this.result = result;
            this.endTime = Instant.now().toString();
        }

        public String getTask() { return task; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public int getTotalSteps() { return totalSteps; }
        public boolean isSuccess() { return success; }
        public String getResult() { return result; }
        public List<HistoryStep> getSteps() { return steps; }

        public void setTask(String task) { this.task = task; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        public void setSuccess(boolean success) { this.success = success; }
        public void setResult(String result) { this.result = result; }
        public void setSteps(List<HistoryStep> steps) { this.steps = steps; }
    }

    /**
     * A single step in the execution history.
     */
    public static class HistoryStep {
        @JsonProperty("stepNumber")
        private int stepNumber;
        @JsonProperty("url")
        private String url;
        @JsonProperty("thought")
        private String thought;
        @JsonProperty("actions")
        private List<HistoryAction> actions;
        @JsonProperty("success")
        private boolean success;
        @JsonProperty("error")
        private String error;

        public HistoryStep() {
            this.actions = new ArrayList<>();
        }

        public HistoryStep(int stepNumber, String url, String thought) {
            this.stepNumber = stepNumber;
            this.url = url;
            this.thought = thought;
            this.actions = new ArrayList<>();
        }

        public void addAction(HistoryAction action) { actions.add(action); }

        public int getStepNumber() { return stepNumber; }
        public String getUrl() { return url; }
        public String getThought() { return thought; }
        public List<HistoryAction> getActions() { return actions; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }

        public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
        public void setUrl(String url) { this.url = url; }
        public void setThought(String thought) { this.thought = thought; }
        public void setActions(List<HistoryAction> actions) { this.actions = actions; }
        public void setSuccess(boolean success) { this.success = success; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * A single action within a history step.
     */
    public static class HistoryAction {
        @JsonProperty("actionType")
        private String actionType;
        @JsonProperty("parameters")
        private Map<String, Object> parameters;
        @JsonProperty("expectedResult")
        private String expectedResult;
        @JsonProperty("success")
        private boolean success;

        public HistoryAction() {}

        public HistoryAction(String actionType, Map<String, Object> parameters) {
            this.actionType = actionType;
            this.parameters = parameters;
        }

        public String getActionType() { return actionType; }
        public Map<String, Object> getParameters() { return parameters; }
        public String getExpectedResult() { return expectedResult; }
        public boolean isSuccess() { return success; }

        public void setActionType(String actionType) { this.actionType = actionType; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    /**
     * A single action to replay from history.
     */
    public record ReplayAction(String actionType, Map<String, Object> parameters,
                                int originalStep, String expectedResult) {}
}
