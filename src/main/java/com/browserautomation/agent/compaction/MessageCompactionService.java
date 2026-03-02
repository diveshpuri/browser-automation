package com.browserautomation.agent.compaction;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM-driven message compaction service that summarizes old conversation history
 * to reduce token usage for long-running agent tasks.
 *
 * Triggers compaction every N steps or when token count exceeds threshold.
 */
public class MessageCompactionService {

    private static final Logger logger = LoggerFactory.getLogger(MessageCompactionService.class);

    private final LlmProvider llmProvider;
    private final CompactionConfig config;
    private int compactionCount;
    private int totalTokensSaved;

    public MessageCompactionService(LlmProvider llmProvider) {
        this(llmProvider, new CompactionConfig());
    }

    public MessageCompactionService(LlmProvider llmProvider, CompactionConfig config) {
        this.llmProvider = llmProvider;
        this.config = config;
    }

    /**
     * Check if compaction should be triggered based on message count or token estimate.
     */
    public boolean shouldCompact(List<ChatMessage> messages) {
        if (messages.size() < config.getMinMessagesBeforeCompaction()) {
            return false;
        }

        // Check if we have enough messages beyond keepLastN to warrant compaction
        if (messages.size() >= config.getMinMessagesBeforeCompaction()) {
            return true;
        }

        // Estimate tokens and check threshold
        int estimatedTokens = estimateTokenCount(messages);
        return estimatedTokens > config.getTokenThreshold();
    }

    /**
     * Compact old messages by summarizing them with the LLM.
     * Keeps the system prompt, the most recent keepLastN messages, and replaces
     * the middle with a summary.
     */
    public CompactionResult compact(List<ChatMessage> messages) {
        if (messages.size() <= config.getKeepLastN() + 1) {
            return new CompactionResult(messages, 0, false);
        }

        int originalCount = messages.size();
        int originalTokenEstimate = estimateTokenCount(messages);

        // Split messages: system prompt + old messages + recent messages
        ChatMessage systemPrompt = null;
        int startIdx = 0;
        if (!messages.isEmpty() && "system".equals(messages.get(0).getRoleString())) {
            systemPrompt = messages.get(0);
            startIdx = 1;
        }

        int keepFromEnd = Math.min(config.getKeepLastN(), messages.size() - startIdx);
        int endOfOldMessages = messages.size() - keepFromEnd;

        if (endOfOldMessages <= startIdx) {
            return new CompactionResult(messages, 0, false);
        }

        // Extract old messages to summarize
        List<ChatMessage> oldMessages = messages.subList(startIdx, endOfOldMessages);
        List<ChatMessage> recentMessages = messages.subList(endOfOldMessages, messages.size());

        // Build summary request
        String summaryContent = summarizeMessages(oldMessages);

        // Build compacted message list
        List<ChatMessage> compacted = new ArrayList<>();
        if (systemPrompt != null) {
            compacted.add(systemPrompt);
        }
        compacted.add(ChatMessage.system(
                "[Conversation Summary]\n" + summaryContent +
                        "\n[End Summary - " + oldMessages.size() + " messages compacted]"));
        compacted.addAll(recentMessages);

        int newTokenEstimate = estimateTokenCount(compacted);
        int tokensSaved = Math.max(0, originalTokenEstimate - newTokenEstimate);
        totalTokensSaved += tokensSaved;
        compactionCount++;

        logger.info("Message compaction: {} -> {} messages, ~{} tokens saved",
                originalCount, compacted.size(), tokensSaved);

        return new CompactionResult(compacted, tokensSaved, true);
    }

    private String summarizeMessages(List<ChatMessage> messages) {
        StringBuilder conversationText = new StringBuilder();
        for (ChatMessage msg : messages) {
            conversationText.append("[").append(msg.getRoleString()).append("]: ")
                    .append(msg.getText()).append("\n");
        }

        String prompt = "Summarize the following conversation history concisely. " +
                "Focus on key decisions, actions taken, results observed, and current state. " +
                "Be brief but preserve important context:\n\n" + conversationText;

        try {
            List<ChatMessage> summaryRequest = List.of(
                    ChatMessage.system("You are a conversation summarizer. Be concise."),
                    ChatMessage.user(prompt)
            );
            LlmResponse response = llmProvider.chatCompletion(summaryRequest);
            return response.getContent();
        } catch (Exception e) {
            logger.warn("LLM summarization failed, using simple truncation: {}", e.getMessage());
            return buildSimpleSummary(messages);
        }
    }

    private String buildSimpleSummary(List<ChatMessage> messages) {
        StringBuilder summary = new StringBuilder("Previous conversation (" + messages.size() + " messages):\n");
        int maxMessages = Math.min(5, messages.size());
        for (int i = 0; i < maxMessages; i++) {
            ChatMessage msg = messages.get(i);
            String content = msg.getText();
            if (content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            summary.append("- [").append(msg.getRoleString()).append("]: ").append(content).append("\n");
        }
        if (messages.size() > maxMessages) {
            summary.append("... and ").append(messages.size() - maxMessages).append(" more messages\n");
        }
        return summary.toString();
    }

    /**
     * Estimate token count for a list of messages (rough approximation: ~4 chars per token).
     */
    public int estimateTokenCount(List<ChatMessage> messages) {
        int totalChars = 0;
        for (ChatMessage msg : messages) {
            String text = msg.getText();
            totalChars += text != null ? text.length() : 0;
        }
        return totalChars / 4;
    }

    public int getCompactionCount() { return compactionCount; }
    public int getTotalTokensSaved() { return totalTokensSaved; }
    public CompactionConfig getConfig() { return config; }

    /**
     * Configuration for message compaction.
     */
    public static class CompactionConfig {
        private int compactionInterval = 10; // Compact every N messages
        private int tokenThreshold = 8000; // Compact when estimated tokens exceed this
        private int keepLastN = 5; // Keep the last N messages intact
        private int minMessagesBeforeCompaction = 10; // Don't compact until this many messages

        public CompactionConfig compactionInterval(int v) { compactionInterval = v; return this; }
        public CompactionConfig tokenThreshold(int v) { tokenThreshold = v; return this; }
        public CompactionConfig keepLastN(int v) { keepLastN = v; return this; }
        public CompactionConfig minMessagesBeforeCompaction(int v) { minMessagesBeforeCompaction = v; return this; }

        public int getCompactionInterval() { return compactionInterval; }
        public int getTokenThreshold() { return tokenThreshold; }
        public int getKeepLastN() { return keepLastN; }
        public int getMinMessagesBeforeCompaction() { return minMessagesBeforeCompaction; }
    }

    /**
     * Result of a compaction operation.
     */
    public record CompactionResult(List<ChatMessage> messages, int tokensSaved, boolean wasCompacted) {}
}
