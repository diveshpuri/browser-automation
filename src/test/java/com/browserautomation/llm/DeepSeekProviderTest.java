package com.browserautomation.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekProviderTest {

    @Test
    void testConstructorMinimal() {
        DeepSeekProvider provider = new DeepSeekProvider("test-key", "deepseek-chat");

        assertEquals("deepseek", provider.getProviderName());
        assertEquals("deepseek-chat", provider.getModelName());
    }

    @Test
    void testConstructorWithConfig() {
        DeepSeekProvider provider = new DeepSeekProvider("test-key", "deepseek-reasoner", 0.5, 8192);

        assertEquals("deepseek", provider.getProviderName());
        assertEquals("deepseek-reasoner", provider.getModelName());
    }

    @Test
    void testDoesNotSupportVision() {
        DeepSeekProvider v3 = new DeepSeekProvider("key", "deepseek-chat");
        assertFalse(v3.supportsVision());

        DeepSeekProvider r1 = new DeepSeekProvider("key", "deepseek-reasoner");
        assertFalse(r1.supportsVision());
    }

    @Test
    void testExtendsOpenAiProvider() {
        DeepSeekProvider provider = new DeepSeekProvider("key", "deepseek-chat");
        assertTrue(provider instanceof OpenAiProvider);
        assertTrue(provider instanceof LlmProvider);
    }
}
