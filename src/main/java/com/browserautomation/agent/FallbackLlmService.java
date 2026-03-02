package com.browserautomation.agent;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback LLM service that automatically switches to a backup LLM provider
 * on errors or rate limits. Supports multiple fallback providers in priority order.
 *
 */
public class FallbackLlmService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackLlmService.class);

    private final LlmProvider primaryProvider;
    private final List<LlmProvider> fallbackProviders;
    private final FallbackConfig config;
    private int primaryFailures;
    private int fallbacksUsed;
    private volatile boolean usingFallback;
    private volatile LlmProvider currentProvider;

    public FallbackLlmService(LlmProvider primaryProvider, List<LlmProvider> fallbackProviders) {
        this(primaryProvider, fallbackProviders, new FallbackConfig());
    }

    public FallbackLlmService(LlmProvider primaryProvider, List<LlmProvider> fallbackProviders,
                               FallbackConfig config) {
        this.primaryProvider = primaryProvider;
        this.fallbackProviders = new ArrayList<>(fallbackProviders);
        this.config = config;
        this.currentProvider = primaryProvider;
    }

    /**
     * Send a chat request, automatically falling back to backup providers on failure.
     */
    public LlmResponse chat(List<ChatMessage> messages, boolean includeVision) {
        // Try primary provider first
        try {
            LlmResponse response = primaryProvider.chatCompletion(messages);
            primaryFailures = 0;
            if (usingFallback) {
                logger.info("Primary LLM provider recovered, switching back");
                usingFallback = false;
                currentProvider = primaryProvider;
            }
            return response;
        } catch (Exception primaryError) {
            primaryFailures++;
            logger.warn("Primary LLM provider failed (attempt {}): {}",
                    primaryFailures, primaryError.getMessage());

            if (isRateLimitError(primaryError) || primaryFailures >= config.getMaxPrimaryFailures()) {
                return tryFallbackProviders(messages, primaryError);
            }

            throw new RuntimeException("Primary LLM provider error: " + primaryError.getMessage(), primaryError);
        }
    }

    private LlmResponse tryFallbackProviders(List<ChatMessage> messages,
                                              Exception originalError) {
        for (int i = 0; i < fallbackProviders.size(); i++) {
            LlmProvider fallback = fallbackProviders.get(i);
            try {
                logger.info("Trying fallback LLM provider #{}", i + 1);
                LlmResponse response = fallback.chatCompletion(messages);
                usingFallback = true;
                currentProvider = fallback;
                fallbacksUsed++;
                logger.info("Fallback LLM provider #{} succeeded", i + 1);
                return response;
            } catch (Exception fallbackError) {
                logger.warn("Fallback LLM provider #{} also failed: {}",
                        i + 1, fallbackError.getMessage());
            }
        }

        throw new RuntimeException("All LLM providers failed. Primary error: " +
                originalError.getMessage(), originalError);
    }

    private boolean isRateLimitError(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        return message.contains("429") || message.contains("rate limit") ||
                message.contains("Rate limit") || message.contains("quota") ||
                message.contains("too many requests");
    }

    /**
     * Reset failure counters and switch back to primary.
     */
    public void reset() {
        primaryFailures = 0;
        usingFallback = false;
        currentProvider = primaryProvider;
    }

    public LlmProvider getCurrentProvider() { return currentProvider; }
    public boolean isUsingFallback() { return usingFallback; }
    public int getPrimaryFailures() { return primaryFailures; }
    public int getFallbacksUsed() { return fallbacksUsed; }

    /**
     * Configuration for fallback behavior.
     */
    public static class FallbackConfig {
        private int maxPrimaryFailures = 3;
        private long cooldownMs = 60000;

        public FallbackConfig maxPrimaryFailures(int v) { maxPrimaryFailures = v; return this; }
        public FallbackConfig cooldownMs(long v) { cooldownMs = v; return this; }

        public int getMaxPrimaryFailures() { return maxPrimaryFailures; }
        public long getCooldownMs() { return cooldownMs; }
    }
}
