package com.browserautomation.agent.compaction;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageCompactionServiceTest {

    private LlmProvider mockProvider;
    private MessageCompactionService service;

    @BeforeEach
    void setUp() {
        mockProvider = mock(LlmProvider.class);
        service = new MessageCompactionService(mockProvider,
                new MessageCompactionService.CompactionConfig()
                        .minMessagesBeforeCompaction(5)
                        .compactionInterval(5)
                        .keepLastN(3)
                        .tokenThreshold(2000));
    }

    @Test
    void testShouldNotCompactWhenBelowMinMessages() {
        List<ChatMessage> messages = List.of(
                ChatMessage.system("You are a helper"),
                ChatMessage.user("Hello"),
                ChatMessage.assistant("Hi")
        );
        assertFalse(service.shouldCompact(messages));
    }

    @Test
    void testShouldCompactAtInterval() {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(ChatMessage.user("Message " + i));
        }
        assertTrue(service.shouldCompact(messages));
    }

    @Test
    void testCompactReducesMessageCount() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Summary of previous conversation", null, 30, 50));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system("System prompt"));
        for (int i = 0; i < 10; i++) {
            messages.add(ChatMessage.user("User message " + i + " with some content"));
            messages.add(ChatMessage.assistant("Response " + i + " with some content"));
        }

        MessageCompactionService.CompactionResult result = service.compact(messages);
        assertTrue(result.wasCompacted());
        assertTrue(result.messages().size() < messages.size());
        // Should have: system prompt + summary + 3 recent messages
        assertEquals(5, result.messages().size());
    }

    @Test
    void testCompactPreservesSystemPrompt() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Summary", null, 10, 20));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system("System prompt"));
        for (int i = 0; i < 10; i++) {
            messages.add(ChatMessage.user("Message " + i));
        }

        MessageCompactionService.CompactionResult result = service.compact(messages);
        assertEquals("system", result.messages().get(0).getRoleString());
        assertEquals("System prompt", result.messages().get(0).getText());
    }

    @Test
    void testCompactPreservesRecentMessages() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Summary", null, 10, 20));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system("System prompt"));
        for (int i = 0; i < 10; i++) {
            messages.add(ChatMessage.user("Message " + i));
        }

        MessageCompactionService.CompactionResult result = service.compact(messages);
        // Last message should be preserved
        String lastContent = result.messages().get(result.messages().size() - 1).getText();
        assertEquals("Message 9", lastContent);
    }

    @Test
    void testCompactWithLlmFailureFallsBackToSimpleSummary() {
        when(mockProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("LLM error"));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system("System prompt"));
        for (int i = 0; i < 10; i++) {
            messages.add(ChatMessage.user("Message " + i));
        }

        MessageCompactionService.CompactionResult result = service.compact(messages);
        assertTrue(result.wasCompacted());
        // Summary message should contain "[Conversation Summary]"
        assertTrue(result.messages().get(1).getText().contains("[Conversation Summary]"));
    }

    @Test
    void testEstimateTokenCount() {
        List<ChatMessage> messages = List.of(
                ChatMessage.user("Hello world") // 11 chars
        );
        int tokens = service.estimateTokenCount(messages);
        assertEquals(2, tokens); // 11 / 4 = 2
    }

    @Test
    void testCompactionCountAndTokensSaved() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Brief summary", null, 10, 15));

        assertEquals(0, service.getCompactionCount());
        assertEquals(0, service.getTotalTokensSaved());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system("System prompt"));
        for (int i = 0; i < 10; i++) {
            messages.add(ChatMessage.user("Long message content for testing " + i));
        }

        service.compact(messages);
        assertEquals(1, service.getCompactionCount());
    }

    @Test
    void testDoesNotCompactWhenTooFewMessages() {
        List<ChatMessage> messages = List.of(
                ChatMessage.system("System prompt"),
                ChatMessage.user("Hello")
        );

        MessageCompactionService.CompactionResult result = service.compact(messages);
        assertFalse(result.wasCompacted());
        assertEquals(messages.size(), result.messages().size());
    }

    @Test
    void testCompactionConfig() {
        MessageCompactionService.CompactionConfig config = new MessageCompactionService.CompactionConfig()
                .compactionInterval(15)
                .tokenThreshold(10000)
                .keepLastN(10)
                .minMessagesBeforeCompaction(20);

        assertEquals(15, config.getCompactionInterval());
        assertEquals(10000, config.getTokenThreshold());
        assertEquals(10, config.getKeepLastN());
        assertEquals(20, config.getMinMessagesBeforeCompaction());
    }
}
