package com.browserautomation.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObservabilityServiceTest {

    private ObservabilityService service;

    @BeforeEach
    void setUp() {
        service = new ObservabilityService("test-service");
    }

    @Test
    void testStartAndEndSpan() {
        ObservabilityService.Span span = service.startSpan("test.operation");

        assertNotNull(span);
        assertNotNull(span.getSpanId());
        assertNotNull(span.getTraceId());
        assertEquals("test.operation", span.getName());
        assertEquals("test-service", span.getServiceName());

        service.endSpan(span);

        assertNotNull(span.getEndTime());
        assertTrue(span.getDurationMs() >= 0);
        assertEquals(1, service.getCompletedSpans().size());
    }

    @Test
    void testChildSpan() {
        ObservabilityService.Span parent = service.startSpan("parent");
        ObservabilityService.Span child = service.startSpan("child", parent.getSpanId());

        assertEquals(parent.getTraceId(), child.getTraceId());
        assertEquals(parent.getSpanId(), child.getParentId());

        service.endSpan(child);
        service.endSpan(parent);

        assertEquals(2, service.getCompletedSpans().size());
    }

    @Test
    void testSpanAttributes() {
        ObservabilityService.Span span = service.startSpan("test");
        span.setAttribute("url", "https://example.com");
        span.setAttribute("action", "click");

        assertEquals("https://example.com", span.getAttributes().get("url"));
        assertEquals("click", span.getAttributes().get("action"));

        service.endSpan(span);
    }

    @Test
    void testSpanEvents() {
        ObservabilityService.Span span = service.startSpan("test");
        span.addEvent("page.loaded");
        span.addEvent("element.clicked", Map.of("index", "5"));

        assertEquals(2, span.getEvents().size());
        assertEquals("page.loaded", span.getEvents().get(0).name());

        service.endSpan(span);
    }

    @Test
    void testSpanStatus() {
        ObservabilityService.Span span = service.startSpan("test");
        span.setStatus(ObservabilityService.SpanStatus.ERROR, "Element not found");

        assertEquals(ObservabilityService.SpanStatus.ERROR, span.getStatus());
        assertEquals("Element not found", span.getStatusMessage());

        service.endSpan(span);
    }

    @Test
    void testRecordMetric() {
        service.recordMetric("tokens.used", 150);
        service.recordMetric("tokens.used", 200);

        assertEquals(350, service.getMetricValue("tokens.used"));
    }

    @Test
    void testRecordMetricWithTags() {
        service.recordMetric("api.calls", 1, Map.of("provider", "openai"));
        service.recordMetric("api.calls", 1, Map.of("provider", "anthropic"));

        assertFalse(service.getMetrics().isEmpty());
    }

    @Test
    void testGlobalAttributes() {
        service.setGlobalAttribute("session.id", "abc123");

        ObservabilityService.Span span = service.startSpan("test");
        assertEquals("abc123", span.getAttributes().get("session.id"));

        service.endSpan(span);
    }

    @Test
    void testSpanExporter() {
        ObservabilityService.InMemorySpanExporter exporter = new ObservabilityService.InMemorySpanExporter();
        service.registerExporter(exporter);

        ObservabilityService.Span span = service.startSpan("test");
        service.endSpan(span);

        assertEquals(1, exporter.getExportedSpans().size());
        assertEquals("test", exporter.getExportedSpans().get(0).getName());
    }

    @Test
    void testConsoleExporter() {
        StringBuilder output = new StringBuilder();
        ObservabilityService.ConsoleSpanExporter exporter =
                new ObservabilityService.ConsoleSpanExporter(output::append);
        service.registerExporter(exporter);

        assertEquals("console", exporter.getName());

        ObservabilityService.Span span = service.startSpan("test.op");
        service.endSpan(span);

        assertTrue(output.toString().contains("test.op"));
    }

    @Test
    void testDisabled() {
        service.setEnabled(false);

        ObservabilityService.Span span = service.startSpan("test");
        assertEquals(ObservabilityService.Span.NOOP, span);

        service.endSpan(span);
        assertTrue(service.getCompletedSpans().isEmpty());

        service.recordMetric("test", 100);
        assertEquals(0, service.getMetricValue("test"));
    }

    @Test
    void testClear() {
        ObservabilityService.Span span = service.startSpan("test");
        service.endSpan(span);
        service.recordMetric("test", 100);

        service.clear();

        assertTrue(service.getCompletedSpans().isEmpty());
        assertTrue(service.getActiveSpans().isEmpty());
        assertEquals(0, service.getMetricValue("test"));
    }

    @Test
    void testActiveSpans() {
        ObservabilityService.Span span = service.startSpan("active");

        assertFalse(service.getActiveSpans().isEmpty());
        assertTrue(service.getActiveSpans().containsKey(span.getSpanId()));

        service.endSpan(span);

        assertTrue(service.getActiveSpans().isEmpty());
    }

    @Test
    void testSpanToString() {
        ObservabilityService.Span span = service.startSpan("test");
        service.endSpan(span);

        String str = span.toString();
        assertTrue(str.contains("test"));
        assertTrue(str.contains("OK"));
    }

    @Test
    void testNoopSpan() {
        ObservabilityService.Span noop = ObservabilityService.Span.NOOP;
        assertEquals("noop", noop.getSpanId());
        assertEquals("noop", noop.getName());
    }

    @Test
    void testMetricCounter() {
        ObservabilityService.MetricCounter counter =
                new ObservabilityService.MetricCounter("test.metric", Map.of("env", "test"));

        counter.increment(10);
        counter.increment(5);

        assertEquals(15, counter.getValue());
        assertEquals("test.metric", counter.getName());
        assertEquals("test", counter.getTags().get("env"));
    }

    @Test
    void testInMemoryExporterClear() {
        ObservabilityService.InMemorySpanExporter exporter = new ObservabilityService.InMemorySpanExporter();
        assertEquals("in-memory", exporter.getName());

        service.registerExporter(exporter);
        ObservabilityService.Span span = service.startSpan("test");
        service.endSpan(span);

        assertEquals(1, exporter.getExportedSpans().size());
        exporter.clear();
        assertTrue(exporter.getExportedSpans().isEmpty());
    }
}
