package com.browserautomation.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AzureOpenAiProviderTest {

    @Test
    void testConstructorMinimal() {
        AzureOpenAiProvider provider = new AzureOpenAiProvider(
                "test-key", "https://myresource.openai.azure.com", "gpt-4o");

        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("gpt-4o", provider.getModelName());
    }

    @Test
    void testConstructorFull() {
        AzureOpenAiProvider provider = new AzureOpenAiProvider(
                "test-key", "https://myresource.openai.azure.com/", "gpt-4o",
                "2024-10-21", 0.5, 8192);

        assertEquals("azure-openai", provider.getProviderName());
        assertEquals("gpt-4o", provider.getModelName());
    }

    @Test
    void testSupportsVision() {
        AzureOpenAiProvider gpt4o = new AzureOpenAiProvider(
                "key", "https://endpoint.openai.azure.com", "gpt-4o");
        assertTrue(gpt4o.supportsVision());

        AzureOpenAiProvider gpt4 = new AzureOpenAiProvider(
                "key", "https://endpoint.openai.azure.com", "gpt-4");
        assertTrue(gpt4.supportsVision());

        AzureOpenAiProvider gpt35 = new AzureOpenAiProvider(
                "key", "https://endpoint.openai.azure.com", "gpt-35-turbo");
        assertFalse(gpt35.supportsVision());
    }

    @Test
    void testEndpointTrailingSlashRemoved() {
        AzureOpenAiProvider provider = new AzureOpenAiProvider(
                "key", "https://myresource.openai.azure.com/", "gpt-4o");
        assertEquals("gpt-4o", provider.getModelName());
    }
}
