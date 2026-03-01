package com.browserautomation.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CLI argument parser and configuration.
 */
class BrowserAutomationCliTest {

    @Test
    void testDefaultValues() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{});

        assertEquals("openai", cli.getProvider());
        assertEquals("gpt-4o", cli.getModel());
        assertTrue(cli.isHeadless());
        assertTrue(cli.isUseVision());
        assertEquals(50, cli.getMaxSteps());
        assertNull(cli.getTask());
        assertTrue(cli.isInteractive()); // Default to interactive when no task
    }

    @Test
    void testParseTask() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--task", "Search for flights"});

        assertEquals("Search for flights", cli.getTask());
        assertFalse(cli.isInteractive());
    }

    @Test
    void testParseProvider() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--provider", "gemini", "--model", "gemini-2.0-flash-exp"});

        assertEquals("gemini", cli.getProvider());
        assertEquals("gemini-2.0-flash-exp", cli.getModel());
    }

    @Test
    void testParseHeadlessFlags() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--no-headless"});
        assertFalse(cli.isHeadless());

        cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--headless"});
        assertTrue(cli.isHeadless());
    }

    @Test
    void testParseVisionFlags() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--no-vision"});
        assertFalse(cli.isUseVision());

        cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--vision"});
        assertTrue(cli.isUseVision());
    }

    @Test
    void testParseMaxSteps() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--max-steps", "100"});

        assertEquals(100, cli.getMaxSteps());
    }

    @Test
    void testParseInteractive() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--interactive"});

        assertTrue(cli.isInteractive());
    }

    @Test
    void testParseAllOptions() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{
                "--provider", "anthropic",
                "--model", "claude-sonnet-4-20250514",
                "--no-headless",
                "--no-vision",
                "--max-steps", "25",
                "--task", "Find weather in NYC"
        });

        assertEquals("anthropic", cli.getProvider());
        assertEquals("claude-sonnet-4-20250514", cli.getModel());
        assertFalse(cli.isHeadless());
        assertFalse(cli.isUseVision());
        assertEquals(25, cli.getMaxSteps());
        assertEquals("Find weather in NYC", cli.getTask());
    }

    @Test
    void testTaskWithoutFlag() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"Search for Java tutorials"});

        assertEquals("Search for Java tutorials", cli.getTask());
    }

    @Test
    void testInteractiveWithTask() {
        BrowserAutomationCli cli = new BrowserAutomationCli();
        cli.parseArgs(new String[]{"--task", "Find flights", "--interactive"});

        assertEquals("Find flights", cli.getTask());
        assertTrue(cli.isInteractive());
    }
}
