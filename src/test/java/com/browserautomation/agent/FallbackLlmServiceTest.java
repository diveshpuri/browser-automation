package com.browserautomation.agent;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FallbackLlmServiceTest {

    private LlmProvider primaryProvider;
    private LlmProvider fallbackProvider1;
    private LlmProvider fallbackProvider2;
    private FallbackLlmService service;
    private List<ChatMessage> testMessages;

    @BeforeEach
    void setUp() {
        primaryProvider = mock(LlmProvider.class);
        fallbackProvider1 = mock(LlmProvider.class);
        fallbackProvider2 = mock(LlmProvider.class);
        service = new FallbackLlmService(primaryProvider, List.of(fallbackProvider1, fallbackProvider2));
        testMessages = List.of(ChatMessage.user("Hello"));
    }

    @Test
    void testUsesPrimaryProviderWhenSuccessful() {
        when(primaryProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Primary response", null, 30, 50));

        LlmResponse response = service.chat(testMessages, false);
        assertEquals("Primary response", response.getContent());
        assertFalse(service.isUsingFallback());
        verify(fallbackProvider1, never()).chatCompletion(anyList());
    }

    @Test
    void testFallsBackOnRateLimitError() {
        when(primaryProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("429 rate limit exceeded"));
        when(fallbackProvider1.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Fallback response", null, 20, 40));

        LlmResponse response = service.chat(testMessages, false);
        assertEquals("Fallback response", response.getContent());
        assertTrue(service.isUsingFallback());
        assertEquals(1, service.getFallbacksUsed());
    }

    @Test
    void testFallsBackAfterMaxFailures() {
        FallbackLlmService customService = new FallbackLlmService(
                primaryProvider, List.of(fallbackProvider1),
                new FallbackLlmService.FallbackConfig().maxPrimaryFailures(2));

        when(primaryProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("Some error"));
        when(fallbackProvider1.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Fallback response", null, 20, 40));

        // First failure - should throw since below maxPrimaryFailures
        assertThrows(RuntimeException.class, () -> customService.chat(testMessages, false));

        // Second failure - should now fallback
        LlmResponse response = customService.chat(testMessages, false);
        assertEquals("Fallback response", response.getContent());
    }

    @Test
    void testTriesMultipleFallbacks() {
        when(primaryProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("429 rate limit"));
        when(fallbackProvider1.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("Fallback 1 also failed"));
        when(fallbackProvider2.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Fallback 2 response", null, 15, 30));

        LlmResponse response = service.chat(testMessages, false);
        assertEquals("Fallback 2 response", response.getContent());
    }

    @Test
    void testThrowsWhenAllProvidersFail() {
        when(primaryProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("429 rate limit"));
        when(fallbackProvider1.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("Fallback 1 failed"));
        when(fallbackProvider2.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("Fallback 2 failed"));

        assertThrows(RuntimeException.class, () -> service.chat(testMessages, false));
    }

    @Test
    void testRecoversToPrimaryAfterFallback() {
        // First call: primary fails, falls back
        when(primaryProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("429 rate limit"))
                .thenReturn(new LlmResponse("Primary recovered", null, 30, 50));
        when(fallbackProvider1.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Fallback response", null, 20, 40));

        service.chat(testMessages, false);
        assertTrue(service.isUsingFallback());

        // Second call: primary succeeds
        LlmResponse response = service.chat(testMessages, false);
        assertEquals("Primary recovered", response.getContent());
        assertFalse(service.isUsingFallback());
    }

    @Test
    void testReset() {
        service = new FallbackLlmService(primaryProvider, List.of(fallbackProvider1));
        when(primaryProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("429 rate limit"));
        when(fallbackProvider1.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("Fallback", null, 20, 40));

        service.chat(testMessages, false);
        assertTrue(service.isUsingFallback());

        service.reset();
        assertFalse(service.isUsingFallback());
        assertEquals(0, service.getPrimaryFailures());
    }

    @Test
    void testGetCurrentProvider() {
        assertEquals(primaryProvider, service.getCurrentProvider());
    }

    @Test
    void testFallbackConfig() {
        FallbackLlmService.FallbackConfig config = new FallbackLlmService.FallbackConfig()
                .maxPrimaryFailures(5)
                .cooldownMs(120000);
        assertEquals(5, config.getMaxPrimaryFailures());
        assertEquals(120000, config.getCooldownMs());
    }
}
