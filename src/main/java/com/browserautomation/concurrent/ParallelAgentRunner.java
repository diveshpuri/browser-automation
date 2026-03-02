package com.browserautomation.concurrent;

import com.browserautomation.agent.AgentResult;
import com.browserautomation.BrowserAutomation;
import com.browserautomation.llm.LlmProvider;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.agent.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Runs multiple browser automation agents concurrently.
 *
 * <p>Supports running multiple tasks in parallel, each with its own
 * browser session, and collecting results when all complete.</p>
 *
 * <pre>{@code
 * ParallelAgentRunner runner = new ParallelAgentRunner(4);
 * runner.addTask("Search for flights", openAiProvider);
 * runner.addTask("Check hotel prices", geminiProvider);
 * Map<String, AgentResult> results = runner.runAll();
 * runner.shutdown();
 * }</pre>
 */
public class ParallelAgentRunner implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ParallelAgentRunner.class);

    private final ExecutorService executor;
    private final int maxConcurrency;
    private final List<AgentTask> tasks;
    private final BrowserProfile defaultProfile;
    private final AgentConfig defaultConfig;

    /**
     * Create a runner with specified max concurrency.
     *
     * @param maxConcurrency maximum number of concurrent agents
     */
    public ParallelAgentRunner(int maxConcurrency) {
        this(maxConcurrency, new BrowserProfile(), new AgentConfig());
    }

    /**
     * Create a runner with custom defaults.
     *
     * @param maxConcurrency maximum number of concurrent agents
     * @param defaultProfile default browser profile for agents
     * @param defaultConfig  default agent configuration
     */
    public ParallelAgentRunner(int maxConcurrency, BrowserProfile defaultProfile, AgentConfig defaultConfig) {
        this.maxConcurrency = maxConcurrency;
        this.defaultProfile = defaultProfile;
        this.defaultConfig = defaultConfig;
        this.executor = Executors.newFixedThreadPool(maxConcurrency, r -> {
            Thread t = new Thread(r);
            t.setName("agent-runner-" + t.getId());
            t.setDaemon(true);
            return t;
        });
        this.tasks = new ArrayList<>();
    }

    /**
     * Add a task to be executed.
     *
     * @param taskDescription the task description
     * @param llmProvider     the LLM provider for this task
     * @return this runner for chaining
     */
    public ParallelAgentRunner addTask(String taskDescription, LlmProvider llmProvider) {
        tasks.add(new AgentTask(taskDescription, llmProvider, null, null));
        return this;
    }

    /**
     * Add a task with custom browser profile and agent config.
     *
     * @param taskDescription the task description
     * @param llmProvider     the LLM provider
     * @param profile         custom browser profile (null for default)
     * @param config          custom agent config (null for default)
     * @return this runner for chaining
     */
    public ParallelAgentRunner addTask(String taskDescription, LlmProvider llmProvider,
                                       BrowserProfile profile, AgentConfig config) {
        tasks.add(new AgentTask(taskDescription, llmProvider, profile, config));
        return this;
    }

    /**
     * Run all tasks concurrently and wait for completion.
     *
     * @return map of task description to result
     */
    public Map<String, AgentResult> runAll() {
        return runAll(0);
    }

    /**
     * Run all tasks concurrently with a timeout.
     *
     * @param timeoutMs timeout in milliseconds (0 for no timeout)
     * @return map of task description to result
     */
    public Map<String, AgentResult> runAll(long timeoutMs) {
        logger.info("Starting {} tasks with max concurrency {}", tasks.size(), maxConcurrency);
        long start = System.currentTimeMillis();

        Map<String, Future<AgentResult>> futures = new LinkedHashMap<>();
        for (AgentTask task : tasks) {
            futures.put(task.description, executor.submit(() -> executeTask(task)));
        }

        Map<String, AgentResult> results = new LinkedHashMap<>();
        for (Map.Entry<String, Future<AgentResult>> entry : futures.entrySet()) {
            try {
                AgentResult result;
                if (timeoutMs > 0) {
                    long remaining = timeoutMs - (System.currentTimeMillis() - start);
                    result = entry.getValue().get(Math.max(remaining, 1), TimeUnit.MILLISECONDS);
                } else {
                    result = entry.getValue().get();
                }
                results.put(entry.getKey(), result);
            } catch (TimeoutException e) {
                logger.warn("Task timed out: {}", entry.getKey());
                entry.getValue().cancel(true);
                results.put(entry.getKey(), AgentResult.failed("Timed out after " + timeoutMs + "ms"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.put(entry.getKey(), AgentResult.failed("Interrupted"));
            } catch (ExecutionException e) {
                logger.error("Task failed: {}: {}", entry.getKey(), e.getCause().getMessage());
                results.put(entry.getKey(), AgentResult.failed(e.getCause().getMessage()));
            }
        }

        long duration = System.currentTimeMillis() - start;
        long successCount = results.values().stream().filter(AgentResult::isSuccess).count();
        logger.info("All {} tasks completed in {}ms ({} succeeded, {} failed)",
                tasks.size(), duration, successCount, tasks.size() - successCount);

        return results;
    }

    /**
     * Run a single task asynchronously.
     *
     * @param taskDescription the task description
     * @param llmProvider     the LLM provider
     * @return a Future with the result
     */
    public Future<AgentResult> submitTask(String taskDescription, LlmProvider llmProvider) {
        AgentTask task = new AgentTask(taskDescription, llmProvider, null, null);
        return executor.submit(() -> executeTask(task));
    }

    private AgentResult executeTask(AgentTask task) {
        BrowserProfile profile = task.profile != null ? task.profile : defaultProfile;
        AgentConfig config = task.config != null ? task.config : defaultConfig;

        logger.info("Executing task: {}", task.description);
        try {
            return BrowserAutomation.agent()
                    .task(task.description)
                    .llmProvider(task.llmProvider)
                    .browserProfile(profile)
                    .config(config)
                    .run();
        } catch (Exception e) {
            logger.error("Task execution failed: {}: {}", task.description, e.getMessage());
            return AgentResult.failed(e.getMessage());
        }
    }

    /**
     * Get the number of queued tasks.
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * Clear all queued tasks.
     */
    public void clearTasks() {
        tasks.clear();
    }

    /**
     * Shutdown the executor.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    private static class AgentTask {
        final String description;
        final LlmProvider llmProvider;
        final BrowserProfile profile;
        final AgentConfig config;

        AgentTask(String description, LlmProvider llmProvider,
                  BrowserProfile profile, AgentConfig config) {
            this.description = description;
            this.llmProvider = llmProvider;
            this.profile = profile;
            this.config = config;
        }
    }
}
