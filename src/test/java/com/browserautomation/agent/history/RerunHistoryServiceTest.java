package com.browserautomation.agent.history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RerunHistoryServiceTest {

    private RerunHistoryService service;

    @BeforeEach
    void setUp() {
        service = new RerunHistoryService();
    }

    @Test
    void testSaveAndLoadHistory(@TempDir Path tempDir) throws IOException {
        RerunHistoryService.AgentHistoryTrace trace = new RerunHistoryService.AgentHistoryTrace("Find stars");

        RerunHistoryService.HistoryStep step1 = new RerunHistoryService.HistoryStep(1, "https://github.com", "Navigate to GitHub");
        RerunHistoryService.HistoryAction action1 = new RerunHistoryService.HistoryAction("navigate", Map.of("url", "https://github.com"));
        action1.setSuccess(true);
        step1.addAction(action1);
        step1.setSuccess(true);

        RerunHistoryService.HistoryStep step2 = new RerunHistoryService.HistoryStep(2, "https://github.com", "Click search");
        RerunHistoryService.HistoryAction action2 = new RerunHistoryService.HistoryAction("click", Map.of("element", 5));
        action2.setSuccess(true);
        step2.addAction(action2);
        step2.setSuccess(true);

        trace.addStep(step1);
        trace.addStep(step2);
        trace.complete(true, "Found 50k stars");

        Path filePath = tempDir.resolve("history.json");
        service.saveHistory(trace, filePath);
        assertTrue(filePath.toFile().exists());

        RerunHistoryService.AgentHistoryTrace loaded = service.loadHistory(filePath);
        assertEquals("Find stars", loaded.getTask());
        assertEquals(2, loaded.getTotalSteps());
        assertTrue(loaded.isSuccess());
        assertEquals("Found 50k stars", loaded.getResult());
        assertEquals(2, loaded.getSteps().size());
    }

    @Test
    void testCreateReplayPlan() {
        RerunHistoryService.AgentHistoryTrace trace = new RerunHistoryService.AgentHistoryTrace("Test task");

        RerunHistoryService.HistoryStep step = new RerunHistoryService.HistoryStep(1, "https://a.com", "Navigate");
        step.addAction(new RerunHistoryService.HistoryAction("navigate", Map.of("url", "https://a.com")));
        step.addAction(new RerunHistoryService.HistoryAction("click", Map.of("element", 1)));
        trace.addStep(step);

        List<RerunHistoryService.ReplayAction> plan = service.createReplayPlan(trace);
        assertEquals(2, plan.size());
        assertEquals("navigate", plan.get(0).actionType());
        assertEquals("click", plan.get(1).actionType());
        assertEquals(1, plan.get(0).originalStep());
    }

    @Test
    void testAgentHistoryTrace() {
        RerunHistoryService.AgentHistoryTrace trace = new RerunHistoryService.AgentHistoryTrace("task");
        assertNotNull(trace.getStartTime());
        assertEquals("task", trace.getTask());
        assertFalse(trace.isSuccess());
        assertEquals(0, trace.getTotalSteps());

        trace.complete(true, "done");
        assertTrue(trace.isSuccess());
        assertEquals("done", trace.getResult());
        assertNotNull(trace.getEndTime());
    }

    @Test
    void testHistoryStep() {
        RerunHistoryService.HistoryStep step = new RerunHistoryService.HistoryStep(1, "https://url.com", "thinking");
        assertEquals(1, step.getStepNumber());
        assertEquals("https://url.com", step.getUrl());
        assertEquals("thinking", step.getThought());
        assertTrue(step.getActions().isEmpty());

        step.addAction(new RerunHistoryService.HistoryAction("click", Map.of("elem", 1)));
        assertEquals(1, step.getActions().size());

        step.setSuccess(true);
        assertTrue(step.isSuccess());

        step.setError("some error");
        assertEquals("some error", step.getError());
    }

    @Test
    void testHistoryAction() {
        RerunHistoryService.HistoryAction action = new RerunHistoryService.HistoryAction("click", Map.of("element", 5));
        assertEquals("click", action.getActionType());
        assertEquals(5, action.getParameters().get("element"));
        assertFalse(action.isSuccess());

        action.setSuccess(true);
        assertTrue(action.isSuccess());

        action.setExpectedResult("Element clicked");
        assertEquals("Element clicked", action.getExpectedResult());
    }

    @Test
    void testReplayAction() {
        var replay = new RerunHistoryService.ReplayAction("click", Map.of("element", 3), 2, "Element clicked");
        assertEquals("click", replay.actionType());
        assertEquals(3, replay.parameters().get("element"));
        assertEquals(2, replay.originalStep());
        assertEquals("Element clicked", replay.expectedResult());
    }

    @Test
    void testEmptyTrace(@TempDir Path tempDir) throws IOException {
        RerunHistoryService.AgentHistoryTrace trace = new RerunHistoryService.AgentHistoryTrace("empty task");
        trace.complete(false, "No steps executed");

        Path filePath = tempDir.resolve("empty.json");
        service.saveHistory(trace, filePath);

        RerunHistoryService.AgentHistoryTrace loaded = service.loadHistory(filePath);
        assertEquals(0, loaded.getTotalSteps());
        assertFalse(loaded.isSuccess());
    }
}
