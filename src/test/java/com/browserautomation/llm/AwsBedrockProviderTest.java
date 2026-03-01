package com.browserautomation.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AwsBedrockProvider.
 */
class AwsBedrockProviderTest {

    @Test
    void testProviderName() {
        AwsBedrockProvider provider = new AwsBedrockProvider(
                "access-key", "secret-key", "us-east-1", "anthropic.claude-3-5-sonnet-20241022-v2:0");
        assertEquals("aws-bedrock", provider.getProviderName());
    }

    @Test
    void testModelName() {
        AwsBedrockProvider provider = new AwsBedrockProvider(
                "access-key", "secret-key", "us-east-1", "anthropic.claude-3-5-sonnet-20241022-v2:0");
        assertEquals("anthropic.claude-3-5-sonnet-20241022-v2:0", provider.getModelName());
    }

    @Test
    void testVisionSupportClaude3() {
        AwsBedrockProvider provider = new AwsBedrockProvider(
                "access-key", "secret-key", "us-east-1", "anthropic.claude-3-5-sonnet-20241022-v2:0");
        assertTrue(provider.supportsVision());
    }

    @Test
    void testVisionSupportNonVisionModel() {
        AwsBedrockProvider provider = new AwsBedrockProvider(
                "access-key", "secret-key", "us-east-1", "amazon.titan-text-express-v1");
        assertFalse(provider.supportsVision());
    }

    @Test
    void testCustomConfig() {
        AwsBedrockProvider provider = new AwsBedrockProvider(
                "key", "secret", "eu-west-1", "meta.llama3-1-70b-instruct-v1:0", 0.5, 2048);
        assertEquals("meta.llama3-1-70b-instruct-v1:0", provider.getModelName());
        assertEquals("aws-bedrock", provider.getProviderName());
    }
}
