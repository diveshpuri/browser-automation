package com.browserautomation.browser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DemoMode.
 */
class DemoModeTest {

    @Test
    void testDefaultSettings() {
        DemoMode demo = new DemoMode();
        assertFalse(demo.isEnabled());
        assertEquals(1000, demo.getSlowMotionMs());
        assertEquals("#FF6B35", demo.getHighlightColor());
        assertEquals("3px", demo.getHighlightBorderWidth());
        assertEquals(800, demo.getHighlightDurationMs());
        assertTrue(demo.isShowActionLabels());
        assertTrue(demo.isShowTooltips());
    }

    @Test
    void testEnable() {
        DemoMode demo = new DemoMode().enable();
        assertTrue(demo.isEnabled());
    }

    @Test
    void testDisable() {
        DemoMode demo = new DemoMode().enable().disable();
        assertFalse(demo.isEnabled());
    }

    @Test
    void testFluentConfiguration() {
        DemoMode demo = new DemoMode()
                .enable()
                .slowMotion(500)
                .highlightColor("#00FF00")
                .highlightBorderWidth("5px")
                .highlightDuration(1000)
                .showActionLabels(false)
                .showTooltips(false);

        assertTrue(demo.isEnabled());
        assertEquals(500, demo.getSlowMotionMs());
        assertEquals("#00FF00", demo.getHighlightColor());
        assertEquals("5px", demo.getHighlightBorderWidth());
        assertEquals(1000, demo.getHighlightDurationMs());
        assertFalse(demo.isShowActionLabels());
        assertFalse(demo.isShowTooltips());
    }

    @Test
    void testHighlightElementWhenDisabled() {
        DemoMode demo = new DemoMode(); // disabled by default
        // Should not throw when disabled, just no-op
        assertDoesNotThrow(() -> demo.highlightElement(null, "body", "test"));
    }

    @Test
    void testShowNotificationWhenDisabled() {
        DemoMode demo = new DemoMode();
        assertDoesNotThrow(() -> demo.showNotification(null, "test"));
    }

    @Test
    void testClearOverlaysWhenDisabled() {
        DemoMode demo = new DemoMode();
        assertDoesNotThrow(() -> demo.clearOverlays(null));
    }
}
