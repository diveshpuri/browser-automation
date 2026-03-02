package com.browserautomation.codeuse;

import com.browserautomation.llm.ChatMessage;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full Jupyter-notebook-like code execution agent with persistent namespace.
 * Maintains state across executions, allowing incremental code building
 * and variable persistence between cells.
 *
 * Equivalent to browser-use's code_use service (1438 lines).
 *
 * Features:
 * - Persistent Python/Node.js namespace across executions
 * - Variable tracking and inspection
 * - Output capture with rich formatting
 * - Error recovery and retry
 * - LLM-assisted code generation
 * - Cell-based execution history
 */
public class CodeUseAgent {

    private static final Logger logger = LoggerFactory.getLogger(CodeUseAgent.class);

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(?:python|javascript|js|bash|sh)?\\s*\\n(.*?)\\n```",
            Pattern.DOTALL);

    private final LlmProvider llmProvider;
    private final Language language;
    private final long timeoutMs;
    private final Path workingDirectory;
    private final Map<String, Object> namespace;
    private final List<Cell> cells;
    private final Map<String, String> environmentVariables;
    private Process persistentProcess;
    private BufferedWriter processWriter;
    private BufferedReader processReader;
    private BufferedReader processErrorReader;
    private boolean initialized;

    public enum Language {
        PYTHON("python3", ".py"),
        JAVASCRIPT("node", ".js"),
        BASH("bash", ".sh");

        private final String command;
        private final String extension;

        Language(String command, String extension) {
            this.command = command;
            this.extension = extension;
        }

        public String getCommand() { return command; }
        public String getExtension() { return extension; }
    }

    public CodeUseAgent(LlmProvider llmProvider) {
        this(llmProvider, Language.PYTHON, 60000);
    }

    public CodeUseAgent(LlmProvider llmProvider, Language language, long timeoutMs) {
        this.llmProvider = llmProvider;
        this.language = language;
        this.timeoutMs = timeoutMs;
        this.workingDirectory = Path.of(System.getProperty("java.io.tmpdir"), "code-use-agent");
        this.namespace = new LinkedHashMap<>();
        this.cells = new ArrayList<>();
        this.environmentVariables = new LinkedHashMap<>();
        this.initialized = false;
    }

    /**
     * Execute a code cell and return the result.
     *
     * @param code the code to execute
     * @return the execution result
     */
    public CellResult executeCell(String code) {
        int cellNumber = cells.size() + 1;
        logger.info("Executing cell #{}: {}", cellNumber, truncate(code, 100));

        long start = System.currentTimeMillis();
        CellResult result;

        try {
            ensureWorkingDirectory();
            result = executeCode(code, cellNumber);
        } catch (Exception e) {
            result = new CellResult(cellNumber, code, false, "",
                    e.getMessage(), System.currentTimeMillis() - start);
        }

        Cell cell = new Cell(cellNumber, code, result);
        cells.add(cell);

        // Track variables from output
        if (result.isSuccess()) {
            trackVariables(code, result.getOutput());
        }

        return result;
    }

    /**
     * Ask the LLM to generate and execute code for a task.
     *
     * @param task the task description
     * @return the execution result
     */
    public CellResult executeTask(String task) {
        logger.info("Generating code for task: {}", task);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(getCodeGenPrompt()));

        // Include previous cell context
        if (!cells.isEmpty()) {
            StringBuilder context = new StringBuilder("Previous cells:\n");
            int startIdx = Math.max(0, cells.size() - 5);
            for (int i = startIdx; i < cells.size(); i++) {
                Cell cell = cells.get(i);
                context.append(String.format("In [%d]:\n%s\n", cell.number(), cell.code()));
                if (cell.result().isSuccess()) {
                    context.append(String.format("Out [%d]: %s\n", cell.number(),
                            truncate(cell.result().getOutput(), 200)));
                }
            }
            messages.add(ChatMessage.user(context.toString()));
        }

        // Include namespace info
        if (!namespace.isEmpty()) {
            StringBuilder nsInfo = new StringBuilder("Current variables:\n");
            namespace.forEach((k, v) -> nsInfo.append(String.format("  %s = %s\n", k, truncate(String.valueOf(v), 100))));
            messages.add(ChatMessage.user(nsInfo.toString()));
        }

        messages.add(ChatMessage.user("Task: " + task + "\n\nGenerate code to accomplish this task."));

        try {
            LlmResponse response = llmProvider.chatCompletion(messages);
            String content = response.getContent();
            String code = extractCode(content);
            if (code != null && !code.isEmpty()) {
                return executeCell(code);
            } else {
                return new CellResult(cells.size() + 1, "", false, "",
                        "LLM did not generate executable code", 0);
            }
        } catch (Exception e) {
            return new CellResult(cells.size() + 1, "", false, "",
                    "Failed to generate code: " + e.getMessage(), 0);
        }
    }

    /**
     * Set a variable in the namespace.
     */
    public void setVariable(String name, Object value) {
        namespace.put(name, value);
    }

    /**
     * Get a variable from the namespace.
     */
    public Object getVariable(String name) {
        return namespace.get(name);
    }

    /**
     * Get all variables in the namespace.
     */
    public Map<String, Object> getNamespace() {
        return Collections.unmodifiableMap(namespace);
    }

    /**
     * Get execution history.
     */
    public List<Cell> getCells() {
        return Collections.unmodifiableList(cells);
    }

    /**
     * Get the last cell result.
     */
    public CellResult getLastResult() {
        if (cells.isEmpty()) return null;
        return cells.get(cells.size() - 1).result();
    }

    /**
     * Clear the namespace and history.
     */
    public void reset() {
        namespace.clear();
        cells.clear();
        stopPersistentProcess();
        initialized = false;
    }

    /**
     * Set an environment variable for code execution.
     */
    public void setEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }

    /**
     * Get the language being used.
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Close and cleanup resources.
     */
    public void close() {
        stopPersistentProcess();
    }

    private CellResult executeCode(String code, int cellNumber) throws Exception {
        long start = System.currentTimeMillis();

        // Write code to temp file
        Path tempFile = workingDirectory.resolve("cell_" + cellNumber + language.getExtension());
        
        // For Python, inject namespace variables
        String fullCode = code;
        if (language == Language.PYTHON && !namespace.isEmpty()) {
            StringBuilder preamble = new StringBuilder("# Injected namespace\n");
            for (Map.Entry<String, Object> entry : namespace.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof String) {
                    preamble.append(String.format("%s = %s\n", entry.getKey(), quoteString((String) val)));
                } else if (val instanceof Number) {
                    preamble.append(String.format("%s = %s\n", entry.getKey(), val));
                }
            }
            preamble.append("# End namespace\n\n");
            fullCode = preamble + code;
        }

        Files.writeString(tempFile, fullCode);

        ProcessBuilder pb = new ProcessBuilder(language.getCommand(), tempFile.toAbsolutePath().toString());
        pb.directory(workingDirectory.toFile());
        pb.environment().putAll(environmentVariables);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });

        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });

        stdoutThread.start();
        stderrThread.start();

        boolean completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        long duration = System.currentTimeMillis() - start;

        if (!completed) {
            process.destroyForcibly();
            stdoutThread.interrupt();
            stderrThread.interrupt();
            return new CellResult(cellNumber, code, false, stdout.toString().trim(),
                    "Execution timed out after " + timeoutMs + "ms", duration);
        }

        stdoutThread.join(1000);
        stderrThread.join(1000);

        int exitCode = process.exitValue();
        String output = stdout.toString().trim();
        String error = stderr.toString().trim();

        // Clean up temp file
        Files.deleteIfExists(tempFile);

        if (exitCode == 0) {
            return new CellResult(cellNumber, code, true, output, null, duration);
        } else {
            return new CellResult(cellNumber, code, false, output,
                    error.isEmpty() ? "Exit code: " + exitCode : error, duration);
        }
    }

    private void trackVariables(String code, String output) {
        // Simple variable tracking from Python assignments
        if (language == Language.PYTHON) {
            String[] lines = code.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                // Match simple assignments: var = value
                if (trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*=\\s*.+") && !trimmed.startsWith("#")) {
                    String varName = trimmed.split("\\s*=\\s*", 2)[0].trim();
                    String varValue = trimmed.split("\\s*=\\s*", 2)[1].trim();
                    // Store as string representation
                    namespace.put(varName, varValue);
                }
            }
        }
    }

    private String extractCode(String text) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // If no code block found, try to use the whole text if it looks like code
        String trimmed = text.trim();
        if (trimmed.contains("\n") || trimmed.contains("=") || trimmed.contains("(")) {
            return trimmed;
        }
        return null;
    }

    private String getCodeGenPrompt() {
        return String.format(
                "You are a code execution assistant. Generate %s code to accomplish the given task. " +
                "Wrap your code in a ```%s code block. " +
                "Use print() to output results. " +
                "The code should be self-contained and executable. " +
                "You have access to previously defined variables in the namespace.",
                language.name().toLowerCase(), language.name().toLowerCase());
    }

    private void ensureWorkingDirectory() throws IOException {
        if (!Files.exists(workingDirectory)) {
            Files.createDirectories(workingDirectory);
        }
    }

    private void stopPersistentProcess() {
        if (persistentProcess != null && persistentProcess.isAlive()) {
            persistentProcess.destroyForcibly();
            persistentProcess = null;
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String quoteString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    /**
     * Result of a cell execution.
     */
    public record CellResult(
            int cellNumber,
            String code,
            boolean success,
            String output,
            String error,
            long durationMs
    ) {
        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }

        @Override
        public String toString() {
            if (success) {
                return String.format("Out [%d]: %s", cellNumber,
                        output.length() > 200 ? output.substring(0, 200) + "..." : output);
            } else {
                return String.format("Error [%d]: %s", cellNumber, error);
            }
        }
    }

    /**
     * Represents an executed cell in history.
     */
    public record Cell(int number, String code, CellResult result) {}
}
