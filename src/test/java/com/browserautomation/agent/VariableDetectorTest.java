package com.browserautomation.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VariableDetector.
 */
class VariableDetectorTest {

    @Test
    void testDetectPlaceholders() {
        VariableDetector detector = new VariableDetector();
        Map<String, String> vars = detector.detectVariables("Hello ${name}, your id is {userId}");
        assertTrue(vars.containsKey("name"));
        assertTrue(vars.containsKey("userId"));
    }

    @Test
    void testDetectUrls() {
        VariableDetector detector = new VariableDetector();
        Map<String, String> vars = detector.detectVariables("Visit https://example.com for more info");
        assertTrue(vars.containsKey("url"));
        assertEquals("https://example.com", vars.get("url"));
    }

    @Test
    void testDetectEmails() {
        VariableDetector detector = new VariableDetector();
        Map<String, String> vars = detector.detectVariables("Contact user@example.com");
        assertTrue(vars.containsKey("email"));
        assertEquals("user@example.com", vars.get("email"));
    }

    @Test
    void testDetectAssignments() {
        VariableDetector detector = new VariableDetector();
        Map<String, String> vars = detector.detectVariables("set the username to john_doe");
        assertFalse(vars.isEmpty());
    }

    @Test
    void testSubstituteVariables() {
        VariableDetector detector = new VariableDetector();
        String result = detector.substituteVariables("Hello ${name}!", Map.of("name", "World"));
        assertEquals("Hello World!", result);
    }

    @Test
    void testSubstituteBraceVariables() {
        VariableDetector detector = new VariableDetector();
        String result = detector.substituteVariables("Hello {name}!", Map.of("name", "World"));
        assertEquals("Hello World!", result);
    }

    @Test
    void testExtractKeyValuePairs() {
        VariableDetector detector = new VariableDetector();
        Map<String, String> pairs = detector.extractKeyValuePairs("name: John, age: 30");
        assertEquals("John", pairs.get("name"));
    }

    @Test
    void testGetPlaceholderNames() {
        VariableDetector detector = new VariableDetector();
        List<String> names = detector.getPlaceholderNames("${first} and {second} then ${first}");
        assertEquals(2, names.size());
        assertTrue(names.contains("first"));
        assertTrue(names.contains("second"));
    }

    @Test
    void testSetAndGetVariable() {
        VariableDetector detector = new VariableDetector();
        detector.setVariable("key", "value");
        assertEquals("value", detector.getVariable("key"));
    }

    @Test
    void testClear() {
        VariableDetector detector = new VariableDetector();
        detector.setVariable("key", "value");
        detector.clear();
        assertNull(detector.getVariable("key"));
        assertTrue(detector.getAllDetectedVariables().isEmpty());
    }

    @Test
    void testNullInput() {
        VariableDetector detector = new VariableDetector();
        Map<String, String> result = detector.detectVariables(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testEmptyInput() {
        VariableDetector detector = new VariableDetector();
        Map<String, String> result = detector.detectVariables("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testSubstituteNullTemplate() {
        VariableDetector detector = new VariableDetector();
        assertNull(detector.substituteVariables(null, Map.of()));
    }
}
