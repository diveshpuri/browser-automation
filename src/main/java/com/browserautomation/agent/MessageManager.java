package com.browserautomation.agent;

import com.browserautomation.llm.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the conversation message history for the agent.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>Token-aware history pruning to stay within context limits</li>
 *   <li>Preserving system messages and recent context</li>
 *   <li>Image content management (removing old screenshots)</li>
 *   <li>Summarization of pruned history</li>
 * </ul>
 */
public class MessageManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageManager.class);

    /** Approximate characters per token (rough estimate for pruning decisions) */
    private static final int CHARS_PER_TOKEN = 4;

    private final int maxTokens;
    private final int maxMessages;
    private final boolean keepSystemMessage;
    private final int recentMessagesToKeep;
    private final boolean removeOldScreenshots;
    private final List<ChatMessage> messages;

    /**
     * Create a MessageManager with default settings.
     */
    public MessageManager() {
        this(128000, 100, true, 10, true);
    }

    /**
     * Create a MessageManager with custom settings.
     *
     * @param maxTokens            approximate max tokens for the conversation history
     * @param maxMessages          maximum number of messages to keep
     * @param keepSystemMessage    whether to always keep the system message
     * @param recentMessagesToKeep minimum number of recent messages to preserve
     * @param removeOldScreenshots whether to strip image content from older messages
     */
    public MessageManager(int maxTokens, int maxMessages, boolean keepSystemMessage,
                          int recentMessagesToKeep, boolean removeOldScreenshots) {
        this.maxTokens = maxTokens;
        this.maxMessages = maxMessages;
        this.keepSystemMessage = keepSystemMessage;
        this.recentMessagesToKeep = recentMessagesToKeep;
        this.removeOldScreenshots = removeOldScreenshots;
        this.messages = new ArrayList<>();
    }

    /**
     * Add a message to the conversation history.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        pruneIfNeeded();
    }

    /**
     * Add multiple messages to the conversation history.
     */
    public void addMessages(List<ChatMessage> newMessages) {
        messages.addAll(newMessages);
        pruneIfNeeded();
    }

    /**
     * Get the current conversation messages (after any pruning).
     *
     * @return unmodifiable list of messages
     */
    public List<ChatMessage> getMessages() {
        return List.copyOf(messages);
    }

    /**
     * Get the estimated token count of the current conversation.
     */
    public int estimateTokenCount() {
        int totalChars = 0;
        for (ChatMessage msg : messages) {
            for (ChatMessage.ContentPart part : msg.getContent()) {
                if (part.getType() == ChatMessage.ContentPart.Type.TEXT && part.getText() != null) {
                    totalChars += part.getText().length();
                } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                    // Images consume ~85 tokens for low detail, ~765 for high detail
                    totalChars += 3000; // approximate
                }
            }
        }
        return totalChars / CHARS_PER_TOKEN;
    }

    /**
     * Get the number of messages in the conversation.
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * Clear all messages except the system message (if present).
     */
    public void clear() {
        ChatMessage systemMsg = null;
        if (keepSystemMessage && !messages.isEmpty()
                && messages.get(0).getRole() == ChatMessage.Role.SYSTEM) {
            systemMsg = messages.get(0);
        }
        messages.clear();
        if (systemMsg != null) {
            messages.add(systemMsg);
        }
    }

    /**
     * Prune messages if over token or message limits.
     */
    private void pruneIfNeeded() {
        // Remove old screenshots first
        if (removeOldScreenshots) {
            stripOldScreenshots();
        }

        // Prune by message count
        if (messages.size() > maxMessages) {
            pruneByMessageCount();
        }

        // Prune by token count
        int estimatedTokens = estimateTokenCount();
        if (estimatedTokens > maxTokens) {
            pruneByTokenCount(estimatedTokens);
        }
    }

    /**
     * Strip image content from older messages, keeping only the most recent ones.
     */
    private void stripOldScreenshots() {
        int imageCount = 0;
        // Count from end, keep only last 2 images
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            boolean hasImage = msg.getContent().stream()
                    .anyMatch(p -> p.getType() == ChatMessage.ContentPart.Type.IMAGE);
            if (hasImage) {
                imageCount++;
                if (imageCount > 2) {
                    // Replace with text-only version
                    String text = msg.getText();
                    if (text != null && !text.isEmpty()) {
                        ChatMessage replacement;
                        switch (msg.getRole()) {
                            case USER -> replacement = ChatMessage.user(text + "\n[Screenshot removed to save context]");
                            case ASSISTANT -> replacement = ChatMessage.assistant(text);
                            default -> replacement = msg;
                        }
                        messages.set(i, replacement);
                        logger.debug("Stripped screenshot from message at index {}", i);
                    }
                }
            }
        }
    }

    /**
     * Prune oldest non-system messages to stay within message count limit.
     */
    private void pruneByMessageCount() {
        int startIndex = keepSystemMessage && !messages.isEmpty()
                && messages.get(0).getRole() == ChatMessage.Role.SYSTEM ? 1 : 0;

        int messagesToRemove = messages.size() - maxMessages;
        if (messagesToRemove <= 0) return;

        // Keep the system message and recent messages
        int removeEnd = Math.min(startIndex + messagesToRemove, messages.size() - recentMessagesToKeep);
        if (removeEnd <= startIndex) return;

        // Create summary of removed messages
        StringBuilder summary = new StringBuilder("Previous conversation summary: ");
        int actionCount = 0;
        for (int i = startIndex; i < removeEnd; i++) {
            ChatMessage msg = messages.get(i);
            if (msg.getRole() == ChatMessage.Role.TOOL) {
                actionCount++;
            }
        }
        summary.append(actionCount).append(" actions were executed. ");

        // Remove the messages
        List<ChatMessage> removed = new ArrayList<>(messages.subList(startIndex, removeEnd));
        messages.subList(startIndex, removeEnd).clear();

        // Insert summary after system message
        messages.add(startIndex, ChatMessage.user(summary.toString()));

        logger.info("Pruned {} messages (by count limit {}), inserted summary", removed.size(), maxMessages);
    }

    /**
     * Prune messages to stay within the token limit.
     */
    private void pruneByTokenCount(int currentTokens) {
        int startIndex = keepSystemMessage && !messages.isEmpty()
                && messages.get(0).getRole() == ChatMessage.Role.SYSTEM ? 1 : 0;

        int tokensToRemove = currentTokens - (int)(maxTokens * 0.8); // Target 80% of max
        int tokensRemoved = 0;

        int removeEnd = startIndex;
        while (removeEnd < messages.size() - recentMessagesToKeep && tokensRemoved < tokensToRemove) {
            ChatMessage msg = messages.get(removeEnd);
            tokensRemoved += estimateMessageTokens(msg);
            removeEnd++;
        }

        if (removeEnd <= startIndex) return;

        messages.subList(startIndex, removeEnd).clear();
        messages.add(startIndex, ChatMessage.user(
                "[Conversation history pruned to fit context window. " + tokensRemoved + " tokens removed.]"));

        logger.info("Pruned {} tokens (target: {}), current estimate: {}",
                tokensRemoved, maxTokens, estimateTokenCount());
    }

    /**
     * Estimate the token count of a single message.
     */
    private int estimateMessageTokens(ChatMessage message) {
        int chars = 0;
        for (ChatMessage.ContentPart part : message.getContent()) {
            if (part.getType() == ChatMessage.ContentPart.Type.TEXT && part.getText() != null) {
                chars += part.getText().length();
            } else if (part.getType() == ChatMessage.ContentPart.Type.IMAGE) {
                chars += 3000;
            }
        }
        return chars / CHARS_PER_TOKEN;
    }

    // Getters for testing
    int getMaxTokens() { return maxTokens; }
    int getMaxMessages() { return maxMessages; }
    boolean isKeepSystemMessage() { return keepSystemMessage; }
    int getRecentMessagesToKeep() { return recentMessagesToKeep; }
    boolean isRemoveOldScreenshots() { return removeOldScreenshots; }
}
