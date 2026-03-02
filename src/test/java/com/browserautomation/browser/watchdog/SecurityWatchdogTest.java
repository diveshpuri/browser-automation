package com.browserautomation.browser.watchdog;

import com.browserautomation.browser.BrowserSession;
import com.browserautomation.event.BrowserEvents;
import com.browserautomation.event.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityWatchdogTest {

    private EventBus eventBus;
    private BrowserSession session;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        session = mock(BrowserSession.class);
        when(session.isStarted()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
    }

    @Test
    void testWatchdogName() {
        SecurityWatchdog watchdog = new SecurityWatchdog(eventBus, session);
        assertEquals("security", watchdog.getWatchdogName());
        watchdog.close();
    }

    @Test
    void testDetectsBlockedDomain() {
        SecurityWatchdog watchdog = new SecurityWatchdog(eventBus, session, Set.of("malicious.com"));
        watchdog.start();

        AtomicBoolean violationDetected = new AtomicBoolean(false);
        eventBus.subscribe(BrowserEvents.SecurityViolationEvent.class, e -> violationDetected.set(true));

        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://malicious.com/page"));

        assertTrue(watchdog.hasViolations());
        assertTrue(violationDetected.get());
        watchdog.close();
    }

    @Test
    void testDetectsNonHttps() {
        SecurityWatchdog watchdog = new SecurityWatchdog(eventBus, session);
        watchdog.start();

        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("http://insecure-site.com/page"));

        assertTrue(watchdog.hasViolations());
        watchdog.close();
    }

    @Test
    void testAllowsLocalhostHttp() {
        SecurityWatchdog watchdog = new SecurityWatchdog(eventBus, session);
        watchdog.start();

        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("http://localhost:3000"));

        assertFalse(watchdog.hasViolations());
        watchdog.close();
    }

    @Test
    void testNoViolationsForHttps() {
        SecurityWatchdog watchdog = new SecurityWatchdog(eventBus, session);
        watchdog.start();

        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("https://secure-site.com"));

        assertFalse(watchdog.hasViolations());
        watchdog.close();
    }

    @Test
    void testGetViolations() {
        SecurityWatchdog watchdog = new SecurityWatchdog(eventBus, session);
        watchdog.start();

        eventBus.dispatch(new BrowserEvents.NavigateToUrlEvent("http://insecure.com"));

        Set<String> violations = watchdog.getViolations();
        assertFalse(violations.isEmpty());
        watchdog.close();
    }
}
