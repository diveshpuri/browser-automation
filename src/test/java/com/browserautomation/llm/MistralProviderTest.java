package com.browserautomation.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MistralProvider.
 */
class MistralProviderTest {

    @Test
    void testProviderName() {
        MistralProvider provider = new MistralProvider("test-key", "mistral-large-latest");
        assertEquals("mistral", provider.getProviderName());
    }

    @Test
    void testModelName() {
        MistralProvider provider = new MistralProvider("test-key", "mistral-large-latest");
        assertEquals("mistral-large-latest", provider.getModelName());
    }

    @Test
    void testVisionSupport() {
        MistralProvider provider = new MistralProvider("test-key", "mistral-large-latest");
        assertFalse(provider.supportsVision());

        MistralProvider visionProvider = new MistralProvider("test-key", "pixtral-large-latest");
        assertTrue(visionProvider.supportsVision());
    }

    @Test
    void testCustomConfig() {
        MistralProvider provider = new MistralProvider("test-key", "mistral-small-latest", 0.7, 1024);
        assertEquals("mistral-small-latest", provider.getModelName());
        assertEquals("mistral", provider.getProviderName());
    }
}
