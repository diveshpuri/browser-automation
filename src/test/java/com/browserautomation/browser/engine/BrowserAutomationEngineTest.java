package com.browserautomation.browser.engine;

import com.browserautomation.BrowserAutomation;
import com.browserautomation.browser.BrowserEngineType;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for engine selection via BrowserAutomation facade and BrowserSession.
 */
class BrowserAutomationEngineTest {

    @Test
    void testCreateDefaultSessionUsesPlaywright() {
        BrowserSession session = BrowserAutomation.createBrowserSession();
        assertEquals(BrowserEngineType.PLAYWRIGHT, session.getEngineType());
        assertFalse(session.isStarted());
    }

    @Test
    void testCreateSeleniumSession() {
        BrowserProfile profile = new BrowserProfile().useSelenium();
        BrowserSession session = BrowserAutomation.createBrowserSession(profile);
        assertEquals(BrowserEngineType.SELENIUM, session.getEngineType());
        assertFalse(session.isStarted());
    }

    @Test
    void testCreatePlaywrightSession() {
        BrowserProfile profile = new BrowserProfile().usePlaywright();
        BrowserSession session = BrowserAutomation.createBrowserSession(profile);
        assertEquals(BrowserEngineType.PLAYWRIGHT, session.getEngineType());
    }

    @Test
    void testSessionWithSeleniumProfileSettings() {
        BrowserProfile profile = new BrowserProfile()
                .useSelenium()
                .headless(true)
                .viewportSize(1920, 1080)
                .disableSecurity(false)
                .waitBetweenActionsMs(300);

        BrowserSession session = new BrowserSession(profile);
        assertEquals(BrowserEngineType.SELENIUM, session.getEngineType());
        assertEquals(1920, session.getProfile().getViewportWidth());
        assertEquals(1080, session.getProfile().getViewportHeight());
        assertEquals(300, session.getProfile().getWaitBetweenActionsMs());
    }

    @Test
    void testGetEngineBeforeStartReturnsNull() {
        BrowserSession session = new BrowserSession(new BrowserProfile().useSelenium());
        assertNull(session.getEngine());
    }

    @Test
    void testGetCurrentPageReturnsNullForSeleniumProfile() {
        // Before starting, engine is null so getCurrentPage returns null
        BrowserSession session = new BrowserSession(new BrowserProfile().useSelenium());
        assertNull(session.getCurrentPage());
    }

    @Test
    void testGetContextReturnsNullForSeleniumProfile() {
        BrowserSession session = new BrowserSession(new BrowserProfile().useSelenium());
        assertNull(session.getContext());
    }

    @Test
    void testPlaywrightEngineInstantiation() {
        PlaywrightBrowserEngine engine = new PlaywrightBrowserEngine();
        assertNotNull(engine);
        assertFalse(engine.isStarted());
        assertEquals("Playwright", engine.getEngineTypeName());
    }

    @Test
    void testSeleniumEngineInstantiation() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertNotNull(engine);
        assertFalse(engine.isStarted());
        assertEquals("Selenium", engine.getEngineTypeName());
    }

    @Test
    void testEngineTypeEnumeration() {
        // Verify both engine types exist
        assertNotNull(BrowserEngineType.PLAYWRIGHT);
        assertNotNull(BrowserEngineType.SELENIUM);
        assertEquals(2, BrowserEngineType.values().length);
    }

    @Test
    void testProfileEngineTypeChaining() {
        // Test that engine type can be changed after initial creation
        BrowserProfile profile = new BrowserProfile();
        assertEquals(BrowserEngineType.PLAYWRIGHT, profile.getEngineType());

        profile.useSelenium();
        assertEquals(BrowserEngineType.SELENIUM, profile.getEngineType());

        profile.usePlaywright();
        assertEquals(BrowserEngineType.PLAYWRIGHT, profile.getEngineType());

        profile.engineType(BrowserEngineType.SELENIUM);
        assertEquals(BrowserEngineType.SELENIUM, profile.getEngineType());
    }
}
