package com.browserautomation.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrowserEventsTest {

    @Test
    void testNavigateToUrlEvent() {
        var event = new BrowserEvents.NavigateToUrlEvent("https://example.com");
        assertEquals("NavigateToUrlEvent", event.getEventType());
        assertEquals("https://example.com", event.getUrl());
        assertNotNull(event.getEventId());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void testClickElementEvent() {
        var event = new BrowserEvents.ClickElementEvent(5);
        assertEquals("ClickElementEvent", event.getEventType());
        assertEquals(5, event.getElementIndex());
    }

    @Test
    void testTypeTextEvent() {
        var event = new BrowserEvents.TypeTextEvent(3, "hello");
        assertEquals("TypeTextEvent", event.getEventType());
        assertEquals(3, event.getElementIndex());
        assertEquals("hello", event.getText());
    }

    @Test
    void testScrollEvent() {
        var event = new BrowserEvents.ScrollEvent(true, 500);
        assertEquals("ScrollEvent", event.getEventType());
        assertTrue(event.isDown());
        assertEquals(500, event.getPixels());
    }

    @Test
    void testAgentStartedEvent() {
        var event = new BrowserEvents.AgentStartedEvent("Find stars", 10);
        assertEquals("Find stars", event.getTask());
        assertEquals(10, event.getMaxSteps());
    }

    @Test
    void testAgentCompletedEvent() {
        var event = new BrowserEvents.AgentCompletedEvent("Success", 5);
        assertEquals("Success", event.getResult());
        assertEquals(5, event.getTotalSteps());
    }

    @Test
    void testAgentErrorEvent() {
        var event = new BrowserEvents.AgentErrorEvent("timeout", 3);
        assertEquals("timeout", event.getError());
        assertEquals(3, event.getStepNumber());
    }

    @Test
    void testCaptchaDetectedEvent() {
        var event = new BrowserEvents.CaptchaDetectedEvent("https://site.com", "reCAPTCHA");
        assertEquals("https://site.com", event.getUrl());
        assertEquals("reCAPTCHA", event.getCaptchaType());
    }

    @Test
    void testDownloadEvents() {
        var started = new BrowserEvents.DownloadStartedEvent("https://file.com/doc.pdf", "doc.pdf");
        assertEquals("https://file.com/doc.pdf", started.getUrl());
        assertEquals("doc.pdf", started.getSuggestedFilename());

        var completed = new BrowserEvents.DownloadCompletedEvent("/tmp/doc.pdf");
        assertEquals("/tmp/doc.pdf", completed.getPath());
    }

    @Test
    void testLlmEvents() {
        var request = new BrowserEvents.LlmRequestEvent("openai", "gpt-4o", 500);
        assertEquals("openai", request.getProvider());
        assertEquals("gpt-4o", request.getModel());
        assertEquals(500, request.getInputTokens());

        var response = new BrowserEvents.LlmResponseEvent("openai", 200, 1500);
        assertEquals(200, response.getOutputTokens());
        assertEquals(1500, response.getLatencyMs());

        var error = new BrowserEvents.LlmErrorEvent("openai", "rate limit", true);
        assertEquals("rate limit", error.getError());
        assertTrue(error.isWillFallback());
    }

    @Test
    void testMessageCompactedEvent() {
        var event = new BrowserEvents.MessageCompactedEvent(20, 5, 3000);
        assertEquals(20, event.getOriginalCount());
        assertEquals(5, event.getCompactedCount());
        assertEquals(3000, event.getTokensSaved());
    }

    @Test
    void testLoopDetectedEvent() {
        var event = new BrowserEvents.LoopDetectedEvent(3, "click");
        assertEquals(3, event.getLoopCount());
        assertEquals("click", event.getLastAction());
    }

    @Test
    void testSecurityViolationEvent() {
        var event = new BrowserEvents.SecurityViolationEvent("Non-HTTPS", "http://site.com");
        assertEquals("Non-HTTPS", event.getViolation());
        assertEquals("http://site.com", event.getUrl());
    }

    @Test
    void testEventDataAccess() {
        var event = new BrowserEvents.NavigateToUrlEvent("https://example.com");
        assertEquals("https://example.com", event.get("url"));
        assertNull(event.get("nonexistent"));
        assertEquals("default", event.get("nonexistent", "default"));
    }

    @Test
    void testEventToString() {
        var event = new BrowserEvents.ClickElementEvent(5);
        String str = event.toString();
        assertTrue(str.contains("ClickElementEvent"));
        assertTrue(str.contains("elementIndex"));
    }

    @Test
    void testGoBackEvent() {
        var event = new BrowserEvents.GoBackEvent();
        assertEquals("GoBackEvent", event.getEventType());
    }

    @Test
    void testSwitchTabEvent() {
        var event = new BrowserEvents.SwitchTabEvent(2);
        assertEquals(2, event.getTabIndex());
    }

    @Test
    void testOpenNewTabEvent() {
        var event = new BrowserEvents.OpenNewTabEvent("https://new.com");
        assertEquals("https://new.com", event.getUrl());
    }

    @Test
    void testDomStateExtractedEvent() {
        var event = new BrowserEvents.DomStateExtractedEvent(42, "https://page.com");
        assertEquals(42, event.getElementCount());
        assertEquals("https://page.com", event.getUrl());
    }

    @Test
    void testHoverElementEvent() {
        var event = new BrowserEvents.HoverElementEvent(7);
        assertEquals(7, event.getElementIndex());
    }

    @Test
    void testDragAndDropEvent() {
        var event = new BrowserEvents.DragAndDropEvent(1, 5);
        assertEquals(1, event.getSourceIndex());
        assertEquals(5, event.getTargetIndex());
    }

    @Test
    void testSendKeysEvent() {
        var event = new BrowserEvents.SendKeysEvent("Enter");
        assertEquals("Enter", event.getKeys());
    }

    @Test
    void testUploadFileEvent() {
        var event = new BrowserEvents.UploadFileEvent(3, "/tmp/file.txt");
        assertEquals(3, event.getElementIndex());
        assertEquals("/tmp/file.txt", event.getFilePath());
    }

    @Test
    void testClickDropdownEvent() {
        var event = new BrowserEvents.ClickDropdownEvent(2, "option1");
        assertEquals(2, event.getElementIndex());
        assertEquals("option1", event.getValue());
    }
}
