package com.browserautomation.token;

import com.browserautomation.llm.ChatMessage;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Token counting utility for estimating token usage.
 * Equivalent to browser-use's tokens module.
 *
 * <p>Provides approximate token counting for different models and providers.
 * Uses character-based estimation with model-specific adjustments.</p>
 */
public class TokenCounter {

    /** Average characters per token for English text (GPT models) */
    private static final double DEFAULT_CHARS_PER_TOKEN = 4.0;

    /** Pattern for splitting text into approximate tokens */
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\s+|[^\\s\\w]|\\w+");

    private TokenCounter() {
        // Utility class
    }

    /**
     * Estimate token count for a text string.
     *
     * @param text the text to count tokens for
     * @return estimated token count
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / DEFAULT_CHARS_PER_TOKEN);
    }

    /**
     * Estimate token count for a text string with a model-specific multiplier.
     *
     * @param text  the text to count tokens for
     * @param model the model name for model-specific adjustments
     * @return estimated token count
     */
    public static int estimateTokens(String text, String model) {
        if (text == null || text.isEmpty()) return 0;

        double charsPerToken = DEFAULT_CHARS_PER_TOKEN;
        if (model != null) {
            if (model.contains("claude")) {
                charsPerToken = 3.5; // Claude tends to use slightly more tokens
            } else if (model.contains("gemini")) {
                charsPerToken = 4.0;
            } else if (model.contains("llama") || model.contains("mixtral")) {
                charsPerToken = 3.8;
            }
        }
        return (int) Math.ceil(text.length() / charsPerToken);
    }

    /**
     * Estimate token count for a list of chat messages.
     *
     * @param messages the messages to count tokens for
     * @return estimated total token count
     */
    public static int estimateTokens(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage msg : messages) {
            // Per-message overhead (role, formatting)
            total += 4;
            for (ChatMessage.ContentPart part : msg.getContent()) {
                if (part.getType() == ChatMessage.ContentPart.Type.TEXT) {
                    total += estimateTokens(part.getText());
                } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                    // Images consume ~765 tokens for high detail, ~85 for low detail
                    total += 765;
                }
            }
        }
        // Reply priming overhead
        total += 3;
        return total;
    }

    /**
     * Check if adding a message would exceed the token limit.
     *
     * @param currentMessages current conversation messages
     * @param newMessage      the message to potentially add
     * @param maxTokens       the maximum token limit
     * @return true if adding the message would exceed the limit
     */
    public static boolean wouldExceedLimit(List<ChatMessage> currentMessages,
                                           ChatMessage newMessage, int maxTokens) {
        int currentTokens = estimateTokens(currentMessages);
        int newTokens = estimateTokens(List.of(newMessage));
        return (currentTokens + newTokens) > maxTokens;
    }

    /**
     * Get the context window size for a known model.
     *
     * @param model the model name
     * @return the context window size in tokens, or -1 if unknown
     */
    public static int getContextWindowSize(String model) {
        if (model == null) return -1;

        // OpenAI models
        if (model.contains("gpt-4o")) return 128000;
        if (model.contains("gpt-4-turbo")) return 128000;
        if (model.contains("gpt-4")) return 8192;
        if (model.contains("gpt-3.5-turbo")) return 16385;

        // Anthropic models
        if (model.contains("claude-3") || model.contains("claude-sonnet-4") || model.contains("claude-opus-4")) return 200000;

        // Google models
        if (model.contains("gemini-2")) return 1048576;
        if (model.contains("gemini-1.5-pro")) return 2097152;
        if (model.contains("gemini-1.5-flash")) return 1048576;

        // DeepSeek
        if (model.contains("deepseek-chat")) return 64000;
        if (model.contains("deepseek-reasoner")) return 64000;

        // Groq models
        if (model.contains("llama-3.3-70b")) return 131072;
        if (model.contains("mixtral-8x7b")) return 32768;

        // Mistral models
        if (model.contains("mistral-large")) return 128000;
        if (model.contains("mistral-small")) return 32000;

        return -1;
    }
}
