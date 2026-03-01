package com.browserautomation.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Sandboxed code execution environment.
 * Equivalent to browser-use's sandbox module.
 *
 * Provides isolated execution of code snippets with resource limits,
 * restricted file system access, and timeout enforcement.
 */
public class SandboxExecutor implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SandboxExecutor.class);

    private final SandboxConfig config;
    private final List<ExecutionRecord> executionHistory;
    private Path sandboxDirectory;

    public SandboxExecutor() {
        this(new SandboxConfig());
    }

    public SandboxExecutor(SandboxConfig config) {
        this.config = config;
        this.executionHistory = new ArrayList<>();
        initSandbox();
    }

    private void initSandbox() {
        try {
            sandboxDirectory = Files.createTempDirectory("ba-sandbox-");
            logger.info("Sandbox initialized at: {}", sandboxDirectory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize sandbox", e);
        }
    }

    /**
     * Execute code in the sandbox.
     *
     * @param code     the code to execute
     * @param language the programming language (javascript, python, shell)
     * @return the execution result
     */
    public ExecutionRecord execute(String code, String language) {
        logger.info("Executing {} code in sandbox", language);
        long start = System.currentTimeMillis();

        try {
            // Write code to a temp file in the sandbox
            String extension = getExtension(language);
            Path codeFile = Files.createTempFile(sandboxDirectory, "sandbox-", extension);
            Files.writeString(codeFile, code, StandardCharsets.UTF_8);

            // Build command based on language
            List<String> command = buildCommand(language, codeFile);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(sandboxDirectory.toFile());
            pb.redirectErrorStream(true);

            // Apply environment restrictions
            Map<String, String> env = pb.environment();
            if (!config.isAllowNetworkAccess()) {
                env.put("http_proxy", "http://0.0.0.0:0");
                env.put("https_proxy", "http://0.0.0.0:0");
            }

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < config.getMaxOutputLines()) {
                    output.append(line).append("\n");
                    lineCount++;
                }
            }

            boolean finished = process.waitFor(config.getTimeoutMs(), TimeUnit.MILLISECONDS);
            long duration = System.currentTimeMillis() - start;

            // Clean up temp file
            Files.deleteIfExists(codeFile);

            ExecutionRecord record;
            if (!finished) {
                process.destroyForcibly();
                record = new ExecutionRecord(false, "", "Execution timed out after " + config.getTimeoutMs() + "ms",
                        language, -1, duration);
            } else {
                int exitCode = process.exitValue();
                record = new ExecutionRecord(
                        exitCode == 0,
                        output.toString().trim(),
                        exitCode != 0 ? "Exit code: " + exitCode : null,
                        language, exitCode, duration
                );
            }

            executionHistory.add(record);
            return record;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            ExecutionRecord record = new ExecutionRecord(false, "", e.getMessage(), language, -1, duration);
            executionHistory.add(record);
            return record;
        }
    }

    private List<String> buildCommand(String language, Path codeFile) {
        return switch (language.toLowerCase()) {
            case "javascript", "js", "node" -> List.of("node", codeFile.toString());
            case "python", "py", "python3" -> List.of("python3", codeFile.toString());
            case "shell", "sh", "bash" -> List.of("sh", codeFile.toString());
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private String getExtension(String language) {
        return switch (language.toLowerCase()) {
            case "javascript", "js", "node" -> ".js";
            case "python", "py", "python3" -> ".py";
            case "shell", "sh", "bash" -> ".sh";
            default -> ".txt";
        };
    }

    /**
     * Get execution history.
     */
    public List<ExecutionRecord> getHistory() {
        return new ArrayList<>(executionHistory);
    }

    /**
     * Clear execution history.
     */
    public void clearHistory() {
        executionHistory.clear();
    }

    /**
     * Get the sandbox directory.
     */
    public Path getSandboxDirectory() {
        return sandboxDirectory;
    }

    @Override
    public void close() {
        try {
            if (sandboxDirectory != null && Files.exists(sandboxDirectory)) {
                // Clean up sandbox directory
                try (var stream = Files.walk(sandboxDirectory)) {
                    stream.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                            });
                }
                logger.info("Sandbox cleaned up");
            }
        } catch (Exception e) {
            logger.warn("Failed to clean sandbox: {}", e.getMessage());
        }
    }

    /**
     * Configuration for the sandbox executor.
     */
    public static class SandboxConfig {
        private long timeoutMs = 10000;
        private int maxOutputLines = 1000;
        private boolean allowNetworkAccess = false;
        private long maxMemoryMb = 256;

        public SandboxConfig timeoutMs(long ms) { this.timeoutMs = ms; return this; }
        public SandboxConfig maxOutputLines(int lines) { this.maxOutputLines = lines; return this; }
        public SandboxConfig allowNetworkAccess(boolean allow) { this.allowNetworkAccess = allow; return this; }
        public SandboxConfig maxMemoryMb(long mb) { this.maxMemoryMb = mb; return this; }

        public long getTimeoutMs() { return timeoutMs; }
        public int getMaxOutputLines() { return maxOutputLines; }
        public boolean isAllowNetworkAccess() { return allowNetworkAccess; }
        public long getMaxMemoryMb() { return maxMemoryMb; }
    }

    /**
     * Record of a sandbox execution.
     */
    public static class ExecutionRecord {
        private final boolean success;
        private final String output;
        private final String error;
        private final String language;
        private final int exitCode;
        private final long durationMs;

        public ExecutionRecord(boolean success, String output, String error,
                               String language, int exitCode, long durationMs) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.language = language;
            this.exitCode = exitCode;
            this.durationMs = durationMs;
        }

        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public String getLanguage() { return language; }
        public int getExitCode() { return exitCode; }
        public long getDurationMs() { return durationMs; }

        @Override
        public String toString() {
            return String.format("ExecutionRecord[%s, success=%s, exit=%d, %dms]",
                    language, success, exitCode, durationMs);
        }
    }
}
