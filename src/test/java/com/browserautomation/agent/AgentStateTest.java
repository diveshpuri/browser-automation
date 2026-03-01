package com.browserautomation.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentStateTest {

    @Test
    void testInitialState() {
        AgentState state = new AgentState();
        assertEquals(0, state.getCurrentStep());
        assertEquals(0, state.getConsecutiveFailures());
        assertEquals(0, state.getTotalTokensUsed());
        assertFalse(state.isCompleted());
        assertFalse(state.isFailed());
        assertNull(state.getFinalResult());
        assertTrue(state.getHistory().isEmpty());
    }

    @Test
    void testRecordStep() {
        AgentState state = new AgentState();
        AgentState.AgentStep step = new AgentState.AgentStep(
                1, "thinking", "navigate", "navigated", null, 100, 500);

        state.recordStep(step);
        assertEquals(1, state.getCurrentStep());
        assertEquals(0, state.getConsecutiveFailures());
        assertEquals(100, state.getTotalTokensUsed());
        assertEquals(1, state.getHistory().size());
    }

    @Test
    void testConsecutiveFailures() {
        AgentState state = new AgentState();

        // Record a failure
        state.recordStep(new AgentState.AgentStep(1, null, "click", null, "error", 50, 100));
        assertEquals(1, state.getConsecutiveFailures());

        // Record another failure
        state.recordStep(new AgentState.AgentStep(2, null, "click", null, "error2", 50, 100));
        assertEquals(2, state.getConsecutiveFailures());

        // Record a success - resets consecutive failures
        state.recordStep(new AgentState.AgentStep(3, null, "navigate", "ok", null, 50, 100));
        assertEquals(0, state.getConsecutiveFailures());
    }

    @Test
    void testMarkCompleted() {
        AgentState state = new AgentState();
        state.markCompleted("done");
        assertTrue(state.isCompleted());
        assertEquals("done", state.getFinalResult());
    }

    @Test
    void testMarkFailed() {
        AgentState state = new AgentState();
        state.markFailed("too many failures");
        assertTrue(state.isFailed());
        assertEquals("too many failures", state.getFinalResult());
    }

    @Test
    void testGetHistorySummary() {
        AgentState state = new AgentState();
        state.recordStep(new AgentState.AgentStep(1, null, "navigate", "navigated", null, 100, 500));
        state.recordStep(new AgentState.AgentStep(2, null, "click", null, "element not found", 80, 300));

        String summary = state.getHistorySummary(10);
        assertTrue(summary.contains("navigate"));
        assertTrue(summary.contains("click"));
        assertTrue(summary.contains("ERROR"));
    }

    @Test
    void testAgentStepSummary() {
        AgentState.AgentStep successStep = new AgentState.AgentStep(
                1, null, "navigate", "ok", null, 100, 500);
        assertFalse(successStep.hasError());
        assertTrue(successStep.getSummary().contains("navigate"));

        AgentState.AgentStep failureStep = new AgentState.AgentStep(
                2, null, "click", null, "not found", 100, 500);
        assertTrue(failureStep.hasError());
        assertTrue(failureStep.getSummary().contains("ERROR"));
    }
}
