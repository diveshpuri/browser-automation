package com.browserautomation.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the custom exception hierarchy.
 */
class ExceptionHierarchyTest {

    @Test
    void testBrowserAutomationExceptionIsRuntimeException() {
        BrowserAutomationException ex = new BrowserAutomationException("test");
        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("test", ex.getMessage());
    }

    @Test
    void testBrowserAutomationExceptionWithCause() {
        Exception cause = new Exception("root cause");
        BrowserAutomationException ex = new BrowserAutomationException("test", cause);
        assertEquals("test", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testBrowserExceptionExtendsBrowserAutomationException() {
        BrowserException ex = new BrowserException("browser error");
        assertInstanceOf(BrowserAutomationException.class, ex);
        assertEquals("browser error", ex.getMessage());
    }

    @Test
    void testBrowserNotStartedException() {
        BrowserNotStartedException ex = new BrowserNotStartedException();
        assertInstanceOf(BrowserException.class, ex);
        assertTrue(ex.getMessage().contains("not started"));
    }

    @Test
    void testBrowserNotStartedExceptionCustomMessage() {
        BrowserNotStartedException ex = new BrowserNotStartedException("custom");
        assertEquals("custom", ex.getMessage());
    }

    @Test
    void testElementNotFoundException() {
        ElementNotFoundException ex = new ElementNotFoundException(5);
        assertInstanceOf(BrowserException.class, ex);
        assertEquals(5, ex.getElementIndex());
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    void testElementNotFoundExceptionWithContext() {
        ElementNotFoundException ex = new ElementNotFoundException(3, "Source");
        assertEquals(3, ex.getElementIndex());
        assertTrue(ex.getMessage().contains("Source"));
        assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    void testLlmException() {
        LlmException ex = new LlmException("provider error");
        assertInstanceOf(BrowserAutomationException.class, ex);
        assertNull(ex.getProvider());
        assertEquals(-1, ex.getStatusCode());
    }

    @Test
    void testLlmExceptionWithProvider() {
        LlmException ex = new LlmException("OpenAI", "rate limited");
        assertEquals("OpenAI", ex.getProvider());
        assertTrue(ex.getMessage().contains("OpenAI"));
    }

    @Test
    void testLlmExceptionWithStatusCode() {
        LlmException ex = new LlmException("Anthropic", 429, "rate limited");
        assertEquals("Anthropic", ex.getProvider());
        assertEquals(429, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("429"));
    }

    @Test
    void testActionException() {
        ActionException ex = new ActionException("click", "element not found");
        assertInstanceOf(BrowserAutomationException.class, ex);
        assertEquals("click", ex.getActionName());
        assertTrue(ex.getMessage().contains("click"));
    }

    @Test
    void testActionExceptionWithCause() {
        RuntimeException cause = new RuntimeException("timeout");
        ActionException ex = new ActionException("navigate", "failed", cause);
        assertEquals("navigate", ex.getActionName());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testAgentException() {
        AgentException ex = new AgentException("too many failures");
        assertInstanceOf(BrowserAutomationException.class, ex);
        assertEquals(-1, ex.getStepNumber());
    }

    @Test
    void testAgentExceptionWithStep() {
        AgentException ex = new AgentException("timeout", 5);
        assertEquals(5, ex.getStepNumber());
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    void testConfigurationException() {
        ConfigurationException ex = new ConfigurationException("missing config");
        assertInstanceOf(BrowserAutomationException.class, ex);
        assertNull(ex.getConfigKey());
    }

    @Test
    void testConfigurationExceptionWithKey() {
        ConfigurationException ex = new ConfigurationException("OPENAI_API_KEY", "is required");
        assertEquals("OPENAI_API_KEY", ex.getConfigKey());
        assertTrue(ex.getMessage().contains("OPENAI_API_KEY"));
    }

    @Test
    void testTimeoutException() {
        TimeoutException ex = new TimeoutException("timed out");
        assertInstanceOf(BrowserAutomationException.class, ex);
        assertEquals(-1, ex.getTimeoutMs());
    }

    @Test
    void testTimeoutExceptionWithDuration() {
        TimeoutException ex = new TimeoutException("navigation", 30000);
        assertEquals(30000, ex.getTimeoutMs());
        assertTrue(ex.getMessage().contains("30000"));
        assertTrue(ex.getMessage().contains("navigation"));
    }
}
