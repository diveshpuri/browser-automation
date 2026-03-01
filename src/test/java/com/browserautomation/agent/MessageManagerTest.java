package com.browserautomation.agent;

import com.browserautomation.llm.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MessageManager.
 */
class MessageManagerTest {

    @Test
    void testDefaultConstruction() {
        MessageManager manager = new MessageManager();
        assertEquals(0, manager.getMessageCount());
        assertEquals(0, manager.estimateTokenCount());
    }

    @Test
    void testAddMessage() {
        MessageManager manager = new MessageManager();
        manager.addMessage(ChatMessage.system("You are a helpful assistant."));
        manager.addMessage(ChatMessage.user("Hello"));

        assertEquals(2, manager.getMessageCount());
        assertTrue(manager.estimateTokenCount() > 0);
    }

    @Test
    void testGetMessages() {
        MessageManager manager = new MessageManager();
        manager.addMessage(ChatMessage.system("System message"));
        manager.addMessage(ChatMessage.user("User message"));

        assertEquals(2, manager.getMessages().size());
        assertEquals(ChatMessage.Role.SYSTEM, manager.getMessages().get(0).getRole());
        assertEquals(ChatMessage.Role.USER, manager.getMessages().get(1).getRole());
    }

    @Test
    void testClearKeepsSystemMessage() {
        MessageManager manager = new MessageManager();
        manager.addMessage(ChatMessage.system("System message"));
        manager.addMessage(ChatMessage.user("User message"));
        manager.addMessage(ChatMessage.assistant("Assistant message"));

        manager.clear();

        assertEquals(1, manager.getMessageCount());
        assertEquals(ChatMessage.Role.SYSTEM, manager.getMessages().get(0).getRole());
    }

    @Test
    void testEstimateTokenCount() {
        MessageManager manager = new MessageManager();
        // "Hello World" = 11 chars / 4 chars per token ≈ 3 tokens
        manager.addMessage(ChatMessage.user("Hello World"));

        assertTrue(manager.estimateTokenCount() > 0);
    }

    @Test
    void testMessageCountPruning() {
        // Create a manager with max 5 messages
        MessageManager manager = new MessageManager(128000, 5, true, 2, false);
        manager.addMessage(ChatMessage.system("System"));

        // Add more than 5 messages
        for (int i = 0; i < 10; i++) {
            manager.addMessage(ChatMessage.user("Message " + i));
        }

        // Should be pruned to maxMessages or fewer
        assertTrue(manager.getMessageCount() <= 6); // 5 max + summary
    }

    @Test
    void testCustomConfiguration() {
        MessageManager manager = new MessageManager(64000, 50, false, 5, false);
        assertEquals(64000, manager.getMaxTokens());
        assertEquals(50, manager.getMaxMessages());
        assertFalse(manager.isKeepSystemMessage());
        assertEquals(5, manager.getRecentMessagesToKeep());
        assertFalse(manager.isRemoveOldScreenshots());
    }

    @Test
    void testAddMultipleMessages() {
        MessageManager manager = new MessageManager();
        manager.addMessages(java.util.List.of(
                ChatMessage.system("System"),
                ChatMessage.user("User 1"),
                ChatMessage.user("User 2")
        ));

        assertEquals(3, manager.getMessageCount());
    }
}
