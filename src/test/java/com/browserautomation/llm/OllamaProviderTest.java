package com.browserautomation.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaProviderTest {

    @Test
    void testConstructorMinimal() {
        OllamaProvider provider = new OllamaProvider("qwen2.5");

        assertEquals("ollama", provider.getProviderName());
        assertEquals("qwen2.5", provider.getModelName());
    }

    @Test
    void testConstructorWithBaseUrl() {
        OllamaProvider provider = new OllamaProvider("http://my-server:11434", "llama3.1");

        assertEquals("ollama", provider.getProviderName());
        assertEquals("llama3.1", provider.getModelName());
    }

    @Test
    void testConstructorFull() {
        OllamaProvider provider = new OllamaProvider(
                "http://localhost:11434", "mistral", 0.7, 16000);

        assertEquals("ollama", provider.getProviderName());
        assertEquals("mistral", provider.getModelName());
    }

    @Test
    void testSupportsVision() {
        OllamaProvider llava = new OllamaProvider("llava");
        assertTrue(llava.supportsVision());

        OllamaProvider bakllava = new OllamaProvider("bakllava");
        assertTrue(bakllava.supportsVision());

        OllamaProvider moondream = new OllamaProvider("moondream");
        assertTrue(moondream.supportsVision());

        OllamaProvider qwen = new OllamaProvider("qwen2.5");
        assertFalse(qwen.supportsVision());

        OllamaProvider llama = new OllamaProvider("llama3.1");
        assertFalse(llama.supportsVision());
    }

    @Test
    void testBaseUrlTrailingSlashRemoved() {
        OllamaProvider provider = new OllamaProvider("http://localhost:11434/", "qwen2.5");
        assertEquals("qwen2.5", provider.getModelName());
    }
}
