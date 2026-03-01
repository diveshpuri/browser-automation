package com.browserautomation.llm;

import com.browserautomation.config.BrowserAutomationConfig;

/**
 * DeepSeek LLM provider.
 * Uses the DeepSeek API which is OpenAI-compatible.
 *
 * <p>Supports DeepSeek-V3 (deepseek-chat) and DeepSeek-R1 (deepseek-reasoner).</p>
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code DEEPSEEK_API_KEY} - DeepSeek API key</li>
 * </ul>
 *
 * <p>Note: DeepSeek models do not support vision. Set {@code useVision(false)} in AgentConfig.</p>
 */
public class DeepSeekProvider extends OpenAiProvider {

    private static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1";

    /**
     * Create a DeepSeek provider with minimal configuration.
     *
     * @param apiKey the DeepSeek API key
     * @param model  the model name (e.g., "deepseek-chat" for V3, "deepseek-reasoner" for R1)
     */
    public DeepSeekProvider(String apiKey, String model) {
        super(apiKey, DEEPSEEK_BASE_URL, model, 0.0, 4096);
    }

    /**
     * Create a DeepSeek provider with custom configuration.
     *
     * @param apiKey      the DeepSeek API key
     * @param model       the model name
     * @param temperature the sampling temperature
     * @param maxTokens   the maximum number of tokens to generate
     */
    public DeepSeekProvider(String apiKey, String model, double temperature, int maxTokens) {
        super(apiKey, DEEPSEEK_BASE_URL, model, temperature, maxTokens);
    }

    /**
     * Create from environment configuration.
     */
    public static DeepSeekProvider fromConfig() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        return new DeepSeekProvider(
                config.getDeepSeekApiKey(),
                config.getDefaultModel()
        );
    }

    @Override
    public String getProviderName() {
        return "deepseek";
    }

    @Override
    public boolean supportsVision() {
        // DeepSeek models currently do not support vision
        return false;
    }
}
