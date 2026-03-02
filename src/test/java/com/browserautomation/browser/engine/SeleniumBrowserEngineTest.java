package com.browserautomation.browser.engine;

import com.browserautomation.browser.BrowserEngineType;
import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SeleniumBrowserEngine creation and configuration.
 * These tests verify engine instantiation and profile integration without
 * requiring a running browser (no Chrome/ChromeDriver needed).
 */
class SeleniumBrowserEngineTest {

    @Test
    void testEngineCreation() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertNotNull(engine);
        assertFalse(engine.isStarted());
        assertEquals("Selenium", engine.getEngineTypeName());
    }

    @Test
    void testEngineNotStartedThrowsOnGetUrl() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, engine::getCurrentUrl);
    }

    @Test
    void testEngineNotStartedThrowsOnGetTitle() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, engine::getPageTitle);
    }

    @Test
    void testEngineNotStartedThrowsOnNavigate() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, () -> engine.navigateTo("https://example.com"));
    }

    @Test
    void testEngineNotStartedThrowsOnTakeScreenshot() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, engine::takeScreenshot);
    }

    @Test
    void testEngineNotStartedThrowsOnExtractDomState() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, engine::extractDomState);
    }

    @Test
    void testEngineNotStartedThrowsOnClickElement() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, () -> engine.clickElement("div", null));
    }

    @Test
    void testEngineNotStartedThrowsOnTypeText() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, () -> engine.typeText("input", "text"));
    }

    @Test
    void testEngineNotStartedThrowsOnScroll() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, () -> engine.scroll(true, 100));
    }

    @Test
    void testEngineNotStartedThrowsOnGoBack() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, engine::goBack);
    }

    @Test
    void testEngineNotStartedThrowsOnSendKeys() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, () -> engine.sendKeys("Enter"));
    }

    @Test
    void testEngineNotStartedThrowsOnExtractContent() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, engine::extractContent);
    }

    @Test
    void testEngineNotStartedThrowsOnExecuteJavaScript() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, () -> engine.executeJavaScript("return 1"));
    }

    @Test
    void testEngineNotStartedThrowsOnGetTabsInfo() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, engine::getTabsInfo);
    }

    @Test
    void testEngineNotStartedThrowsOnGetActiveTabIndex() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertThrows(IllegalStateException.class, engine::getActiveTabIndex);
    }

    @Test
    void testWebDriverIsNullBeforeStart() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertNull(engine.getWebDriver());
    }

    @Test
    void testCloseWithoutStartDoesNotThrow() {
        SeleniumBrowserEngine engine = new SeleniumBrowserEngine();
        assertDoesNotThrow(engine::close);
    }

    @Test
    void testSessionCreatesSeleniumEngine() {
        BrowserProfile profile = new BrowserProfile().useSelenium();
        BrowserSession session = new BrowserSession(profile);
        assertEquals(BrowserEngineType.SELENIUM, session.getEngineType());
        assertFalse(session.isStarted());
    }

    @Test
    void testSessionNotStartedThrowsOnNavigate() {
        BrowserProfile profile = new BrowserProfile().useSelenium();
        BrowserSession session = new BrowserSession(profile);
        assertThrows(IllegalStateException.class, () -> session.navigateTo("https://example.com"));
    }

    @Test
    void testSessionNotStartedThrowsOnGetUrl() {
        BrowserProfile profile = new BrowserProfile().useSelenium();
        BrowserSession session = new BrowserSession(profile);
        assertThrows(IllegalStateException.class, session::getCurrentUrl);
    }

    @Test
    void testSessionNotStartedThrowsOnGetTitle() {
        BrowserProfile profile = new BrowserProfile().useSelenium();
        BrowserSession session = new BrowserSession(profile);
        assertThrows(IllegalStateException.class, session::getPageTitle);
    }

    @Test
    void testSessionCloseWithoutStartDoesNotThrow() {
        BrowserProfile profile = new BrowserProfile().useSelenium();
        BrowserSession session = new BrowserSession(profile);
        assertDoesNotThrow(session::close);
    }
}
