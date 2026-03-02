package com.browserautomation.agent.output;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructuredOutputActionTest {

    public static class FlightResult {
        public String airline;
        public double price;
        public int stops;
        public String departure;
    }

    public static class SimpleResult {
        public String name;
        public int count;
    }

    @Test
    void testParseValidJson() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        SimpleResult result = action.parse("{\"name\": \"test\", \"count\": 42}");

        assertEquals("test", result.name);
        assertEquals(42, result.count);
    }

    @Test
    void testParseFromMarkdownCodeBlock() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        SimpleResult result = action.parse("```json\n{\"name\": \"test\", \"count\": 5}\n```");

        assertEquals("test", result.name);
        assertEquals(5, result.count);
    }

    @Test
    void testParseExtractsJsonFromText() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        SimpleResult result = action.parse("Here is the result: {\"name\": \"extracted\", \"count\": 10}");

        assertEquals("extracted", result.name);
        assertEquals(10, result.count);
    }

    @Test
    void testParseInvalidJson() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        assertThrows(StructuredOutputAction.StructuredOutputException.class,
                () -> action.parse("not json at all"));
    }

    @Test
    void testParseEmptyInput() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        assertThrows(StructuredOutputAction.StructuredOutputException.class,
                () -> action.parse(""));
    }

    @Test
    void testValidateValid() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        List<String> errors = action.validate("{\"name\": \"test\", \"count\": 5}");
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateInvalid() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        List<String> errors = action.validate("not json");
        assertFalse(errors.isEmpty());
    }

    @Test
    void testValidateNullInput() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        List<String> errors = action.validate(null);
        assertFalse(errors.isEmpty());
    }

    @Test
    void testGetJsonSchema() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        Map<String, Object> schema = action.getJsonSchema();

        assertEquals("object", schema.get("type"));
        assertNotNull(schema.get("properties"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertTrue(properties.containsKey("name"));
        assertTrue(properties.containsKey("count"));
    }

    @Test
    void testGetToolDefinition() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(
                SimpleResult.class, "Get a simple result");
        Map<String, Object> toolDef = action.getToolDefinition();

        assertEquals("function", toolDef.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) toolDef.get("function");
        assertEquals("structured_output", function.get("name"));
        assertEquals("Get a simple result", function.get("description"));
    }

    @Test
    void testGetOutputType() {
        StructuredOutputAction<SimpleResult> action = new StructuredOutputAction<>(SimpleResult.class);
        assertEquals(SimpleResult.class, action.getOutputType());
    }

    @Test
    void testFlightResultSchema() {
        StructuredOutputAction<FlightResult> action = new StructuredOutputAction<>(FlightResult.class);
        Map<String, Object> schema = action.getJsonSchema();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertTrue(properties.containsKey("airline"));
        assertTrue(properties.containsKey("price"));
        assertTrue(properties.containsKey("stops"));
    }

    @Test
    void testParseFlightResult() {
        StructuredOutputAction<FlightResult> action = new StructuredOutputAction<>(FlightResult.class);
        FlightResult result = action.parse(
                "{\"airline\": \"Delta\", \"price\": 499.99, \"stops\": 1, \"departure\": \"10:30\"}");

        assertEquals("Delta", result.airline);
        assertEquals(499.99, result.price, 0.01);
        assertEquals(1, result.stops);
        assertEquals("10:30", result.departure);
    }
}
