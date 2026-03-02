package com.browserautomation.codeuse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CodeExecutor.
 */
class CodeExecutorTest {

    @Test
    void testDefaultConstructor() {
        CodeExecutor executor = new CodeExecutor();
        assertTrue(executor.getHistory().isEmpty());
    }

    @Test
    void testCustomTimeout() {
        CodeExecutor executor = new CodeExecutor(5000);
        assertTrue(executor.getHistory().isEmpty());
    }

    @Test
    void testExecuteCommand() {
        CodeExecutor executor = new CodeExecutor();
        CodeExecutor.ExecutionResult result = executor.executeCommand("echo hello");
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getOutput());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    void testExecuteCommandFailure() {
        CodeExecutor executor = new CodeExecutor();
        CodeExecutor.ExecutionResult result = executor.executeCommand("exit 1");
        assertFalse(result.isSuccess());
        assertEquals(1, result.getExitCode());
    }

    @Test
    void testExecuteCommandHistory() {
        CodeExecutor executor = new CodeExecutor();
        executor.executeCommand("echo a");
        executor.executeCommand("echo b");
        assertEquals(2, executor.getHistory().size());
    }

    @Test
    void testClearHistory() {
        CodeExecutor executor = new CodeExecutor();
        executor.executeCommand("echo a");
        executor.clearHistory();
        assertTrue(executor.getHistory().isEmpty());
    }

    @Test
    void testSetWorkingDirectory(@TempDir Path tempDir) {
        CodeExecutor executor = new CodeExecutor();
        executor.setWorkingDirectory(tempDir);
        CodeExecutor.ExecutionResult result = executor.executeCommand("pwd");
        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains(tempDir.getFileName().toString()));
    }

    @Test
    void testSetEnvironmentVariable() {
        CodeExecutor executor = new CodeExecutor();
        executor.setEnvironmentVariable("TEST_VAR", "test_value");
        CodeExecutor.ExecutionResult result = executor.executeCommand("echo $TEST_VAR");
        assertTrue(result.isSuccess());
        assertEquals("test_value", result.getOutput());
    }

    @Test
    void testExecutePython() {
        CodeExecutor executor = new CodeExecutor();
        CodeExecutor.ExecutionResult result = executor.executePython("print('hello from python')");
        assertTrue(result.isSuccess());
        assertEquals("hello from python", result.getOutput());
    }

    @Test
    void testExecutionResultToString() {
        CodeExecutor.ExecutionResult result = new CodeExecutor.ExecutionResult(true, "output", null, 0, 100);
        String str = result.toString();
        assertTrue(str.contains("true"));
        assertTrue(str.contains("100"));
    }
}
