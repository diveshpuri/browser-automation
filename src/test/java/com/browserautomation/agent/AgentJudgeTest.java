package com.browserautomation.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentJudge.
 */
class AgentJudgeTest {

    @Test
    void testEvaluationScoreClamping() {
        AgentJudge.Evaluation eval = new AgentJudge.Evaluation(1.5, true, "test", List.of());
        assertEquals(1.0, eval.getScore(), 0.001);

        AgentJudge.Evaluation eval2 = new AgentJudge.Evaluation(-0.5, false, "test", List.of());
        assertEquals(0.0, eval2.getScore(), 0.001);
    }

    @Test
    void testEvaluationProperties() {
        AgentJudge.Evaluation eval = new AgentJudge.Evaluation(0.8, true, "good job", List.of("minor issue"));
        assertEquals(0.8, eval.getScore(), 0.001);
        assertTrue(eval.isTaskCompleted());
        assertEquals("good job", eval.getFeedback());
        assertEquals(1, eval.getIssues().size());
        assertEquals("minor issue", eval.getIssues().get(0));
    }

    @Test
    void testEvaluationToString() {
        AgentJudge.Evaluation eval = new AgentJudge.Evaluation(0.9, true, "excellent", List.of());
        String str = eval.toString();
        assertTrue(str.contains("0.90"));
        assertTrue(str.contains("true"));
    }

    @Test
    void testStepEvaluation() {
        AgentJudge.StepEvaluation stepEval = new AgentJudge.StepEvaluation(1, true, true, "good");
        assertEquals(1, stepEval.getStepNumber());
        assertTrue(stepEval.isRelevant());
        assertTrue(stepEval.isSuccessful());
        assertEquals("good", stepEval.getFeedback());
    }

    @Test
    void testStepEvaluationFromStep() {
        AgentState.AgentStep step = new AgentState.AgentStep(
                1, "thinking", "click", "clicked element", null, 100, 500);

        AgentJudge judge = new AgentJudge(null); // No LLM needed for step eval
        AgentJudge.StepEvaluation eval = judge.evaluateStep("test task", step, 1);

        assertTrue(eval.isRelevant());
        assertTrue(eval.isSuccessful());
    }

    @Test
    void testStepEvaluationWithError() {
        AgentState.AgentStep step = new AgentState.AgentStep(
                2, "thinking", "navigate", null, "timeout error", 50, 200);

        AgentJudge judge = new AgentJudge(null);
        AgentJudge.StepEvaluation eval = judge.evaluateStep("test task", step, 2);

        assertTrue(eval.isRelevant());
        assertFalse(eval.isSuccessful());
        assertTrue(eval.getFeedback().contains("error"));
    }

    @Test
    void testEvaluationNullIssues() {
        AgentJudge.Evaluation eval = new AgentJudge.Evaluation(0.5, false, "partial", null);
        assertNotNull(eval.getIssues());
        assertTrue(eval.getIssues().isEmpty());
    }
}
