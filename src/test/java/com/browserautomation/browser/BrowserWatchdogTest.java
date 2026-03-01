package com.browserautomation.browser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the BrowserWatchdog and its configuration.
 */
class BrowserWatchdogTest {

    @Test
    void testWatchdogConfigDefaults() {
        BrowserWatchdog.WatchdogConfig config = new BrowserWatchdog.WatchdogConfig();
        assertTrue(config.isAutoHandleDialogs());
        assertEquals(BrowserWatchdog.WatchdogConfig.DialogAction.DISMISS, config.getDialogAction());
        assertEquals("", config.getDialogResponseText());
        assertTrue(config.isMonitorConsoleErrors());
        assertEquals(5000, config.getHealthCheckIntervalMs());
    }

    @Test
    void testWatchdogConfigBuilder() {
        BrowserWatchdog.WatchdogConfig config = new BrowserWatchdog.WatchdogConfig()
                .autoHandleDialogs(false)
                .dialogAction(BrowserWatchdog.WatchdogConfig.DialogAction.ACCEPT)
                .dialogResponseText("OK")
                .monitorConsoleErrors(false)
                .healthCheckIntervalMs(10000);

        assertFalse(config.isAutoHandleDialogs());
        assertEquals(BrowserWatchdog.WatchdogConfig.DialogAction.ACCEPT, config.getDialogAction());
        assertEquals("OK", config.getDialogResponseText());
        assertFalse(config.isMonitorConsoleErrors());
        assertEquals(10000, config.getHealthCheckIntervalMs());
    }

    @Test
    void testWatchdogEventCreation() {
        BrowserWatchdog.WatchdogEvent event = new BrowserWatchdog.WatchdogEvent(
                BrowserWatchdog.WatchdogEvent.EventType.DIALOG, "Alert: Hello");

        assertEquals(BrowserWatchdog.WatchdogEvent.EventType.DIALOG, event.getType());
        assertEquals("Alert: Hello", event.getMessage());
        assertTrue(event.getTimestamp() > 0);
        assertTrue(event.toString().contains("DIALOG"));
    }

    @Test
    void testWatchdogEventTypes() {
        assertNotNull(BrowserWatchdog.WatchdogEvent.EventType.DIALOG);
        assertNotNull(BrowserWatchdog.WatchdogEvent.EventType.CONSOLE_ERROR);
        assertNotNull(BrowserWatchdog.WatchdogEvent.EventType.CRASH);
        assertNotNull(BrowserWatchdog.WatchdogEvent.EventType.UNRESPONSIVE);
        assertNotNull(BrowserWatchdog.WatchdogEvent.EventType.NAVIGATION_ERROR);
    }

    @Test
    void testDialogActions() {
        assertNotNull(BrowserWatchdog.WatchdogConfig.DialogAction.ACCEPT);
        assertNotNull(BrowserWatchdog.WatchdogConfig.DialogAction.DISMISS);
        assertNotNull(BrowserWatchdog.WatchdogConfig.DialogAction.ACCEPT_WITH_TEXT);
    }
}
