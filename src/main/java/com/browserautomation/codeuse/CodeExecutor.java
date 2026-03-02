package com.browserautomation.codeuse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Code understanding and execution module.
 *
 * Allows the agent to execute code snippets in a controlled environment
 * and use the results to inform browser automation decisions.
 */
public class CodeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CodeExecutor.class);

    private final long timeoutMs;
    private final Map<String, String> environmentVariables;
    private final List<ExecutionResult> history;
    private Path workingDirectory;

    public CodeExecutor() {
        this(30000);
    }

    public CodeExecutor(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.environmentVariables = new HashMap<>();
        this.history = new ArrayList<>();
        this.workingDirectory = Path.of(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Execute a shell command and return the result.
     *
     * @param command the shell command to execute
     * @return the execution result
     */
    public ExecutionResult executeCommand(String command) {
        logger.info("Executing command: {}", command);
        long start = System.currentTimeMillis();

        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.directory(workingDirectory.toFile());
            pb.environment().putAll(environmentVariables);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long duration = System.currentTimeMillis() - start;

            if (!completed) {
                process.destroyForcibly();
                ExecutionResult result = new ExecutionResult(false, "", "Command timed out after " + timeoutMs + "ms", -1, duration);
                history.add(result);
                return result;
            }

            int exitCode = process.exitValue();
            ExecutionResult result = new ExecutionResult(
                    exitCode == 0,
                    output.toString().trim(),
                    exitCode != 0 ? "Exit code: " + exitCode : null,
                    exitCode,
                    duration
            );
            history.add(result);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            ExecutionResult result = new ExecutionResult(false, "", e.getMessage(), -1, duration);
            history.add(result);
            return result;
        }
    }

    /**
     * Execute a JavaScript snippet using Node.js (if available).
     *
     * @param code the JavaScript code to execute
     * @return the execution result
     */
    public ExecutionResult executeJavaScript(String code) {
        try {
            Path tempFile = Files.createTempFile("ba-code-", ".js");
            Files.writeString(tempFile, code);
            ExecutionResult result = executeCommand("node " + tempFile.toAbsolutePath());
            Files.deleteIfExists(tempFile);
            return result;
        } catch (Exception e) {
            return new ExecutionResult(false, "", "Failed to execute JavaScript: " + e.getMessage(), -1, 0);
        }
    }

    /**
     * Execute a Python snippet (if available).
     *
     * @param code the Python code to execute
     * @return the execution result
     */
    public ExecutionResult executePython(String code) {
        try {
            Path tempFile = Files.createTempFile("ba-code-", ".py");
            Files.writeString(tempFile, code);
            ExecutionResult result = executeCommand("python3 " + tempFile.toAbsolutePath());
            Files.deleteIfExists(tempFile);
            return result;
        } catch (Exception e) {
            return new ExecutionResult(false, "", "Failed to execute Python: " + e.getMessage(), -1, 0);
        }
    }

    /**
     * Set an environment variable for code execution.
     */
    public void setEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }

    /**
     * Set the working directory for code execution.
     */
    public void setWorkingDirectory(Path directory) {
        this.workingDirectory = directory;
    }

    /**
     * Get the execution history.
     */
    public List<ExecutionResult> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Clear execution history.
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * Result of a code execution.
     */
    public static class ExecutionResult {
        private final boolean success;
        private final String output;
        private final String error;
        private final int exitCode;
        private final long durationMs;

        public ExecutionResult(boolean success, String output, String error, int exitCode, long durationMs) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.exitCode = exitCode;
            this.durationMs = durationMs;
        }

        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public int getExitCode() { return exitCode; }
        public long getDurationMs() { return durationMs; }

        @Override
        public String toString() {
            return String.format("ExecutionResult[success=%s, exit=%d, duration=%dms, output=%s]",
                    success, exitCode, durationMs,
                    output.length() > 100 ? output.substring(0, 100) + "..." : output);
        }
    }
}
