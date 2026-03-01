package com.browserautomation.browser;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrowserProfileTest {

    @Test
    void testDefaultValues() {
        BrowserProfile profile = new BrowserProfile();
        assertTrue(profile.isHeadless());
        assertEquals(1280, profile.getViewportWidth());
        assertEquals(720, profile.getViewportHeight());
        assertTrue(profile.isAcceptDownloads());
        assertFalse(profile.isDisableSecurity());
        assertFalse(profile.isDemoMode());
        assertNull(profile.getUserAgent());
        assertNull(profile.getProxy());
    }

    @Test
    void testFluentApi() {
        BrowserProfile profile = new BrowserProfile()
                .headless(false)
                .viewportSize(1920, 1080)
                .userAgent("TestAgent/1.0")
                .disableSecurity(true)
                .demoMode(true)
                .waitBetweenActionsMs(1000)
                .addArg("--no-sandbox")
                .addHeader("X-Custom", "value");

        assertFalse(profile.isHeadless());
        assertEquals(1920, profile.getViewportWidth());
        assertEquals(1080, profile.getViewportHeight());
        assertEquals("TestAgent/1.0", profile.getUserAgent());
        assertTrue(profile.isDisableSecurity());
        assertTrue(profile.isDemoMode());
        assertEquals(1000, profile.getWaitBetweenActionsMs());
        assertEquals(1, profile.getArgs().size());
        assertEquals("--no-sandbox", profile.getArgs().get(0));
        assertEquals("value", profile.getExtraHeaders().get("X-Custom"));
    }

    @Test
    void testProxySettings() {
        BrowserProfile.ProxySettings proxy = new BrowserProfile.ProxySettings(
                "http://proxy.example.com:8080", "user", "pass");

        BrowserProfile profile = new BrowserProfile().proxy(proxy);
        assertNotNull(profile.getProxy());
        assertEquals("http://proxy.example.com:8080", profile.getProxy().getServer());
        assertEquals("user", profile.getProxy().getUsername());
        assertEquals("pass", profile.getProxy().getPassword());
    }

    @Test
    void testDomainFiltering() {
        BrowserProfile profile = new BrowserProfile()
                .allowedDomains(List.of("example.com", "google.com"))
                .prohibitedDomains(List.of("evil.com"));

        assertEquals(2, profile.getAllowedDomains().size());
        assertEquals(1, profile.getProhibitedDomains().size());
    }
}
