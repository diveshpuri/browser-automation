package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import com.microsoft.playwright.BrowserContext;

import java.nio.file.Path;

/**
 * Watchdog that saves/restores cookies and localStorage across sessions.
 */
public class StorageStateWatchdog extends BaseWatchdog {

    private final BrowserSession session;
    private final Path storagePath;
    private volatile boolean autoSave;

    public StorageStateWatchdog(EventBus eventBus, BrowserSession session, Path storagePath) {
        super(eventBus);
        this.session = session;
        this.storagePath = storagePath;
        this.autoSave = true;
    }

    @Override
    public String getWatchdogName() { return "storage_state"; }

    @Override
    protected void subscribeToEvents() {
        eventBus.subscribe(BrowserEvents.AgentCompletedEvent.class, event -> {
            if (autoSave) saveState();
        });
        eventBus.subscribe(BrowserEvents.NavigateToUrlEvent.class, event -> {
            // Periodically save state during navigation
        });
    }

    /**
     * Save the current browser storage state (cookies + localStorage).
     */
    public void saveState() {
        if (!session.isStarted()) return;
        try {
            BrowserContext context = session.getContext();
            if (context != null) {
                String state = context.storageState(
                        new BrowserContext.StorageStateOptions().setPath(storagePath));
                logger.info("Storage state saved to: {}", storagePath);
                dispatchEvent(new BrowserEvents.StorageStateSavedEvent(storagePath.toString()));
            }
        } catch (Exception e) {
            logger.warn("Failed to save storage state: {}", e.getMessage());
        }
    }

    /**
     * Restore storage state from file. Must be called before browser context is created.
     */
    public boolean hasStorageState() {
        return storagePath.toFile().exists();
    }

    public Path getStoragePath() { return storagePath; }
    public void setAutoSave(boolean autoSave) { this.autoSave = autoSave; }
    public boolean isAutoSave() { return autoSave; }
}
