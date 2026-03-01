package com.browserautomation.agent;

import com.browserautomation.action.ActionRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Generates the system prompt for the agent based on configuration and available actions.
 * Equivalent to browser-use's SystemPrompt.
 */
public class SystemPrompt {

    private static final String PROMPT_RESOURCE = "/system_prompt.md";

    private final AgentConfig config;
    private final ActionRegistry actionRegistry;

    public SystemPrompt(AgentConfig config, ActionRegistry actionRegistry) {
        this.config = config;
        this.actionRegistry = actionRegistry;
    }

    /**
     * Build the full system prompt string.
     */
    public String build() {
        if (config.getOverrideSystemMessage() != null) {
            String prompt = config.getOverrideSystemMessage();
            if (config.getExtendSystemMessage() != null) {
                prompt += "\n" + config.getExtendSystemMessage();
            }
            return prompt;
        }

        String template = loadPromptTemplate();
        String prompt = template
                .replace("{{MAX_ACTIONS}}", String.valueOf(config.getMaxActionsPerStep()))
                .replace("{{CURRENT_DATE}}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .replace("{{AVAILABLE_ACTIONS}}", actionRegistry.getActionsDescription());

        if (config.getExtendSystemMessage() != null) {
            prompt += "\n" + config.getExtendSystemMessage();
        }

        return prompt;
    }

    private String loadPromptTemplate() {
        try (InputStream is = getClass().getResourceAsStream(PROMPT_RESOURCE)) {
            if (is == null) {
                return getDefaultPromptTemplate();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            return getDefaultPromptTemplate();
        }
    }

    private String getDefaultPromptTemplate() {
        return """
                You are an AI agent that controls a web browser to complete tasks. You interact with web pages by analyzing the DOM state and performing actions like clicking, typing, scrolling, and navigating.
                
                Current date and time: {{CURRENT_DATE}}
                
                ## Instructions
                
                1. Analyze the current browser state (URL, page content, interactive elements)
                2. Decide which action to take next to progress toward completing the task
                3. Execute up to {{MAX_ACTIONS}} actions per step
                4. When the task is complete, use the `done` action with a summary of the result
                
                ## Important Rules
                
                - Only interact with elements that are visible and have an index number [N]
                - When you see elements like [1], [2], etc., use these index numbers for click/type actions
                - If a page is loading, wait before taking action
                - If you get stuck, try a different approach (e.g., search, navigate to a different page)
                - Always verify that your actions had the expected effect by checking the updated DOM state
                - Be concise in your responses
                
                ## Available Actions
                
                {{AVAILABLE_ACTIONS}}
                
                ## Response Format
                
                For each step, you should:
                1. Evaluate the result of the previous action (if any)
                2. Think about what you need to do next
                3. Choose one or more actions to execute (up to {{MAX_ACTIONS}} per step)
                
                Use the tool/function calling mechanism to invoke actions.
                """;
    }
}
