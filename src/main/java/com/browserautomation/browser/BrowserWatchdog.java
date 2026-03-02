package com.browserautomation.browser;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Browser watchdog for monitoring and handling browser issues.
 *
 * <p>Monitors for:</p>
 * <ul>
 *   <li>Browser crashes and unresponsiveness</li>
 *   <li>JavaScript dialogs (alert, confirm, prompt, beforeunload)</li>
 *   <li>Navigation errors and timeouts</li>
 *   <li>Console errors</li>
 * </ul>
 */
public class BrowserWatchdog implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BrowserWatchdog.class);

    private final BrowserSession session;
    private final WatchdogConfig config;
    private final ScheduledExecutorService scheduler;
    private final List<WatchdogEvent> events;
    private final List<Consumer<WatchdogEvent>> eventListeners;
    private ScheduledFuture<?> healthCheckFuture;
    private volatile boolean running;

    /**
     * Create a watchdog for the given browser session.
     */
    public BrowserWatchdog(BrowserSession session) {
        this(session, new WatchdogConfig());
    }

    /**
     * Create a watchdog with custom configuration.
     */
    public BrowserWatchdog(BrowserSession session, WatchdogConfig config) {
        this.session = session;
        this.config = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "browser-watchdog");
            t.setDaemon(true);
            return t;
        });
        this.events = new CopyOnWriteArrayList<>();
        this.eventListeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Start monitoring the browser session.
     */
    public void start() {
        if (running) return;
        running = true;

        // Set up dialog handler
        if (config.isAutoHandleDialogs()) {
            setupDialogHandler();
        }

        // Set up console error monitoring
        if (config.isMonitorConsoleErrors()) {
            setupConsoleMonitor();
        }

        // Start periodic health checks
        if (config.getHealthCheckIntervalMs() > 0) {
            healthCheckFuture = scheduler.scheduleAtFixedRate(
                    this::performHealthCheck,
                    config.getHealthCheckIntervalMs(),
                    config.getHealthCheckIntervalMs(),
                    TimeUnit.MILLISECONDS
            );
        }

        logger.info("Browser watchdog started (dialogs={}, console={}, healthCheck={}ms)",
                config.isAutoHandleDialogs(), config.isMonitorConsoleErrors(),
                config.getHealthCheckIntervalMs());
    }

    /**
     * Stop monitoring.
     */
    public void stop() {
        running = false;
        if (healthCheckFuture != null) {
            healthCheckFuture.cancel(false);
        }
        logger.info("Browser watchdog stopped ({} events recorded)", events.size());
    }

    /**
     * Add an event listener.
     */
    public void addEventListener(Consumer<WatchdogEvent> listener) {
        eventListeners.add(listener);
    }

    /**
     * Get all recorded events.
     */
    public List<WatchdogEvent> getEvents() {
        return List.copyOf(events);
    }

    /**
     * Get events of a specific type.
     */
    public List<WatchdogEvent> getEvents(WatchdogEvent.EventType type) {
        return events.stream().filter(e -> e.getType() == type).toList();
    }

    /**
     * Clear recorded events.
     */
    public void clearEvents() {
        events.clear();
    }

    private void setupDialogHandler() {
        try {
            Page page = session.getCurrentPage();
            if (page != null) {
                page.onDialog(dialog -> {
                    String type = dialog.type();
                    String message = dialog.message();
                    logger.info("Dialog detected: type={}, message='{}'", type, message);

                    WatchdogEvent event = new WatchdogEvent(
                            WatchdogEvent.EventType.DIALOG,
                            "Dialog: [" + type + "] " + message
                    );
                    recordEvent(event);

                    // Auto-handle the dialog
                    switch (config.getDialogAction()) {
                        case ACCEPT -> dialog.accept();
                        case DISMISS -> dialog.dismiss();
                        case ACCEPT_WITH_TEXT -> dialog.accept(config.getDialogResponseText());
                    }
                });
            }
        } catch (Exception e) {
            logger.warn("Failed to set up dialog handler: {}", e.getMessage());
        }
    }

    private void setupConsoleMonitor() {
        try {
            Page page = session.getCurrentPage();
            if (page != null) {
                page.onConsoleMessage(consoleMessage -> {
                    if ("error".equals(consoleMessage.type())) {
                        WatchdogEvent event = new WatchdogEvent(
                                WatchdogEvent.EventType.CONSOLE_ERROR,
                                "Console error: " + consoleMessage.text()
                        );
                        recordEvent(event);
                    }
                });
            }
        } catch (Exception e) {
            logger.warn("Failed to set up console monitor: {}", e.getMessage());
        }
    }

    private void performHealthCheck() {
        if (!running) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) {
                recordEvent(new WatchdogEvent(
                        WatchdogEvent.EventType.CRASH, "Browser page is null - possible crash"));
                return;
            }

            // Try to evaluate a simple expression to check if the page is responsive
            page.evaluate("() => true");
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && (message.contains("Target closed") || message.contains("Session closed")
                    || message.contains("Browser closed"))) {
                recordEvent(new WatchdogEvent(
                        WatchdogEvent.EventType.CRASH, "Browser appears to have crashed: " + message));
            } else {
                recordEvent(new WatchdogEvent(
                        WatchdogEvent.EventType.UNRESPONSIVE, "Browser unresponsive: " + message));
            }
        }
    }

    private void recordEvent(WatchdogEvent event) {
        events.add(event);
        logger.debug("Watchdog event: {}", event);
        for (Consumer<WatchdogEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.warn("Error in watchdog event listener: {}", e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * A watchdog event recorded during monitoring.
     */
    public static class WatchdogEvent {
        public enum EventType {
            DIALOG, CONSOLE_ERROR, CRASH, UNRESPONSIVE, NAVIGATION_ERROR
        }

        private final EventType type;
        private final String message;
        private final long timestamp;

        public WatchdogEvent(EventType type, String message) {
            this.type = type;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public EventType getType() { return type; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return "[" + type + "] " + message;
        }
    }

    /**
     * Configuration for the browser watchdog.
     */
    public static class WatchdogConfig {

        public enum DialogAction {
            ACCEPT, DISMISS, ACCEPT_WITH_TEXT
        }

        private boolean autoHandleDialogs = true;
        private DialogAction dialogAction = DialogAction.DISMISS;
        private String dialogResponseText = "";
        private boolean monitorConsoleErrors = true;
        private long healthCheckIntervalMs = 5000;

        public WatchdogConfig autoHandleDialogs(boolean autoHandle) {
            this.autoHandleDialogs = autoHandle;
            return this;
        }

        public WatchdogConfig dialogAction(DialogAction action) {
            this.dialogAction = action;
            return this;
        }

        public WatchdogConfig dialogResponseText(String text) {
            this.dialogResponseText = text;
            return this;
        }

        public WatchdogConfig monitorConsoleErrors(boolean monitor) {
            this.monitorConsoleErrors = monitor;
            return this;
        }

        public WatchdogConfig healthCheckIntervalMs(long intervalMs) {
            this.healthCheckIntervalMs = intervalMs;
            return this;
        }

        public boolean isAutoHandleDialogs() { return autoHandleDialogs; }
        public DialogAction getDialogAction() { return dialogAction; }
        public String getDialogResponseText() { return dialogResponseText; }
        public boolean isMonitorConsoleErrors() { return monitorConsoleErrors; }
        public long getHealthCheckIntervalMs() { return healthCheckIntervalMs; }
    }
}
