package com.browserautomation.token;

import java.util.Map;

/**
 * Token pricing calculator for estimating costs.
 * Provides per-model pricing for input and output tokens.
 */
public class TokenPricing {

    /**
     * Pricing info for a model (per 1M tokens).
     */
    public static class ModelPricing {
        private final double inputPricePerMillion;
        private final double outputPricePerMillion;

        public ModelPricing(double inputPricePerMillion, double outputPricePerMillion) {
            this.inputPricePerMillion = inputPricePerMillion;
            this.outputPricePerMillion = outputPricePerMillion;
        }

        public double getInputPricePerMillion() { return inputPricePerMillion; }
        public double getOutputPricePerMillion() { return outputPricePerMillion; }

        /**
         * Calculate cost for given token usage.
         */
        public double calculateCost(int inputTokens, int outputTokens) {
            return (inputTokens * inputPricePerMillion / 1_000_000.0)
                    + (outputTokens * outputPricePerMillion / 1_000_000.0);
        }
    }

    /** Known model pricing (as of early 2025, prices may change) */
    private static final Map<String, ModelPricing> PRICING = Map.ofEntries(
            // OpenAI
            Map.entry("gpt-4o", new ModelPricing(2.50, 10.00)),
            Map.entry("gpt-4o-mini", new ModelPricing(0.15, 0.60)),
            Map.entry("gpt-4-turbo", new ModelPricing(10.00, 30.00)),

            // Anthropic
            Map.entry("claude-sonnet-4-20250514", new ModelPricing(3.00, 15.00)),
            Map.entry("claude-3-5-sonnet-20241022", new ModelPricing(3.00, 15.00)),
            Map.entry("claude-3-haiku-20240307", new ModelPricing(0.25, 1.25)),
            Map.entry("claude-opus-4-20250514", new ModelPricing(15.00, 75.00)),

            // Google Gemini
            Map.entry("gemini-2.0-flash-exp", new ModelPricing(0.10, 0.40)),
            Map.entry("gemini-1.5-pro", new ModelPricing(1.25, 5.00)),
            Map.entry("gemini-1.5-flash", new ModelPricing(0.075, 0.30)),

            // DeepSeek
            Map.entry("deepseek-chat", new ModelPricing(0.27, 1.10)),
            Map.entry("deepseek-reasoner", new ModelPricing(0.55, 2.19)),

            // Groq (pricing may vary)
            Map.entry("llama-3.3-70b-versatile", new ModelPricing(0.59, 0.79)),
            Map.entry("mixtral-8x7b-32768", new ModelPricing(0.24, 0.24)),

            // Mistral
            Map.entry("mistral-large-latest", new ModelPricing(2.00, 6.00)),
            Map.entry("mistral-small-latest", new ModelPricing(0.20, 0.60))
    );

    private TokenPricing() {
        // Utility class
    }

    /**
     * Get pricing for a specific model.
     *
     * @param model the model name
     * @return the pricing info, or null if unknown
     */
    public static ModelPricing getPricing(String model) {
        if (model == null) return null;

        // Try exact match first
        ModelPricing pricing = PRICING.get(model);
        if (pricing != null) return pricing;

        // Try prefix matching
        for (Map.Entry<String, ModelPricing> entry : PRICING.entrySet()) {
            if (model.startsWith(entry.getKey()) || entry.getKey().startsWith(model)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Calculate the cost for a given model and token usage.
     *
     * @param model        the model name
     * @param inputTokens  number of input (prompt) tokens
     * @param outputTokens number of output (completion) tokens
     * @return the estimated cost in USD, or -1 if pricing is unknown
     */
    public static double calculateCost(String model, int inputTokens, int outputTokens) {
        ModelPricing pricing = getPricing(model);
        if (pricing == null) return -1;
        return pricing.calculateCost(inputTokens, outputTokens);
    }

    /**
     * Format a cost value as a human-readable string.
     */
    public static String formatCost(double cost) {
        if (cost < 0) return "unknown";
        if (cost < 0.01) return String.format("$%.4f", cost);
        return String.format("$%.2f", cost);
    }
}
