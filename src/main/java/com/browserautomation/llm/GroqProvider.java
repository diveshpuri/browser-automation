package com.browserautomation.llm;

import com.browserautomation.config.BrowserAutomationConfig;

/**
 * Groq LLM provider.
 * Uses the Groq API which is OpenAI-compatible.
 *
 * <p>Groq provides ultra-fast inference for open-source models including
 * LLaMA 3, Mixtral, and Gemma.</p>
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code GROQ_API_KEY} - Groq API key</li>
 * </ul>
 *
 * <p>Supported models:</p>
 * <ul>
 *   <li>llama-3.3-70b-versatile</li>
 *   <li>llama-3.1-8b-instant</li>
 *   <li>mixtral-8x7b-32768</li>
 *   <li>gemma2-9b-it</li>
 * </ul>
 */
public class GroqProvider extends OpenAiProvider {

    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";

    /**
     * Create a Groq provider with minimal configuration.
     *
     * @param apiKey the Groq API key
     * @param model  the model name (e.g., "llama-3.3-70b-versatile")
     */
    public GroqProvider(String apiKey, String model) {
        super(apiKey, GROQ_BASE_URL, model, 0.0, 4096);
    }

    /**
     * Create a Groq provider with custom configuration.
     *
     * @param apiKey      the Groq API key
     * @param model       the model name
     * @param temperature the sampling temperature
     * @param maxTokens   the maximum number of tokens to generate
     */
    public GroqProvider(String apiKey, String model, double temperature, int maxTokens) {
        super(apiKey, GROQ_BASE_URL, model, temperature, maxTokens);
    }

    /**
     * Create from environment configuration.
     */
    public static GroqProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new GroqProvider(config.getGroqApiKey(), "llama-3.3-70b-versatile");
    }

    @Override
    public String getProviderName() {
        return "groq";
    }

    @Override
    public boolean supportsVision() {
        // Groq supports vision for some models
        String model = getModelName();
        return model.contains("llama-3.2") && model.contains("vision");
    }
}
