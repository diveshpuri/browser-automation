package com.browserautomation.token;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the TokenPricing.
 */
class TokenPricingTest {

    @Test
    void testGetPricingKnownModel() {
        TokenPricing.ModelPricing pricing = TokenPricing.getPricing("gpt-4o");
        assertNotNull(pricing);
        assertTrue(pricing.getInputPricePerMillion() > 0);
        assertTrue(pricing.getOutputPricePerMillion() > 0);
    }

    @Test
    void testGetPricingUnknownModel() {
        assertNull(TokenPricing.getPricing("totally-unknown-model-xyz"));
    }

    @Test
    void testGetPricingNull() {
        assertNull(TokenPricing.getPricing(null));
    }

    @Test
    void testCalculateCost() {
        double cost = TokenPricing.calculateCost("gpt-4o", 1000, 500);
        assertTrue(cost > 0);
    }

    @Test
    void testCalculateCostUnknownModel() {
        double cost = TokenPricing.calculateCost("unknown-model", 1000, 500);
        assertEquals(-1, cost);
    }

    @Test
    void testModelPricingCalculation() {
        TokenPricing.ModelPricing pricing = new TokenPricing.ModelPricing(2.50, 10.00);
        // 1M input tokens at $2.50 + 1M output tokens at $10.00 = $12.50
        double cost = pricing.calculateCost(1_000_000, 1_000_000);
        assertEquals(12.50, cost, 0.01);
    }

    @Test
    void testFormatCostSmall() {
        String formatted = TokenPricing.formatCost(0.001);
        assertTrue(formatted.startsWith("$"));
        assertTrue(formatted.contains("0.0010"));
    }

    @Test
    void testFormatCostLarge() {
        String formatted = TokenPricing.formatCost(5.50);
        assertEquals("$5.50", formatted);
    }

    @Test
    void testFormatCostUnknown() {
        assertEquals("unknown", TokenPricing.formatCost(-1));
    }

    @Test
    void testMultipleModelsHavePricing() {
        assertNotNull(TokenPricing.getPricing("gpt-4o"));
        assertNotNull(TokenPricing.getPricing("claude-sonnet-4-20250514"));
        assertNotNull(TokenPricing.getPricing("gemini-2.0-flash-exp"));
        assertNotNull(TokenPricing.getPricing("deepseek-chat"));
        assertNotNull(TokenPricing.getPricing("llama-3.3-70b-versatile"));
        assertNotNull(TokenPricing.getPricing("mistral-large-latest"));
    }
}
