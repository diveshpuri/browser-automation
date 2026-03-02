package com.browserautomation.scriptgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScriptRecordingAgent.
 * Tests the recording wrapper and script generation without requiring a live browser.
 */
class ScriptRecordingAgentTest {

    @Test
    void testGetGenerator() {
        PlaywrightScriptGenerator generator = new PlaywrightScriptGenerator();
        // ScriptRecordingAgent requires an Agent, but we can test the generator directly
        assertNotNull(generator);
    }

    @Test
    void testGenerateScriptFromRecordedActions() {
        PlaywrightScriptGenerator generator = new PlaywrightScriptGenerator();
        generator.recordAction("go_to_url",
                java.util.Map.of("url", "https://github.com"), null, null);
        generator.recordAction("click_element",
                java.util.Map.of("index", 1), "[data-testid=\"search\"]", "https://github.com");
        generator.recordAction("input_text",
                java.util.Map.of("text", "java automation"), "#search-input", "https://github.com");

        String script = generator.generateScript();
        assertTrue(script.contains("github.com"));
        assertTrue(script.contains("search"));
        assertTrue(script.contains("java automation"));
    }

    @Test
    void testGenerateScriptWithShadowDomSelector() {
        PlaywrightScriptGenerator generator = new PlaywrightScriptGenerator();
        // Simulate a shadow DOM piercing selector
        generator.recordAction("click_element",
                java.util.Map.of("index", 1),
                "my-component >> [data-testid=\"inner-btn\"]",
                "https://example.com");

        String script = generator.generateScript();
        assertTrue(script.contains("my-component"));
        assertTrue(script.contains("inner-btn"));
    }

    @Test
    void testRecordedActionCount() {
        PlaywrightScriptGenerator generator = new PlaywrightScriptGenerator();
        assertEquals(0, generator.getRecordedActionCount());
        generator.recordAction("click", java.util.Map.of(), null, null);
        assertEquals(1, generator.getRecordedActionCount());
        generator.recordAction("type", java.util.Map.of(), null, null);
        assertEquals(2, generator.getRecordedActionCount());
    }

    @Test
    void testClearAndRegenerate() {
        PlaywrightScriptGenerator generator = new PlaywrightScriptGenerator();
        generator.recordAction("go_to_url", java.util.Map.of("url", "https://a.com"), null, null);
        generator.clearRecordedActions();
        generator.recordAction("go_to_url", java.util.Map.of("url", "https://b.com"), null, null);

        String script = generator.generateScript();
        assertFalse(script.contains("a.com"));
        assertTrue(script.contains("b.com"));
    }

    @Test
    void testGenerateScriptIncludesDynamicWaits() {
        PlaywrightScriptGenerator generator = new PlaywrightScriptGenerator();
        generator.recordAction("go_to_url", java.util.Map.of("url", "https://example.com"), null, null);
        generator.recordAction("click_element", java.util.Map.of("index", 1), "#btn", null);

        String script = generator.generateScript();
        // Check for dynamic wait mechanisms
        assertTrue(script.contains("waitForDomStable"));
        assertTrue(script.contains("waitForLoadState"));
        assertTrue(script.contains("waitForSelector"));
    }

    @Test
    void testRecordedActionAccessors() {
        PlaywrightScriptGenerator.RecordedAction action = new PlaywrightScriptGenerator.RecordedAction(
                "click", java.util.Map.of("index", 5), "#btn", "https://example.com", 99999L);

        assertEquals("click", action.getActionName());
        assertEquals("#btn", action.getSelector());
        assertEquals("https://example.com", action.getUrl());
        assertEquals(99999L, action.getTimestamp());
        assertEquals(5, action.getParams().get("index"));
    }
}
