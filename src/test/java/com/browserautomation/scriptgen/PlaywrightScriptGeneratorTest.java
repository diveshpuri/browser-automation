package com.browserautomation.scriptgen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlaywrightScriptGenerator.
 */
class PlaywrightScriptGeneratorTest {

    private PlaywrightScriptGenerator generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        generator = new PlaywrightScriptGenerator();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(generator);
        assertEquals(0, generator.getRecordedActionCount());
    }

    @Test
    void testCustomConfigConstructor() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig()
                .viewportWidth(1920)
                .viewportHeight(1080);
        PlaywrightScriptGenerator gen = new PlaywrightScriptGenerator(config);
        assertNotNull(gen);
    }

    @Test
    void testRecordAction() {
        generator.recordAction("click", Map.of("index", 1), "#btn", "https://example.com");
        assertEquals(1, generator.getRecordedActionCount());
    }

    @Test
    void testRecordMultipleActions() {
        generator.recordAction("navigate", Map.of("url", "https://example.com"), null, null);
        generator.recordAction("click", Map.of("index", 1), "#btn", "https://example.com");
        generator.recordAction("type", Map.of("text", "hello"), "#input", "https://example.com");
        assertEquals(3, generator.getRecordedActionCount());
    }

    @Test
    void testClearRecordedActions() {
        generator.recordAction("click", Map.of(), null, null);
        generator.clearRecordedActions();
        assertEquals(0, generator.getRecordedActionCount());
    }

    @Test
    void testGenerateScriptContainsImports() {
        String script = generator.generateScript();
        assertTrue(script.contains("import { test, expect, Page, BrowserContext }"));
        assertTrue(script.contains("@playwright/test"));
    }

    @Test
    void testGenerateScriptContainsTestSetup() {
        String script = generator.generateScript();
        assertTrue(script.contains("test.describe"));
        assertTrue(script.contains("test.beforeEach"));
        assertTrue(script.contains("test.afterEach"));
    }

    @Test
    void testGenerateScriptContainsHelperFunctions() {
        String script = generator.generateScript();
        assertTrue(script.contains("waitForDomStable"));
        assertTrue(script.contains("MutationObserver"));
        assertTrue(script.contains("retryWithBackoff"));
        assertTrue(script.contains("waitForNetworkIdle"));
    }

    @Test
    void testGenerateScriptWithNavigateAction() {
        generator.recordAction("go_to_url", Map.of("url", "https://example.com"), null, null);
        String script = generator.generateScript();
        assertTrue(script.contains("page.goto"));
        assertTrue(script.contains("example.com"));
        assertTrue(script.contains("waitUntil"));
        assertTrue(script.contains("networkidle"));
    }

    @Test
    void testGenerateScriptWithClickAction() {
        generator.recordAction("click_element", Map.of("index", 5), "#submit-btn", "https://example.com");
        String script = generator.generateScript();
        assertTrue(script.contains("waitForSelector"));
        assertTrue(script.contains("submit-btn"));
        assertTrue(script.contains(".click("));
    }

    @Test
    void testGenerateScriptWithTypeAction() {
        generator.recordAction("input_text", Map.of("text", "hello world"), "#search", "https://example.com");
        String script = generator.generateScript();
        assertTrue(script.contains("fill("));
        assertTrue(script.contains("hello world"));
    }

    @Test
    void testGenerateScriptWithScrollAction() {
        generator.recordAction("scroll_down", Map.of("amount", 500), null, null);
        String script = generator.generateScript();
        assertTrue(script.contains("scrollBy"));
    }

    @Test
    void testGenerateScriptWithGoBackAction() {
        generator.recordAction("go_back", Map.of(), null, null);
        String script = generator.generateScript();
        assertTrue(script.contains("goBack"));
    }

    @Test
    void testGenerateScriptWithSendKeysAction() {
        generator.recordAction("send_keys", Map.of("keys", "Enter"), null, null);
        String script = generator.generateScript();
        assertTrue(script.contains("keyboard.press"));
        assertTrue(script.contains("Enter"));
    }

    @Test
    void testGenerateScriptWithHoverAction() {
        generator.recordAction("hover_element", Map.of("index", 3), ".menu-item", null);
        String script = generator.generateScript();
        assertTrue(script.contains(".hover()"));
    }

    @Test
    void testGenerateScriptWithTabActions() {
        generator.recordAction("open_tab", Map.of("url", "https://new-tab.com"), null, null);
        String script = generator.generateScript();
        assertTrue(script.contains("context.newPage()"));
    }

    @Test
    void testGenerateScriptWithDoneAction() {
        generator.recordAction("done", Map.of("text", "Task complete"), null, null);
        String script = generator.generateScript();
        assertTrue(script.contains("Task completed") || script.contains("Task complete"));
    }

    @Test
    void testGenerateScriptWithGenericAction() {
        generator.recordAction("custom_action", Map.of("param", "val"), null, null);
        String script = generator.generateScript();
        assertTrue(script.contains("Unknown action: custom_action"));
    }

    @Test
    void testGenerateAndSaveToFile() throws IOException {
        generator.recordAction("go_to_url", Map.of("url", "https://example.com"), null, null);
        Path output = tempDir.resolve("test.spec.ts");
        Path saved = generator.generateAndSave(output);
        assertTrue(Files.exists(saved));
        String content = Files.readString(saved);
        assertTrue(content.contains("@playwright/test"));
        assertTrue(content.contains("example.com"));
    }

    @Test
    void testGenerateScriptWithViewportConfig() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig()
                .viewportWidth(1920)
                .viewportHeight(1080);
        PlaywrightScriptGenerator gen = new PlaywrightScriptGenerator(config);
        String script = gen.generateScript();
        assertTrue(script.contains("1920"));
        assertTrue(script.contains("1080"));
    }

    @Test
    void testGenerateScriptWithUserAgent() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig()
                .userAgent("CustomAgent/1.0");
        PlaywrightScriptGenerator gen = new PlaywrightScriptGenerator(config);
        String script = gen.generateScript();
        assertTrue(script.contains("CustomAgent/1.0"));
    }

    @Test
    void testRecordedActionProperties() {
        PlaywrightScriptGenerator.RecordedAction action = new PlaywrightScriptGenerator.RecordedAction(
                "click", Map.of("index", 1), "#btn", "https://example.com", 12345L);
        assertEquals("click", action.getActionName());
        assertEquals("#btn", action.getSelector());
        assertEquals("https://example.com", action.getUrl());
        assertEquals(12345L, action.getTimestamp());
        assertNotNull(action.getParams());
    }

    @Test
    void testGenerateScriptWithSelectAction() {
        generator.recordAction("select_dropdown_option", Map.of("value", "option1"), "#dropdown", null);
        String script = generator.generateScript();
        assertTrue(script.contains("selectOption"));
        assertTrue(script.contains("option1"));
    }

    @Test
    void testGenerateScriptWithMouseMoveAction() {
        generator.recordAction("mouse_move", Map.of("x", 100.0, "y", 200.0), null, null);
        String script = generator.generateScript();
        assertTrue(script.contains("mouse.move"));
    }
}
