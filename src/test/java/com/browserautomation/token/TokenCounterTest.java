package com.browserautomation.token;

import com.browserautomation.llm.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the TokenCounter.
 */
class TokenCounterTest {

    @Test
    void testEstimateTokensNull() {
        assertEquals(0, TokenCounter.estimateTokens((String) null));
    }

    @Test
    void testEstimateTokensEmpty() {
        assertEquals(0, TokenCounter.estimateTokens(""));
    }

    @Test
    void testEstimateTokensText() {
        // "Hello World" = 11 chars / 4 chars per token ≈ 3 tokens
        int tokens = TokenCounter.estimateTokens("Hello World");
        assertTrue(tokens > 0);
        assertTrue(tokens < 10);
    }

    @Test
    void testEstimateTokensLongText() {
        String text = "A".repeat(4000);
        int tokens = TokenCounter.estimateTokens(text);
        assertEquals(1000, tokens);
    }

    @Test
    void testEstimateTokensWithModel() {
        int openaiTokens = TokenCounter.estimateTokens("Hello World", "gpt-4o");
        int claudeTokens = TokenCounter.estimateTokens("Hello World", "claude-3-opus");

        // Claude should use more tokens (lower chars per token)
        assertTrue(claudeTokens >= openaiTokens);
    }

    @Test
    void testEstimateTokensMessages() {
        List<ChatMessage> messages = List.of(
                ChatMessage.system("You are a helpful assistant."),
                ChatMessage.user("Hello")
        );
        int tokens = TokenCounter.estimateTokens(messages);
        assertTrue(tokens > 0);
    }

    @Test
    void testWouldExceedLimit() {
        List<ChatMessage> messages = List.of(
                ChatMessage.system("System message")
        );
        ChatMessage newMessage = ChatMessage.user("Short message");

        assertFalse(TokenCounter.wouldExceedLimit(messages, newMessage, 1000));
        assertTrue(TokenCounter.wouldExceedLimit(messages, newMessage, 1));
    }

    @Test
    void testGetContextWindowSize() {
        assertEquals(128000, TokenCounter.getContextWindowSize("gpt-4o"));
        assertEquals(200000, TokenCounter.getContextWindowSize("claude-3-opus"));
        assertEquals(64000, TokenCounter.getContextWindowSize("deepseek-chat"));
        assertEquals(-1, TokenCounter.getContextWindowSize("unknown-model"));
        assertEquals(-1, TokenCounter.getContextWindowSize(null));
    }

    @Test
    void testContextWindowKnownModels() {
        assertTrue(TokenCounter.getContextWindowSize("gpt-4o-mini") > 0);
        assertTrue(TokenCounter.getContextWindowSize("gemini-2.0-flash") > 0);
        assertTrue(TokenCounter.getContextWindowSize("llama-3.3-70b-versatile") > 0);
        assertTrue(TokenCounter.getContextWindowSize("mistral-large-latest") > 0);
    }
}
