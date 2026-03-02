package com.browserautomation.scriptgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScriptGeneratorConfig.
 */
class ScriptGeneratorConfigTest {

    @Test
    void testDefaultValues() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig();
        assertEquals(1280, config.getViewportWidth());
        assertEquals(720, config.getViewportHeight());
        assertEquals(30000, config.getDefaultTimeoutMs());
        assertEquals(60000, config.getNavigationTimeoutMs());
        assertEquals(10000, config.getNetworkIdleTimeoutMs());
        assertEquals(2000, config.getDomStableTimeoutMs());
        assertNull(config.getUserAgent());
        assertTrue(config.isIncludeComments());
        assertTrue(config.isIncludeRetryLogic());
        assertTrue(config.isIncludeNetworkWaits());
        assertTrue(config.isIncludeDomStabilityWaits());
    }

    @Test
    void testViewportWidth() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().viewportWidth(1920);
        assertEquals(1920, config.getViewportWidth());
    }

    @Test
    void testViewportHeight() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().viewportHeight(1080);
        assertEquals(1080, config.getViewportHeight());
    }

    @Test
    void testDefaultTimeout() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().defaultTimeout(15000);
        assertEquals(15000, config.getDefaultTimeoutMs());
    }

    @Test
    void testNavigationTimeout() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().navigationTimeout(45000);
        assertEquals(45000, config.getNavigationTimeoutMs());
    }

    @Test
    void testNetworkIdleTimeout() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().networkIdleTimeout(5000);
        assertEquals(5000, config.getNetworkIdleTimeoutMs());
    }

    @Test
    void testDomStableTimeout() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().domStableTimeout(3000);
        assertEquals(3000, config.getDomStableTimeoutMs());
    }

    @Test
    void testUserAgent() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().userAgent("TestAgent/1.0");
        assertEquals("TestAgent/1.0", config.getUserAgent());
    }

    @Test
    void testIncludeComments() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().includeComments(false);
        assertFalse(config.isIncludeComments());
    }

    @Test
    void testIncludeRetryLogic() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().includeRetryLogic(false);
        assertFalse(config.isIncludeRetryLogic());
    }

    @Test
    void testIncludeNetworkWaits() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().includeNetworkWaits(false);
        assertFalse(config.isIncludeNetworkWaits());
    }

    @Test
    void testIncludeDomStabilityWaits() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig().includeDomStabilityWaits(false);
        assertFalse(config.isIncludeDomStabilityWaits());
    }

    @Test
    void testFluentChaining() {
        ScriptGeneratorConfig config = new ScriptGeneratorConfig()
                .viewportWidth(1920)
                .viewportHeight(1080)
                .defaultTimeout(10000)
                .navigationTimeout(30000)
                .networkIdleTimeout(5000)
                .domStableTimeout(1000)
                .userAgent("Agent/1.0")
                .includeComments(false)
                .includeRetryLogic(false)
                .includeNetworkWaits(false)
                .includeDomStabilityWaits(false);

        assertEquals(1920, config.getViewportWidth());
        assertEquals(1080, config.getViewportHeight());
        assertEquals(10000, config.getDefaultTimeoutMs());
        assertEquals(30000, config.getNavigationTimeoutMs());
        assertEquals(5000, config.getNetworkIdleTimeoutMs());
        assertEquals(1000, config.getDomStableTimeoutMs());
        assertEquals("Agent/1.0", config.getUserAgent());
        assertFalse(config.isIncludeComments());
        assertFalse(config.isIncludeRetryLogic());
        assertFalse(config.isIncludeNetworkWaits());
        assertFalse(config.isIncludeDomStabilityWaits());
    }
}
