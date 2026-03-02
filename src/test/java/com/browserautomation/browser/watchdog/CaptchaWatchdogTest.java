package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaptchaWatchdogTest {

    private EventBus eventBus;
    private BrowserSession session;
    private CaptchaWatchdog watchdog;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        session = mock(BrowserSession.class);
        when(session.isStarted()).thenReturn(false);
        watchdog = new CaptchaWatchdog(eventBus, session);
    }

    @AfterEach
    void tearDown() {
        watchdog.close();
        eventBus.shutdown();
    }

    @Test
    void testWatchdogName() {
        assertEquals("captcha", watchdog.getWatchdogName());
    }

    @Test
    void testInitialState() {
        assertFalse(watchdog.isCaptchaDetected());
        assertFalse(watchdog.isWaitingForSolution());
    }

    @Test
    void testStartAndStop() {
        watchdog.start();
        assertTrue(watchdog.isRunning());
        watchdog.stop();
        assertFalse(watchdog.isRunning());
    }
}
