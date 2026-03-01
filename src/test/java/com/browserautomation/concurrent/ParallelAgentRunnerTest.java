package com.browserautomation.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ParallelAgentRunner.
 */
class ParallelAgentRunnerTest {

    @Test
    void testConstruction() {
        try (ParallelAgentRunner runner = new ParallelAgentRunner(4)) {
            assertEquals(0, runner.getTaskCount());
        }
    }

    @Test
    void testClearTasks() {
        try (ParallelAgentRunner runner = new ParallelAgentRunner(4)) {
            // We can't add real tasks without LLM providers in unit tests,
            // but we can test the clear mechanism
            assertEquals(0, runner.getTaskCount());
            runner.clearTasks();
            assertEquals(0, runner.getTaskCount());
        }
    }

    @Test
    void testShutdown() {
        ParallelAgentRunner runner = new ParallelAgentRunner(2);
        runner.shutdown(); // Should not throw
    }

    @Test
    void testRunAllEmpty() {
        try (ParallelAgentRunner runner = new ParallelAgentRunner(2)) {
            var results = runner.runAll();
            assertTrue(results.isEmpty());
        }
    }

    @Test
    void testAutoCloseable() {
        // Verify it implements AutoCloseable correctly
        try (ParallelAgentRunner runner = new ParallelAgentRunner(2)) {
            assertNotNull(runner);
        }
    }
}
