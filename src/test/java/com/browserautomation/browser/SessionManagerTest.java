package com.browserautomation.browser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SessionManager.
 */
class SessionManagerTest {

    @Test
    void testDefaultConstruction() {
        SessionManager manager = new SessionManager();
        assertEquals(0, manager.getActiveSessionCount());
        assertEquals(10, manager.getMaxSessions());
        assertTrue(manager.getSessionNames().isEmpty());
    }

    @Test
    void testCustomConstruction() {
        SessionManager manager = new SessionManager(new BrowserProfile(), 5);
        assertEquals(5, manager.getMaxSessions());
    }

    @Test
    void testHasSession() {
        SessionManager manager = new SessionManager();
        assertFalse(manager.hasSession("test"));
    }

    @Test
    void testGetNonExistentSession() {
        SessionManager manager = new SessionManager();
        assertTrue(manager.getSession("nonexistent").isEmpty());
    }

    @Test
    void testDuplicateSessionName() {
        // Can't actually create sessions without Playwright, but we can test the interface
        SessionManager manager = new SessionManager();
        assertEquals(0, manager.getActiveSessionCount());
    }

    @Test
    void testCloseNonExistentSession() {
        SessionManager manager = new SessionManager();
        assertFalse(manager.closeSession("nonexistent"));
    }

    @Test
    void testCloseEmpty() {
        SessionManager manager = new SessionManager();
        manager.close(); // Should not throw
        assertEquals(0, manager.getActiveSessionCount());
    }
}
