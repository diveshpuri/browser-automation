package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Watchdog that monitors browser console output for errors and warnings.
 */
public class ConsoleWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private final List<ConsoleEntry> entries = new CopyOnWriteArrayList<>();
    private volatile boolean captureAll;

    public ConsoleWatchdog(EventBus eventBus, BrowserSession session) {
        this(eventBus, session, false);
    }

    public ConsoleWatchdog(EventBus eventBus, BrowserSession session, boolean captureAll) {
        super(eventBus);
        this.session = session;
        this.captureAll = captureAll;
    }

    @Override
    public String getWatchdogName() { return "console"; }

    @Override
    protected void subscribeToEvents() {
        setupConsoleHandler();
    }

    private void setupConsoleHandler() {
        if (!session.isStarted()) return;
        try {
            Page page = session.getCurrentPage();
            if (page == null) return;

            page.onConsoleMessage(message -> {
                String type = message.type();
                if (captureAll || "error".equals(type) || "warning".equals(type)) {
                    entries.add(new ConsoleEntry(type, message.text()));
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to set up console handler: {}", e.getMessage());
        }
    }

    public List<ConsoleEntry> getEntries() { return List.copyOf(entries); }
    public List<ConsoleEntry> getErrors() {
        return entries.stream().filter(e -> "error".equals(e.type())).toList();
    }
    public List<ConsoleEntry> getWarnings() {
        return entries.stream().filter(e -> "warning".equals(e.type())).toList();
    }
    public void clearEntries() { entries.clear(); }
    public void setCaptureAll(boolean captureAll) { this.captureAll = captureAll; }

    public record ConsoleEntry(String type, String text) {
        @Override
        public String toString() { return "[" + type + "] " + text; }
    }
}
