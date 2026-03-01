package com.browserautomation.telemetry;

import com.browserautomation.agent.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Telemetry data collection service.
 * Equivalent to browser-use's telemetry module.
 *
 * Collects usage metrics, performance data, and operational telemetry
 * for monitoring and optimization. All data is stored locally.
 */
public class TelemetryService implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryService.class);

    private boolean enabled;
    private final List<TelemetryEvent> events;
    private final Map<String, AtomicLong> counters;
    private final Map<String, List<Double>> metrics;
    private final int maxEvents;
    private final long startTime;

    public TelemetryService() {
        this(true, 10000);
    }

    public TelemetryService(boolean enabled, int maxEvents) {
        this.enabled = enabled;
        this.maxEvents = maxEvents;
        this.events = new ArrayList<>();
        this.counters = new ConcurrentHashMap<>();
        this.metrics = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Enable telemetry collection.
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Disable telemetry collection.
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * Record a telemetry event.
     *
     * @param category the event category (e.g., "agent", "browser", "llm")
     * @param action   the event action (e.g., "step_completed", "navigation")
     * @param label    optional label for the event
     * @param value    optional numeric value
     */
    public void recordEvent(String category, String action, String label, Double value) {
        if (!enabled) return;

        TelemetryEvent event = new TelemetryEvent(category, action, label, value, System.currentTimeMillis());
        synchronized (events) {
            events.add(event);
            if (events.size() > maxEvents) {
                events.remove(0);
            }
        }
        logger.trace("Telemetry: {}/{} {} {}", category, action, label, value);
    }

    /**
     * Record a simple event.
     */
    public void recordEvent(String category, String action) {
        recordEvent(category, action, null, null);
    }

    /**
     * Increment a named counter.
     */
    public void incrementCounter(String name) {
        if (!enabled) return;
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Increment a counter by a specific amount.
     */
    public void incrementCounter(String name, long amount) {
        if (!enabled) return;
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(amount);
    }

    /**
     * Get the value of a counter.
     */
    public long getCounter(String name) {
        AtomicLong counter = counters.get(name);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Record a metric value (for averaging, min/max, etc.).
     */
    public void recordMetric(String name, double value) {
        if (!enabled) return;
        metrics.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    /**
     * Get the average value of a metric.
     */
    public double getMetricAverage(String name) {
        List<Double> values = metrics.get(name);
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    /**
     * Get the min value of a metric.
     */
    public double getMetricMin(String name) {
        List<Double> values = metrics.get(name);
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(d -> d).min().orElse(0.0);
    }

    /**
     * Get the max value of a metric.
     */
    public double getMetricMax(String name) {
        List<Double> values = metrics.get(name);
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(d -> d).max().orElse(0.0);
    }

    /**
     * Record telemetry from an agent result.
     */
    public void recordAgentResult(String taskName, AgentResult result) {
        if (!enabled) return;

        recordEvent("agent", "task_completed", taskName,
                result.isSuccess() ? 1.0 : 0.0);
        recordMetric("agent.steps", result.getTotalSteps());
        recordMetric("agent.tokens", result.getTotalTokensUsed());
        recordMetric("agent.duration_ms", result.getTotalDurationMs());
        incrementCounter("agent.total_tasks");
        if (result.isSuccess()) {
            incrementCounter("agent.successful_tasks");
        } else {
            incrementCounter("agent.failed_tasks");
        }
    }

    /**
     * Record an LLM API call.
     */
    public void recordLlmCall(String provider, String model, int inputTokens,
                               int outputTokens, long latencyMs) {
        if (!enabled) return;

        recordEvent("llm", "api_call", provider + "/" + model, (double) latencyMs);
        incrementCounter("llm.total_calls");
        incrementCounter("llm.total_input_tokens", inputTokens);
        incrementCounter("llm.total_output_tokens", outputTokens);
        recordMetric("llm.latency_ms", latencyMs);
    }

    /**
     * Record a browser action.
     */
    public void recordBrowserAction(String actionName, boolean success, long durationMs) {
        if (!enabled) return;

        recordEvent("browser", "action", actionName, success ? 1.0 : 0.0);
        incrementCounter("browser.total_actions");
        if (success) {
            incrementCounter("browser.successful_actions");
        }
        recordMetric("browser.action_duration_ms", durationMs);
    }

    /**
     * Get all recorded events.
     */
    public List<TelemetryEvent> getEvents() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    /**
     * Get events filtered by category.
     */
    public List<TelemetryEvent> getEventsByCategory(String category) {
        synchronized (events) {
            List<TelemetryEvent> filtered = new ArrayList<>();
            for (TelemetryEvent event : events) {
                if (event.getCategory().equals(category)) {
                    filtered.add(event);
                }
            }
            return filtered;
        }
    }

    /**
     * Get all counters.
     */
    public Map<String, Long> getCounters() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }

    /**
     * Generate a summary report.
     */
    public String getSummary() {
        long uptime = System.currentTimeMillis() - startTime;
        StringBuilder sb = new StringBuilder();
        sb.append("=== Telemetry Summary ===\n");
        sb.append("Uptime: ").append(uptime / 1000).append("s\n");
        sb.append("Events: ").append(events.size()).append("\n\n");

        sb.append("Counters:\n");
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue().get()).append("\n");
        }

        sb.append("\nMetrics:\n");
        for (Map.Entry<String, List<Double>> entry : metrics.entrySet()) {
            String name = entry.getKey();
            sb.append("  ").append(name).append(": avg=")
                    .append(String.format("%.2f", getMetricAverage(name)))
                    .append(", min=").append(String.format("%.2f", getMetricMin(name)))
                    .append(", max=").append(String.format("%.2f", getMetricMax(name)))
                    .append(", count=").append(entry.getValue().size())
                    .append("\n");
        }

        return sb.toString();
    }

    /**
     * Check if telemetry is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Clear all telemetry data.
     */
    public void reset() {
        synchronized (events) {
            events.clear();
        }
        counters.clear();
        metrics.clear();
    }

    @Override
    public void close() {
        reset();
    }

    /**
     * Represents a single telemetry event.
     */
    public static class TelemetryEvent {
        private final String category;
        private final String action;
        private final String label;
        private final Double value;
        private final long timestamp;

        public TelemetryEvent(String category, String action, String label, Double value, long timestamp) {
            this.category = category;
            this.action = action;
            this.label = label;
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getCategory() { return category; }
        public String getAction() { return action; }
        public String getLabel() { return label; }
        public Double getValue() { return value; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("TelemetryEvent[%s/%s: %s=%s @%s]",
                    category, action, label, value,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
        }
    }
}
