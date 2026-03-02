package com.browserautomation.sandbox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SandboxExecutor.
 */
class SandboxExecutorTest {

    private SandboxExecutor sandbox;

    @BeforeEach
    void setUp() {
        sandbox = new SandboxExecutor(new SandboxExecutor.SandboxConfig().timeoutMs(10000));
    }

    @AfterEach
    void tearDown() {
        sandbox.close();
    }

    @Test
    void testSandboxDirectoryCreated() {
        assertNotNull(sandbox.getSandboxDirectory());
        assertTrue(Files.exists(sandbox.getSandboxDirectory()));
    }

    @Test
    void testExecuteShell() {
        SandboxExecutor.ExecutionRecord result = sandbox.execute("echo hello sandbox", "shell");
        assertTrue(result.isSuccess());
        assertEquals("hello sandbox", result.getOutput());
        assertEquals("shell", result.getLanguage());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    void testExecutePython() {
        SandboxExecutor.ExecutionRecord result = sandbox.execute("print('python sandbox')", "python");
        assertTrue(result.isSuccess());
        assertEquals("python sandbox", result.getOutput());
        assertEquals("python", result.getLanguage());
    }

    @Test
    void testExecuteShellFailure() {
        SandboxExecutor.ExecutionRecord result = sandbox.execute("exit 42", "bash");
        assertFalse(result.isSuccess());
        assertEquals(42, result.getExitCode());
    }

    @Test
    void testUnsupportedLanguage() {
        // The execute() method catches IllegalArgumentException internally
        // and returns a failed ExecutionRecord
        SandboxExecutor.ExecutionRecord result = sandbox.execute("code", "cobol");
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    @Test
    void testExecutionHistory() {
        sandbox.execute("echo 1", "shell");
        sandbox.execute("echo 2", "shell");
        assertEquals(2, sandbox.getHistory().size());
    }

    @Test
    void testClearHistory() {
        sandbox.execute("echo 1", "shell");
        sandbox.clearHistory();
        assertTrue(sandbox.getHistory().isEmpty());
    }

    @Test
    void testSandboxConfigDefaults() {
        SandboxExecutor.SandboxConfig config = new SandboxExecutor.SandboxConfig();
        assertEquals(10000, config.getTimeoutMs());
        assertEquals(1000, config.getMaxOutputLines());
        assertFalse(config.isAllowNetworkAccess());
        assertEquals(256, config.getMaxMemoryMb());
    }

    @Test
    void testSandboxConfigBuilder() {
        SandboxExecutor.SandboxConfig config = new SandboxExecutor.SandboxConfig()
                .timeoutMs(5000)
                .maxOutputLines(500)
                .allowNetworkAccess(true)
                .maxMemoryMb(512);
        assertEquals(5000, config.getTimeoutMs());
        assertEquals(500, config.getMaxOutputLines());
        assertTrue(config.isAllowNetworkAccess());
        assertEquals(512, config.getMaxMemoryMb());
    }

    @Test
    void testExecutionRecordToString() {
        SandboxExecutor.ExecutionRecord record = new SandboxExecutor.ExecutionRecord(
                true, "output", null, "shell", 0, 150);
        String str = record.toString();
        assertTrue(str.contains("shell"));
        assertTrue(str.contains("true"));
        assertTrue(str.contains("150"));
    }

    @Test
    void testCloseCleansSandbox() {
        var dir = sandbox.getSandboxDirectory();
        sandbox.close();
        assertFalse(Files.exists(dir));
    }
}
