package com.browserautomation.browser.watchdog;

import com.browserautomation.event.EventBus;
import com.browserautomation.browser.BrowserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WatchdogManagerTest {

    private EventBus eventBus;
    private BrowserSession session;
    private WatchdogManager manager;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        session = mock(BrowserSession.class);
        when(session.isStarted()).thenReturn(false);
        manager = new WatchdogManager(eventBus, session);
    }

    @AfterEach
    void tearDown() {
        manager.close();
        eventBus.shutdown();
    }

    @Test
    void testInitializeDefaults() {
        WatchdogManager.WatchdogManagerConfig config = new WatchdogManager.WatchdogManagerConfig();
        manager.initializeDefaults(config);

        assertTrue(manager.getWatchdogCount() > 0);
        assertNotNull(manager.get("captcha"));
        assertNotNull(manager.get("downloads"));
        assertNotNull(manager.get("permissions"));
        assertNotNull(manager.get("security"));
        assertNotNull(manager.get("local_browser"));
        assertNotNull(manager.get("dialog"));
        assertNotNull(manager.get("dom"));
        assertNotNull(manager.get("console"));
    }

    @Test
    void testDisableSpecificWatchdogs() {
        WatchdogManager.WatchdogManagerConfig config = new WatchdogManager.WatchdogManagerConfig()
                .captchaEnabled(false)
                .securityEnabled(false);
        manager.initializeDefaults(config);

        assertNull(manager.get("captcha"));
        assertNull(manager.get("security"));
        assertNotNull(manager.get("downloads"));
    }

    @Test
    void testStartAndStopAll() {
        WatchdogManager.WatchdogManagerConfig config = new WatchdogManager.WatchdogManagerConfig()
                .downloadsEnabled(false)
                .storageStateEnabled(false)
                .recordingEnabled(false)
                .harRecordingEnabled(false);
        manager.initializeDefaults(config);

        assertDoesNotThrow(() -> manager.startAll());
        assertDoesNotThrow(() -> manager.stopAll());
    }

    @Test
    void testRegisterCustomWatchdog() {
        BaseWatchdog custom = mock(BaseWatchdog.class);
        when(custom.getWatchdogName()).thenReturn("custom");
        manager.register("custom", custom);

        assertNotNull(manager.get("custom"));
        assertEquals(1, manager.getWatchdogCount());
    }

    @Test
    void testGetAllWatchdogs() {
        WatchdogManager.WatchdogManagerConfig config = new WatchdogManager.WatchdogManagerConfig();
        manager.initializeDefaults(config);

        assertFalse(manager.getAll().isEmpty());
    }

    @Test
    void testWatchdogManagerConfigDefaults() {
        WatchdogManager.WatchdogManagerConfig config = new WatchdogManager.WatchdogManagerConfig();
        assertTrue(config.isCaptchaEnabled());
        assertTrue(config.isDownloadsEnabled());
        assertFalse(config.isStorageStateEnabled());
        assertTrue(config.isPermissionsEnabled());
        assertTrue(config.isSecurityEnabled());
        assertFalse(config.isRecordingEnabled());
        assertTrue(config.isLocalBrowserEnabled());
        assertFalse(config.isHarRecordingEnabled());
        assertTrue(config.isDialogEnabled());
        assertTrue(config.isDomEnabled());
        assertTrue(config.isConsoleEnabled());
    }

    @Test
    void testWatchdogManagerConfigCustomPaths() {
        Path downloadDir = Path.of("/tmp/test-downloads");
        Path storagePath = Path.of("/tmp/test-storage.json");
        Path recordingDir = Path.of("/tmp/test-recordings");
        Path harPath = Path.of("/tmp/test.har");

        WatchdogManager.WatchdogManagerConfig config = new WatchdogManager.WatchdogManagerConfig()
                .downloadDirectory(downloadDir)
                .storageStatePath(storagePath)
                .recordingDirectory(recordingDir)
                .harFilePath(harPath);

        assertEquals(downloadDir, config.getDownloadDirectory());
        assertEquals(storagePath, config.getStorageStatePath());
        assertEquals(recordingDir, config.getRecordingDirectory());
        assertEquals(harPath, config.getHarFilePath());
    }

    @Test
    void testCloseReleasesResources() {
        WatchdogManager.WatchdogManagerConfig config = new WatchdogManager.WatchdogManagerConfig()
                .downloadsEnabled(false)
                .storageStateEnabled(false)
                .recordingEnabled(false)
                .harRecordingEnabled(false);
        manager.initializeDefaults(config);
        manager.startAll();

        assertDoesNotThrow(() -> manager.close());
        assertEquals(0, manager.getWatchdogCount());
    }
}
