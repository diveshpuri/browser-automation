package com.browserautomation.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Distributed tracing and observability service for browser automation.
 * Provides span-based tracing, metrics collection, and event recording
 * for monitoring agent behavior and performance.
 *
 * Designed for integration with external observability platforms
 * (e.g., Laminar/lmnr, OpenTelemetry, Jaeger, Zipkin).
 */
public class ObservabilityService {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityService.class);

    private final String serviceName;
    private final Map<String, String> globalAttributes;
    private final ConcurrentHashMap<String, Span> activeSpans;
    private final ConcurrentLinkedQueue<Span> completedSpans;
    private final ConcurrentHashMap<String, MetricCounter> metrics;
    private final List<SpanExporter> exporters;
    private final AtomicLong spanIdCounter;
    private final int maxCompletedSpans;
    private boolean enabled;

    public ObservabilityService(String serviceName) {
        this(serviceName, 10000);
    }

    public ObservabilityService(String serviceName, int maxCompletedSpans) {
        this.serviceName = serviceName;
        this.globalAttributes = new ConcurrentHashMap<>();
        this.activeSpans = new ConcurrentHashMap<>();
        this.completedSpans = new ConcurrentLinkedQueue<>();
        this.metrics = new ConcurrentHashMap<>();
        this.exporters = new ArrayList<>();
        this.spanIdCounter = new AtomicLong(0);
        this.maxCompletedSpans = maxCompletedSpans;
        this.enabled = true;
    }

    /**
     * Start a new trace span.
     *
     * @param name the span name (e.g., "agent.step", "browser.navigate")
     * @return the created span
     */
    public Span startSpan(String name) {
        return startSpan(name, null);
    }

    /**
     * Start a new trace span with a parent.
     *
     * @param name     the span name
     * @param parentId the parent span ID (null for root spans)
     * @return the created span
     */
    public Span startSpan(String name, String parentId) {
        if (!enabled) {
            return Span.NOOP;
        }

        String spanId = generateSpanId();
        String traceId = parentId != null && activeSpans.containsKey(parentId)
                ? activeSpans.get(parentId).getTraceId()
                : generateTraceId();

        Span span = new Span(spanId, traceId, parentId, name, serviceName);
        globalAttributes.forEach(span::setAttribute);
        activeSpans.put(spanId, span);

        logger.trace("Started span: {} ({})", name, spanId);
        return span;
    }

    /**
     * End a span and record it.
     *
     * @param span the span to end
     */
    public void endSpan(Span span) {
        if (!enabled || span == Span.NOOP) {
            return;
        }

        span.end();
        activeSpans.remove(span.getSpanId());
        completedSpans.add(span);

        // Evict old spans if over limit
        while (completedSpans.size() > maxCompletedSpans) {
            completedSpans.poll();
        }

        // Export to registered exporters
        for (SpanExporter exporter : exporters) {
            try {
                exporter.export(span);
            } catch (Exception e) {
                logger.warn("Failed to export span to {}: {}", exporter.getName(), e.getMessage());
            }
        }

        logger.trace("Ended span: {} ({}) duration={}ms", span.getName(), span.getSpanId(), span.getDurationMs());
    }

    /**
     * Record a metric counter increment.
     *
     * @param name  the metric name
     * @param value the increment value
     */
    public void recordMetric(String name, long value) {
        if (!enabled) return;
        metrics.computeIfAbsent(name, k -> new MetricCounter(k)).increment(value);
    }

    /**
     * Record a metric with tags.
     *
     * @param name  the metric name
     * @param value the value
     * @param tags  key-value tags
     */
    public void recordMetric(String name, long value, Map<String, String> tags) {
        if (!enabled) return;
        String taggedName = name + tags.toString();
        metrics.computeIfAbsent(taggedName, k -> new MetricCounter(name, tags)).increment(value);
    }

    /**
     * Set a global attribute that will be added to all spans.
     *
     * @param key   the attribute key
     * @param value the attribute value
     */
    public void setGlobalAttribute(String key, String value) {
        globalAttributes.put(key, value);
    }

    /**
     * Register a span exporter.
     *
     * @param exporter the exporter to register
     */
    public void registerExporter(SpanExporter exporter) {
        exporters.add(exporter);
        logger.info("Registered span exporter: {}", exporter.getName());
    }

    /**
     * Get all completed spans.
     */
    public List<Span> getCompletedSpans() {
        return new ArrayList<>(completedSpans);
    }

    /**
     * Get all active spans.
     */
    public Map<String, Span> getActiveSpans() {
        return Collections.unmodifiableMap(activeSpans);
    }

    /**
     * Get all recorded metrics.
     */
    public Map<String, MetricCounter> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    /**
     * Get a specific metric value.
     */
    public long getMetricValue(String name) {
        MetricCounter counter = metrics.get(name);
        return counter != null ? counter.getValue() : 0;
    }

    /**
     * Enable or disable observability.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if observability is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Clear all recorded data.
     */
    public void clear() {
        activeSpans.clear();
        completedSpans.clear();
        metrics.clear();
    }

    private String generateSpanId() {
        return String.format("span-%016x", spanIdCounter.incrementAndGet());
    }

    private String generateTraceId() {
        return String.format("trace-%016x", UUID.randomUUID().getMostSignificantBits());
    }

    /**
     * Represents a trace span.
     */
    public static class Span {
        static final Span NOOP = new Span("noop", "noop", null, "noop", "noop");

        private final String spanId;
        private final String traceId;
        private final String parentId;
        private final String name;
        private final String serviceName;
        private final Instant startTime;
        private Instant endTime;
        private final Map<String, String> attributes;
        private final List<SpanEvent> events;
        private SpanStatus status;
        private String statusMessage;

        public Span(String spanId, String traceId, String parentId, String name, String serviceName) {
            this.spanId = spanId;
            this.traceId = traceId;
            this.parentId = parentId;
            this.name = name;
            this.serviceName = serviceName;
            this.startTime = Instant.now();
            this.attributes = new LinkedHashMap<>();
            this.events = new ArrayList<>();
            this.status = SpanStatus.OK;
        }

        public void setAttribute(String key, String value) {
            attributes.put(key, value);
        }

        public void addEvent(String name) {
            events.add(new SpanEvent(name, Instant.now(), Collections.emptyMap()));
        }

        public void addEvent(String name, Map<String, String> attributes) {
            events.add(new SpanEvent(name, Instant.now(), attributes));
        }

        public void setStatus(SpanStatus status, String message) {
            this.status = status;
            this.statusMessage = message;
        }

        public void end() {
            this.endTime = Instant.now();
        }

        public String getSpanId() { return spanId; }
        public String getTraceId() { return traceId; }
        public String getParentId() { return parentId; }
        public String getName() { return name; }
        public String getServiceName() { return serviceName; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }
        public List<SpanEvent> getEvents() { return Collections.unmodifiableList(events); }
        public SpanStatus getStatus() { return status; }
        public String getStatusMessage() { return statusMessage; }

        public long getDurationMs() {
            if (endTime == null) return -1;
            return endTime.toEpochMilli() - startTime.toEpochMilli();
        }

        @Override
        public String toString() {
            return String.format("Span[%s/%s, status=%s, duration=%dms]",
                    name, spanId, status, getDurationMs());
        }
    }

    public enum SpanStatus {
        OK, ERROR, UNSET
    }

    public record SpanEvent(String name, Instant timestamp, Map<String, String> attributes) {}

    /**
     * Metric counter for tracking numeric values.
     */
    public static class MetricCounter {
        private final String name;
        private final Map<String, String> tags;
        private final AtomicLong value;

        public MetricCounter(String name) {
            this(name, Collections.emptyMap());
        }

        public MetricCounter(String name, Map<String, String> tags) {
            this.name = name;
            this.tags = new LinkedHashMap<>(tags);
            this.value = new AtomicLong(0);
        }

        public void increment(long delta) {
            value.addAndGet(delta);
        }

        public long getValue() { return value.get(); }
        public String getName() { return name; }
        public Map<String, String> getTags() { return Collections.unmodifiableMap(tags); }
    }

    /**
     * Interface for exporting spans to external systems.
     */
    public interface SpanExporter {
        String getName();
        void export(Span span);
    }

    /**
     * Console-based span exporter for debugging.
     */
    public static class ConsoleSpanExporter implements SpanExporter {
        private final Consumer<String> output;

        public ConsoleSpanExporter() {
            this(System.out::println);
        }

        public ConsoleSpanExporter(Consumer<String> output) {
            this.output = output;
        }

        @Override
        public String getName() {
            return "console";
        }

        @Override
        public void export(Span span) {
            output.accept(String.format("[TRACE] %s | %s | %s | %dms | %s",
                    span.getTraceId(), span.getSpanId(), span.getName(),
                    span.getDurationMs(), span.getStatus()));
        }
    }

    /**
     * In-memory span exporter for testing.
     */
    public static class InMemorySpanExporter implements SpanExporter {
        private final List<Span> exportedSpans = new ArrayList<>();

        @Override
        public String getName() {
            return "in-memory";
        }

        @Override
        public void export(Span span) {
            exportedSpans.add(span);
        }

        public List<Span> getExportedSpans() {
            return Collections.unmodifiableList(exportedSpans);
        }

        public void clear() {
            exportedSpans.clear();
        }
    }
}
