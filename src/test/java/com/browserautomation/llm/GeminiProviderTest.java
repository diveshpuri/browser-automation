package com.browserautomation.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiProviderTest {

    @Test
    void testConstructorMinimal() {
        GeminiProvider provider = new GeminiProvider("test-key", "gemini-2.0-flash-exp");

        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-2.0-flash-exp", provider.getModelName());
    }

    @Test
    void testConstructorFull() {
        GeminiProvider provider = new GeminiProvider("test-key", "gemini-1.5-pro", 0.5, 8192);

        assertEquals("gemini", provider.getProviderName());
        assertEquals("gemini-1.5-pro", provider.getModelName());
    }

    @Test
    void testSupportsVision() {
        GeminiProvider flash = new GeminiProvider("key", "gemini-2.0-flash-exp");
        assertTrue(flash.supportsVision());

        GeminiProvider pro15 = new GeminiProvider("key", "gemini-1.5-pro");
        assertTrue(pro15.supportsVision());

        GeminiProvider proVision = new GeminiProvider("key", "gemini-pro-vision");
        assertTrue(proVision.supportsVision());

        GeminiProvider pro = new GeminiProvider("key", "gemini-pro");
        assertFalse(pro.supportsVision());
    }
}
