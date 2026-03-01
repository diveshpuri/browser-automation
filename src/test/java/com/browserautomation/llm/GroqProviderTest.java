package com.browserautomation.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the GroqProvider.
 */
class GroqProviderTest {

    @Test
    void testProviderName() {
        GroqProvider provider = new GroqProvider("test-key", "llama-3.3-70b-versatile");
        assertEquals("groq", provider.getProviderName());
    }

    @Test
    void testModelName() {
        GroqProvider provider = new GroqProvider("test-key", "llama-3.3-70b-versatile");
        assertEquals("llama-3.3-70b-versatile", provider.getModelName());
    }

    @Test
    void testVisionSupport() {
        GroqProvider provider = new GroqProvider("test-key", "llama-3.3-70b-versatile");
        assertFalse(provider.supportsVision());

        GroqProvider visionProvider = new GroqProvider("test-key", "llama-3.2-11b-vision-preview");
        assertTrue(visionProvider.supportsVision());
    }

    @Test
    void testCustomConfig() {
        GroqProvider provider = new GroqProvider("test-key", "mixtral-8x7b-32768", 0.5, 2048);
        assertEquals("mixtral-8x7b-32768", provider.getModelName());
        assertEquals("groq", provider.getProviderName());
    }
}
