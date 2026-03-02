package com.browserautomation.browser.engine;

import com.browserautomation.browser.BrowserEngineType;
import com.browserautomation.browser.BrowserProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BrowserEngineType enum and engine selection in BrowserProfile.
 */
class BrowserEngineTypeTest {

    @Test
    void testDefaultEngineIsPlaywright() {
        BrowserProfile profile = new BrowserProfile();
        assertEquals(BrowserEngineType.PLAYWRIGHT, profile.getEngineType());
    }

    @Test
    void testUseSelenium() {
        BrowserProfile profile = new BrowserProfile().useSelenium();
        assertEquals(BrowserEngineType.SELENIUM, profile.getEngineType());
    }

    @Test
    void testUsePlaywright() {
        BrowserProfile profile = new BrowserProfile().usePlaywright();
        assertEquals(BrowserEngineType.PLAYWRIGHT, profile.getEngineType());
    }

    @Test
    void testEngineTypeSetter() {
        BrowserProfile profile = new BrowserProfile()
                .engineType(BrowserEngineType.SELENIUM);
        assertEquals(BrowserEngineType.SELENIUM, profile.getEngineType());
    }

    @Test
    void testSwitchEngineType() {
        BrowserProfile profile = new BrowserProfile()
                .useSelenium()
                .usePlaywright();
        assertEquals(BrowserEngineType.PLAYWRIGHT, profile.getEngineType());

        profile.useSelenium();
        assertEquals(BrowserEngineType.SELENIUM, profile.getEngineType());
    }

    @Test
    void testEngineTypeWithOtherSettings() {
        BrowserProfile profile = new BrowserProfile()
                .useSelenium()
                .headless(true)
                .viewportSize(1920, 1080)
                .waitBetweenActionsMs(200);

        assertEquals(BrowserEngineType.SELENIUM, profile.getEngineType());
        assertTrue(profile.isHeadless());
        assertEquals(1920, profile.getViewportWidth());
        assertEquals(1080, profile.getViewportHeight());
        assertEquals(200, profile.getWaitBetweenActionsMs());
    }

    @Test
    void testEnumValues() {
        BrowserEngineType[] values = BrowserEngineType.values();
        assertEquals(2, values.length);
        assertEquals(BrowserEngineType.PLAYWRIGHT, BrowserEngineType.valueOf("PLAYWRIGHT"));
        assertEquals(BrowserEngineType.SELENIUM, BrowserEngineType.valueOf("SELENIUM"));
    }
}
