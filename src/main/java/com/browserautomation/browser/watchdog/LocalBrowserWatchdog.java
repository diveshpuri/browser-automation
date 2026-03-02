package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.Page;

/**
 * Watchdog that manages local browser process health.
 * Monitors for crashes, unresponsiveness, and auto-recovers.
 */
public class LocalBrowserWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private final long healthCheckIntervalMs;
    private volatile int consecutiveFailures;
    private volatile boolean healthy = true;

    public LocalBrowserWatchdog(EventBus eventBus, BrowserSession session) {
        this(eventBus, session, 5000);
    }

    public LocalBrowserWatchdog(EventBus eventBus, BrowserSession session, long healthCheckIntervalMs) {
        super(eventBus);
        this.session = session;
        this.healthCheckIntervalMs = healthCheckIntervalMs;
    }

    @Override
    public String getWatchdogName() { return "local_browser"; }

    @Override
    protected void subscribeToEvents() {
        schedulePeriodicCheck(this::performHealthCheck, healthCheckIntervalMs);
    }

    private void performHealthCheck() {
        if (!session.isStarted()) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) {
                markUnhealthy("Page is null");
                return;
            }
            page.evaluate("() => true");
            consecutiveFailures = 0;
            if (!healthy) {
                healthy = true;
                logger.info("Browser recovered and is healthy again");
            }
        } catch (Exception e) {
            consecutiveFailures++;
            String message = e.getMessage();
            if (message != null && (message.contains("Target closed") ||
                    message.contains("Session closed") || message.contains("Browser closed"))) {
                markUnhealthy("Browser crashed: " + message);
                dispatchEvent(new BrowserEvents.BrowserCrashEvent(message));
            } else if (consecutiveFailures >= 3) {
                markUnhealthy("Browser unresponsive after " + consecutiveFailures + " failures");
            }
        }
    }

    private void markUnhealthy(String reason) {
        healthy = false;
        logger.warn("Browser health check failed: {}", reason);
    }

    public boolean isHealthy() { return healthy; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
}
