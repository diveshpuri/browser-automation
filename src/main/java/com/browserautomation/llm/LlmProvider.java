package com.browserautomation.llm;

import java.util.List;
import java.util.Map;

/**
 * Interface for LLM providers (OpenAI, Anthropic, etc.).
 * Implementations handle the HTTP communication with the LLM API.
 */
public interface LlmProvider {

    /**
     * Send a chat completion request to the LLM.
     *
     * @param messages the conversation messages
     * @param tools    the available tool definitions (can be null for no tools)
     * @return the LLM response
     */
    LlmResponse chatCompletion(List<ChatMessage> messages, List<Map<String, Object>> tools);

    /**
     * Send a simple chat completion request without tools.
     *
     * @param messages the conversation messages
     * @return the LLM response
     */
    default LlmResponse chatCompletion(List<ChatMessage> messages) {
        return chatCompletion(messages, null);
    }

    /**
     * Get the provider name.
     */
    String getProviderName();

    /**
     * Get the model name being used.
     */
    String getModelName();

    /**
     * Check if this provider supports vision (image inputs).
     */
    boolean supportsVision();
}
