package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Manages the lifecycle of all specialized watchdogs.
 * Creates, starts, stops, and provides access to individual watchdogs.
 */
public class WatchdogManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WatchdogManager.class);

    private final EventBus eventBus;
    private final BrowserSession session;
    private final Map<String, BaseWatchdog> watchdogs = new LinkedHashMap<>();

    public WatchdogManager(EventBus eventBus, BrowserSession session) {
        this.eventBus = eventBus;
        this.session = session;
    }

    /**
     * Initialize all default watchdogs.
     */
    public WatchdogManager initializeDefaults(WatchdogManagerConfig config) {
        if (config.isCaptchaEnabled()) {
            watchdogs.put("captcha", new CaptchaWatchdog(eventBus, session));
        }
        if (config.isDownloadsEnabled()) {
            watchdogs.put("downloads", new DownloadsWatchdog(eventBus, session,
                    config.getDownloadDirectory()));
        }
        if (config.isStorageStateEnabled()) {
            watchdogs.put("storage_state", new StorageStateWatchdog(eventBus, session,
                    config.getStorageStatePath()));
        }
        if (config.isPermissionsEnabled()) {
            watchdogs.put("permissions", new PermissionsWatchdog(eventBus, session));
        }
        if (config.isSecurityEnabled()) {
            watchdogs.put("security", new SecurityWatchdog(eventBus, session));
        }
        if (config.isRecordingEnabled()) {
            watchdogs.put("recording", new RecordingWatchdog(eventBus, session,
                    config.getRecordingDirectory()));
        }
        if (config.isLocalBrowserEnabled()) {
            watchdogs.put("local_browser", new LocalBrowserWatchdog(eventBus, session));
        }
        if (config.isHarRecordingEnabled()) {
            watchdogs.put("har_recording", new HarRecordingWatchdog(eventBus, session,
                    config.getHarFilePath()));
        }
        if (config.isDialogEnabled()) {
            watchdogs.put("dialog", new DialogWatchdog(eventBus, session));
        }
        if (config.isDomEnabled()) {
            watchdogs.put("dom", new DomWatchdog(eventBus, session));
        }
        if (config.isConsoleEnabled()) {
            watchdogs.put("console", new ConsoleWatchdog(eventBus, session));
        }
        return this;
    }

    /**
     * Start all registered watchdogs.
     */
    public void startAll() {
        logger.info("Starting {} watchdogs", watchdogs.size());
        for (BaseWatchdog watchdog : watchdogs.values()) {
            try {
                watchdog.start();
            } catch (Exception e) {
                logger.warn("Failed to start {} watchdog: {}", watchdog.getWatchdogName(), e.getMessage());
            }
        }
    }

    /**
     * Stop all registered watchdogs.
     */
    public void stopAll() {
        logger.info("Stopping {} watchdogs", watchdogs.size());
        for (BaseWatchdog watchdog : watchdogs.values()) {
            try {
                watchdog.stop();
            } catch (Exception e) {
                logger.warn("Failed to stop {} watchdog: {}", watchdog.getWatchdogName(), e.getMessage());
            }
        }
    }

    /**
     * Register a custom watchdog.
     */
    public void register(String name, BaseWatchdog watchdog) {
        watchdogs.put(name, watchdog);
    }

    /**
     * Get a watchdog by name.
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseWatchdog> T get(String name) {
        return (T) watchdogs.get(name);
    }

    public Map<String, BaseWatchdog> getAll() { return Collections.unmodifiableMap(watchdogs); }
    public int getWatchdogCount() { return watchdogs.size(); }

    @Override
    public void close() {
        stopAll();
        for (BaseWatchdog watchdog : watchdogs.values()) {
            try {
                watchdog.close();
            } catch (Exception e) {
                logger.warn("Error closing {} watchdog: {}", watchdog.getWatchdogName(), e.getMessage());
            }
        }
        watchdogs.clear();
    }

    /**
     * Configuration for the WatchdogManager.
     */
    public static class WatchdogManagerConfig {
        private boolean captchaEnabled = true;
        private boolean downloadsEnabled = true;
        private boolean storageStateEnabled = false;
        private boolean permissionsEnabled = true;
        private boolean securityEnabled = true;
        private boolean recordingEnabled = false;
        private boolean localBrowserEnabled = true;
        private boolean harRecordingEnabled = false;
        private boolean dialogEnabled = true;
        private boolean domEnabled = true;
        private boolean consoleEnabled = true;
        private Path downloadDirectory = Path.of(System.getProperty("java.io.tmpdir"), "browser-automation-downloads");
        private Path storageStatePath = Path.of(System.getProperty("java.io.tmpdir"), "browser-automation-storage.json");
        private Path recordingDirectory = Path.of(System.getProperty("java.io.tmpdir"), "browser-automation-recordings");
        private Path harFilePath = Path.of(System.getProperty("java.io.tmpdir"), "browser-automation.har");

        public WatchdogManagerConfig captchaEnabled(boolean v) { captchaEnabled = v; return this; }
        public WatchdogManagerConfig downloadsEnabled(boolean v) { downloadsEnabled = v; return this; }
        public WatchdogManagerConfig storageStateEnabled(boolean v) { storageStateEnabled = v; return this; }
        public WatchdogManagerConfig permissionsEnabled(boolean v) { permissionsEnabled = v; return this; }
        public WatchdogManagerConfig securityEnabled(boolean v) { securityEnabled = v; return this; }
        public WatchdogManagerConfig recordingEnabled(boolean v) { recordingEnabled = v; return this; }
        public WatchdogManagerConfig localBrowserEnabled(boolean v) { localBrowserEnabled = v; return this; }
        public WatchdogManagerConfig harRecordingEnabled(boolean v) { harRecordingEnabled = v; return this; }
        public WatchdogManagerConfig dialogEnabled(boolean v) { dialogEnabled = v; return this; }
        public WatchdogManagerConfig domEnabled(boolean v) { domEnabled = v; return this; }
        public WatchdogManagerConfig consoleEnabled(boolean v) { consoleEnabled = v; return this; }
        public WatchdogManagerConfig downloadDirectory(Path v) { downloadDirectory = v; return this; }
        public WatchdogManagerConfig storageStatePath(Path v) { storageStatePath = v; return this; }
        public WatchdogManagerConfig recordingDirectory(Path v) { recordingDirectory = v; return this; }
        public WatchdogManagerConfig harFilePath(Path v) { harFilePath = v; return this; }

        public boolean isCaptchaEnabled() { return captchaEnabled; }
        public boolean isDownloadsEnabled() { return downloadsEnabled; }
        public boolean isStorageStateEnabled() { return storageStateEnabled; }
        public boolean isPermissionsEnabled() { return permissionsEnabled; }
        public boolean isSecurityEnabled() { return securityEnabled; }
        public boolean isRecordingEnabled() { return recordingEnabled; }
        public boolean isLocalBrowserEnabled() { return localBrowserEnabled; }
        public boolean isHarRecordingEnabled() { return harRecordingEnabled; }
        public boolean isDialogEnabled() { return dialogEnabled; }
        public boolean isDomEnabled() { return domEnabled; }
        public boolean isConsoleEnabled() { return consoleEnabled; }
        public Path getDownloadDirectory() { return downloadDirectory; }
        public Path getStorageStatePath() { return storageStatePath; }
        public Path getRecordingDirectory() { return recordingDirectory; }
        public Path getHarFilePath() { return harFilePath; }
    }
}
