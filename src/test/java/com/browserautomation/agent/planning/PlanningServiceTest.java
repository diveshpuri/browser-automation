package com.browserautomation.agent.planning;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class PlanningServiceTest {

    private LlmProvider mockProvider;
    private PlanningService planningService;

    @BeforeEach
    void setUp() {
        mockProvider = mock(LlmProvider.class);
        planningService = new PlanningService(mockProvider, 3, 5);
    }

    @Test
    void testCreatePlan() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse(
                        "1. Navigate to the website\n2. Click the login button\n3. Enter credentials",
                        null, 50, 30));

        List<PlanningService.PlanStep> plan = planningService.createPlan("Login to the website");

        assertEquals(3, plan.size());
        assertEquals("Navigate to the website", plan.get(0).getDescription());
        assertEquals("Click the login button", plan.get(1).getDescription());
        assertEquals("Enter credentials", plan.get(2).getDescription());
        assertEquals(0, planningService.getCurrentStepIndex());
    }

    @Test
    void testRecordStepOutcomeSuccess() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("1. Step one\n2. Step two", null, 50, 30));

        planningService.createPlan("Test task");
        planningService.recordStepOutcome(true, "Completed");

        assertEquals(1, planningService.getCurrentStepIndex());
        assertEquals(0, planningService.getConsecutiveFailures());
    }

    @Test
    void testRecordStepOutcomeFailure() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("1. Step one\n2. Step two", null, 50, 30));

        planningService.createPlan("Test task");
        planningService.recordStepOutcome(false, "Element not found");

        assertEquals(0, planningService.getCurrentStepIndex());
        assertEquals(1, planningService.getConsecutiveFailures());
    }

    @Test
    void testShouldReplan() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("1. Step one\n2. Step two", null, 50, 30));

        planningService.createPlan("Test task");

        assertFalse(planningService.shouldReplan());

        // Record 3 consecutive failures
        planningService.recordStepOutcome(false, "fail 1");
        planningService.recordStepOutcome(false, "fail 2");
        assertFalse(planningService.shouldReplan());

        planningService.recordStepOutcome(false, "fail 3");
        assertTrue(planningService.shouldReplan());
    }

    @Test
    void testReplan() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("1. Step one\n2. Step two", null, 50, 30))
                .thenReturn(new LlmResponse("1. Try alternative approach\n2. Verify result", null, 50, 30));

        planningService.createPlan("Test task");

        // Fail 3 times to trigger replan
        planningService.recordStepOutcome(false, "fail");
        planningService.recordStepOutcome(false, "fail");
        planningService.recordStepOutcome(false, "fail");

        List<PlanningService.PlanStep> newPlan = planningService.replan("Test task", "Current state");

        assertEquals(1, planningService.getReplanCount());
        assertFalse(newPlan.isEmpty());
        assertEquals(0, planningService.getConsecutiveFailures());
    }

    @Test
    void testGetExplorationNudge() {
        String nudge = planningService.getExplorationNudge();
        assertNotNull(nudge);
        assertFalse(nudge.isEmpty());
    }

    @Test
    void testIsPlanComplete() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("1. Single step", null, 50, 30));

        planningService.createPlan("Test task");
        assertFalse(planningService.isPlanComplete());

        planningService.recordStepOutcome(true, "done");
        assertTrue(planningService.isPlanComplete());
    }

    @Test
    void testGetCurrentStepDescription() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse("1. First step\n2. Second step", null, 50, 30));

        planningService.createPlan("Test task");
        assertEquals("First step", planningService.getCurrentStepDescription());

        planningService.recordStepOutcome(true, "done");
        assertEquals("Second step", planningService.getCurrentStepDescription());

        planningService.recordStepOutcome(true, "done");
        assertNull(planningService.getCurrentStepDescription());
    }

    @Test
    void testPlanStepToString() {
        PlanningService.PlanStep step = new PlanningService.PlanStep(1, "Click button");
        assertEquals("Step 1: Click button [PENDING]", step.toString());

        step.markCompleted(true, "done");
        assertEquals("Step 1: Click button [SUCCESS]", step.toString());
    }

    @Test
    void testCreatePlanWithLlmFailure() {
        when(mockProvider.chatCompletion(anyList()))
                .thenThrow(new RuntimeException("LLM error"));

        List<PlanningService.PlanStep> plan = planningService.createPlan("Test task");
        assertTrue(plan.isEmpty());
    }

    @Test
    void testParsePlanWithDifferentFormats() {
        when(mockProvider.chatCompletion(anyList()))
                .thenReturn(new LlmResponse(
                        "1) Navigate to page\n2) Click button\n3) Verify result",
                        null, 50, 30));

        List<PlanningService.PlanStep> plan = planningService.createPlan("Test task");
        assertEquals(3, plan.size());
    }
}
