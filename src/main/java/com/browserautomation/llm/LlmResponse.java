package com.browserautomation.llm;

import java.util.List;
import java.util.Map;

/**
 * Structured response from an LLM, containing the model's output
 * and any tool/function calls it wants to make.
 */
public class LlmResponse {

    private final String content;
    private final List<ToolCall> toolCalls;
    private final int promptTokens;
    private final int completionTokens;

    public LlmResponse(String content, List<ToolCall> toolCalls, int promptTokens, int completionTokens) {
        this.content = content;
        this.toolCalls = toolCalls;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    public String getContent() {
        return content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getTotalTokens() {
        return promptTokens + completionTokens;
    }

    /**
     * Represents a tool/function call requested by the LLM.
     */
    public static class ToolCall {
        private final String id;
        private final String functionName;
        private final Map<String, Object> arguments;

        public ToolCall(String id, String functionName, Map<String, Object> arguments) {
            this.id = id;
            this.functionName = functionName;
            this.arguments = arguments;
        }

        public String getId() {
            return id;
        }

        public String getFunctionName() {
            return functionName;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        @Override
        public String toString() {
            return functionName + "(" + arguments + ")";
        }
    }
}
