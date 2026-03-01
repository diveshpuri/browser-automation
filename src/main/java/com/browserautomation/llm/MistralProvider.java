package com.browserautomation.llm;

import com.browserautomation.config.BrowserAutomationConfig;

/**
 * Mistral AI LLM provider.
 * Uses the Mistral API which is OpenAI-compatible.
 *
 * <p>Supports Mistral's models including Mistral Large, Medium, and Small.</p>
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code MISTRAL_API_KEY} - Mistral API key</li>
 * </ul>
 *
 * <p>Supported models:</p>
 * <ul>
 *   <li>mistral-large-latest</li>
 *   <li>mistral-medium-latest</li>
 *   <li>mistral-small-latest</li>
 *   <li>open-mixtral-8x22b</li>
 *   <li>pixtral-large-latest (vision)</li>
 * </ul>
 */
public class MistralProvider extends OpenAiProvider {

    private static final String MISTRAL_BASE_URL = "https://api.mistral.ai/v1";

    /**
     * Create a Mistral provider with minimal configuration.
     *
     * @param apiKey the Mistral API key
     * @param model  the model name (e.g., "mistral-large-latest")
     */
    public MistralProvider(String apiKey, String model) {
        super(apiKey, MISTRAL_BASE_URL, model, 0.0, 4096);
    }

    /**
     * Create a Mistral provider with custom configuration.
     *
     * @param apiKey      the Mistral API key
     * @param model       the model name
     * @param temperature the sampling temperature
     * @param maxTokens   the maximum number of tokens to generate
     */
    public MistralProvider(String apiKey, String model, double temperature, int maxTokens) {
        super(apiKey, MISTRAL_BASE_URL, model, temperature, maxTokens);
    }

    /**
     * Create from environment configuration.
     */
    public static MistralProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new MistralProvider(config.getMistralApiKey(), "mistral-large-latest");
    }

    @Override
    public String getProviderName() {
        return "mistral";
    }

    @Override
    public boolean supportsVision() {
        // Pixtral models support vision
        String model = getModelName();
        return model.contains("pixtral");
    }
}
