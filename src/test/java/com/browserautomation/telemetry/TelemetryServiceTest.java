package com.browserautomation.telemetry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TelemetryService.
 */
class TelemetryServiceTest {

    private TelemetryService service;

    @BeforeEach
    void setUp() {
        service = new TelemetryService();
    }

    @AfterEach
    void tearDown() {
        service.close();
    }

    @Test
    void testDefaultConstructorEnabled() {
        assertTrue(service.isEnabled());
    }

    @Test
    void testCustomConstructor() {
        TelemetryService custom = new TelemetryService(false, 100);
        assertFalse(custom.isEnabled());
        custom.close();
    }

    @Test
    void testEnableDisable() {
        service.disable();
        assertFalse(service.isEnabled());
        service.enable();
        assertTrue(service.isEnabled());
    }

    @Test
    void testRecordEvent() {
        service.recordEvent("agent", "step", "label1", 1.0);
        List<TelemetryService.TelemetryEvent> events = service.getEvents();
        assertEquals(1, events.size());
        assertEquals("agent", events.get(0).getCategory());
        assertEquals("step", events.get(0).getAction());
        assertEquals("label1", events.get(0).getLabel());
        assertEquals(1.0, events.get(0).getValue());
    }

    @Test
    void testRecordSimpleEvent() {
        service.recordEvent("browser", "navigate");
        List<TelemetryService.TelemetryEvent> events = service.getEvents();
        assertEquals(1, events.size());
        assertEquals("browser", events.get(0).getCategory());
        assertNull(events.get(0).getLabel());
    }

    @Test
    void testRecordEventWhenDisabled() {
        service.disable();
        service.recordEvent("agent", "step", "label", 1.0);
        assertTrue(service.getEvents().isEmpty());
    }

    @Test
    void testIncrementCounter() {
        service.incrementCounter("actions");
        service.incrementCounter("actions");
        service.incrementCounter("actions");
        assertEquals(3, service.getCounter("actions"));
    }

    @Test
    void testIncrementCounterByAmount() {
        service.incrementCounter("tokens", 100);
        service.incrementCounter("tokens", 50);
        assertEquals(150, service.getCounter("tokens"));
    }

    @Test
    void testGetCounterNonExistent() {
        assertEquals(0, service.getCounter("missing"));
    }

    @Test
    void testIncrementCounterWhenDisabled() {
        service.disable();
        service.incrementCounter("test");
        assertEquals(0, service.getCounter("test"));
    }

    @Test
    void testRecordMetric() {
        service.recordMetric("latency", 100.0);
        service.recordMetric("latency", 200.0);
        service.recordMetric("latency", 300.0);
        assertEquals(200.0, service.getMetricAverage("latency"), 0.001);
    }

    @Test
    void testGetMetricMin() {
        service.recordMetric("time", 50.0);
        service.recordMetric("time", 10.0);
        service.recordMetric("time", 80.0);
        assertEquals(10.0, service.getMetricMin("time"), 0.001);
    }

    @Test
    void testGetMetricMax() {
        service.recordMetric("time", 50.0);
        service.recordMetric("time", 10.0);
        service.recordMetric("time", 80.0);
        assertEquals(80.0, service.getMetricMax("time"), 0.001);
    }

    @Test
    void testGetMetricAverageNonExistent() {
        assertEquals(0.0, service.getMetricAverage("missing"), 0.001);
    }

    @Test
    void testGetMetricMinNonExistent() {
        assertEquals(0.0, service.getMetricMin("missing"), 0.001);
    }

    @Test
    void testGetMetricMaxNonExistent() {
        assertEquals(0.0, service.getMetricMax("missing"), 0.001);
    }

    @Test
    void testRecordMetricWhenDisabled() {
        service.disable();
        service.recordMetric("test", 42.0);
        assertEquals(0.0, service.getMetricAverage("test"), 0.001);
    }

    @Test
    void testRecordLlmCall() {
        service.recordLlmCall("openai", "gpt-4o", 500, 200, 1200);
        assertEquals(1, service.getCounter("llm.total_calls"));
        assertEquals(500, service.getCounter("llm.total_input_tokens"));
        assertEquals(200, service.getCounter("llm.total_output_tokens"));
    }

    @Test
    void testRecordBrowserAction() {
        service.recordBrowserAction("click", true, 150);
        service.recordBrowserAction("type", false, 200);
        assertEquals(2, service.getCounter("browser.total_actions"));
        assertEquals(1, service.getCounter("browser.successful_actions"));
    }

    @Test
    void testGetEventsByCategory() {
        service.recordEvent("agent", "step");
        service.recordEvent("browser", "click");
        service.recordEvent("agent", "done");
        List<TelemetryService.TelemetryEvent> agentEvents = service.getEventsByCategory("agent");
        assertEquals(2, agentEvents.size());
    }

    @Test
    void testGetCounters() {
        service.incrementCounter("a");
        service.incrementCounter("b", 5);
        Map<String, Long> counters = service.getCounters();
        assertEquals(1L, counters.get("a"));
        assertEquals(5L, counters.get("b"));
    }

    @Test
    void testGetSummary() {
        service.incrementCounter("test.counter");
        service.recordMetric("test.metric", 42.0);
        String summary = service.getSummary();
        assertTrue(summary.contains("Telemetry Summary"));
        assertTrue(summary.contains("Counters"));
        assertTrue(summary.contains("Metrics"));
    }

    @Test
    void testReset() {
        service.recordEvent("cat", "act");
        service.incrementCounter("c");
        service.recordMetric("m", 1.0);
        service.reset();
        assertTrue(service.getEvents().isEmpty());
        assertEquals(0, service.getCounter("c"));
        assertEquals(0.0, service.getMetricAverage("m"), 0.001);
    }

    @Test
    void testEventMaxSize() {
        TelemetryService small = new TelemetryService(true, 3);
        small.recordEvent("a", "1");
        small.recordEvent("a", "2");
        small.recordEvent("a", "3");
        small.recordEvent("a", "4");
        assertEquals(3, small.getEvents().size());
        small.close();
    }

    @Test
    void testTelemetryEventToString() {
        TelemetryService.TelemetryEvent event = new TelemetryService.TelemetryEvent(
                "cat", "act", "lbl", 1.5, System.currentTimeMillis());
        String str = event.toString();
        assertTrue(str.contains("cat"));
        assertTrue(str.contains("act"));
        assertTrue(str.contains("lbl"));
    }

    @Test
    void testTelemetryEventTimestamp() {
        long before = System.currentTimeMillis();
        service.recordEvent("cat", "act");
        long after = System.currentTimeMillis();
        TelemetryService.TelemetryEvent event = service.getEvents().get(0);
        assertTrue(event.getTimestamp() >= before && event.getTimestamp() <= after);
    }
}
