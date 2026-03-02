package com.browserautomation.agent;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultiActExecutorTest {

    private BrowserSession session;
    private EventBus eventBus;
    private MultiActExecutor executor;

    @BeforeEach
    void setUp() {
        session = mock(BrowserSession.class);
        eventBus = new EventBus();
        executor = new MultiActExecutor(session, eventBus, 5);
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
    }

    @Test
    void testExecuteMultipleActions() {
        List<MultiActExecutor.ActionRequest> actions = List.of(
                new MultiActExecutor.ActionRequest("click", Map.of("element", 1)),
                new MultiActExecutor.ActionRequest("type", Map.of("element", 2, "text", "hello")),
                new MultiActExecutor.ActionRequest("click", Map.of("element", 3))
        );

        MultiActExecutor.MultiActResult result = executor.execute(actions,
                action -> MultiActExecutor.ActionResult.success());

        assertTrue(result.allSuccess());
        assertEquals(3, result.getExecutedCount());
        assertEquals(3, result.getSuccessCount());
    }

    @Test
    void testStopsOnFailure() {
        List<MultiActExecutor.ActionRequest> actions = List.of(
                new MultiActExecutor.ActionRequest("click", Map.of("element", 1)),
                new MultiActExecutor.ActionRequest("click", Map.of("element", 2)),
                new MultiActExecutor.ActionRequest("click", Map.of("element", 3))
        );

        int[] callCount = {0};
        MultiActExecutor.MultiActResult result = executor.execute(actions, action -> {
            callCount[0]++;
            if (callCount[0] == 2) {
                return MultiActExecutor.ActionResult.failure("Element not found");
            }
            return MultiActExecutor.ActionResult.success();
        });

        assertFalse(result.allSuccess());
        assertEquals(2, result.getExecutedCount());
        assertEquals(1, result.getSuccessCount());
    }

    @Test
    void testRespectsMaxActionsPerStep() {
        MultiActExecutor limitedExecutor = new MultiActExecutor(session, eventBus, 2);

        List<MultiActExecutor.ActionRequest> actions = List.of(
                new MultiActExecutor.ActionRequest("click", Map.of("element", 1)),
                new MultiActExecutor.ActionRequest("click", Map.of("element", 2)),
                new MultiActExecutor.ActionRequest("click", Map.of("element", 3))
        );

        MultiActExecutor.MultiActResult result = limitedExecutor.execute(actions,
                action -> MultiActExecutor.ActionResult.success());

        assertEquals(2, result.getExecutedCount());
    }

    @Test
    void testStopsOnTerminalAction() {
        List<MultiActExecutor.ActionRequest> actions = List.of(
                new MultiActExecutor.ActionRequest("click", Map.of("element", 1)),
                new MultiActExecutor.ActionRequest("done", Map.of("result", "finished"), true),
                new MultiActExecutor.ActionRequest("click", Map.of("element", 3))
        );

        MultiActExecutor.MultiActResult result = executor.execute(actions,
                action -> MultiActExecutor.ActionResult.success());

        assertEquals(2, result.getExecutedCount());
        assertTrue(result.allSuccess());
    }

    @Test
    void testHandlesExceptions() {
        List<MultiActExecutor.ActionRequest> actions = List.of(
                new MultiActExecutor.ActionRequest("click", Map.of("element", 1))
        );

        MultiActExecutor.MultiActResult result = executor.execute(actions,
                action -> { throw new RuntimeException("Unexpected error"); });

        assertFalse(result.allSuccess());
        assertEquals(1, result.getExecutedCount());
    }

    @Test
    void testActionRequestParams() {
        MultiActExecutor.ActionRequest request = new MultiActExecutor.ActionRequest(
                "click", Map.of("element", 5, "text", "hello"));

        assertEquals("click", request.getActionType());
        assertEquals(5, (int) request.getParam("element"));
        assertEquals("hello", request.getParam("text"));
        assertNull(request.getParam("nonexistent"));
        assertEquals("default", request.getParam("nonexistent", "default"));
        assertFalse(request.isTerminal());
    }

    @Test
    void testActionResult() {
        MultiActExecutor.ActionResult success = MultiActExecutor.ActionResult.success("output");
        assertTrue(success.isSuccess());
        assertEquals("output", success.getOutput());
        assertNull(success.getError());

        MultiActExecutor.ActionResult failure = MultiActExecutor.ActionResult.failure("error msg");
        assertFalse(failure.isSuccess());
        assertNull(failure.getOutput());
        assertEquals("error msg", failure.getError());
    }

    @Test
    void testGetMaxActionsPerStep() {
        assertEquals(5, executor.getMaxActionsPerStep());
    }

    @Test
    void testEmptyActionList() {
        MultiActExecutor.MultiActResult result = executor.execute(List.of(),
                action -> MultiActExecutor.ActionResult.success());
        assertTrue(result.allSuccess());
        assertEquals(0, result.getExecutedCount());
    }
}
