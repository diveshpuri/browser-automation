package com.browserautomation.token;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the TokenUsageTracker.
 */
class TokenUsageTrackerTest {

    @Test
    void testInitialState() {
        TokenUsageTracker tracker = new TokenUsageTracker("gpt-4o");
        assertEquals(0, tracker.getTotalInputTokens());
        assertEquals(0, tracker.getTotalOutputTokens());
        assertEquals(0, tracker.getTotalTokens());
        assertEquals(0, tracker.getStepCount());
    }

    @Test
    void testRecordStep() {
        TokenUsageTracker tracker = new TokenUsageTracker("gpt-4o");
        tracker.recordStep(1, 100, 50);

        assertEquals(100, tracker.getTotalInputTokens());
        assertEquals(50, tracker.getTotalOutputTokens());
        assertEquals(150, tracker.getTotalTokens());
        assertEquals(1, tracker.getStepCount());
    }

    @Test
    void testRecordMultipleSteps() {
        TokenUsageTracker tracker = new TokenUsageTracker("gpt-4o");
        tracker.recordStep(1, 100, 50);
        tracker.recordStep(2, 200, 75);
        tracker.recordStep(3, 150, 100);

        assertEquals(450, tracker.getTotalInputTokens());
        assertEquals(225, tracker.getTotalOutputTokens());
        assertEquals(675, tracker.getTotalTokens());
        assertEquals(3, tracker.getStepCount());
    }

    @Test
    void testGetStepUsages() {
        TokenUsageTracker tracker = new TokenUsageTracker("gpt-4o");
        tracker.recordStep(1, 100, 50);
        tracker.recordStep(2, 200, 75);

        assertEquals(2, tracker.getStepUsages().size());
        assertEquals(1, tracker.getStepUsages().get(0).getStepNumber());
        assertEquals(100, tracker.getStepUsages().get(0).getInputTokens());
        assertEquals(50, tracker.getStepUsages().get(0).getOutputTokens());
        assertEquals(150, tracker.getStepUsages().get(0).getTotalTokens());
    }

    @Test
    void testGetTotalCost() {
        TokenUsageTracker tracker = new TokenUsageTracker("gpt-4o");
        tracker.recordStep(1, 1000, 500);

        double cost = tracker.getTotalCost();
        assertTrue(cost > 0);
    }

    @Test
    void testGetSummary() {
        TokenUsageTracker tracker = new TokenUsageTracker("gpt-4o");
        tracker.recordStep(1, 100, 50);

        String summary = tracker.getSummary();
        assertTrue(summary.contains("gpt-4o"));
        assertTrue(summary.contains("100"));
        assertTrue(summary.contains("50"));
        assertTrue(summary.contains("Step 1"));
    }
}
