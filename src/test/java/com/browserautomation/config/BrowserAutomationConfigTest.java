package com.browserautomation.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrowserAutomationConfigTest {

    @Test
    void testSingletonInstance() {
        BrowserAutomationConfig config1 = BrowserAutomationConfig.getInstance();
        BrowserAutomationConfig config2 = BrowserAutomationConfig.getInstance();
        assertSame(config1, config2);
    }

    @Test
    void testDefaultValues() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        assertNotNull(config.getDefaultLlmProvider());
        assertNotNull(config.getDefaultModel());
        assertTrue(config.isHeadless());
        assertTrue(config.getDefaultTimeout() > 0);
    }

    @Test
    void testSettersAndGetters() {
        BrowserAutomationConfig config = BrowserAutomationConfig.getInstance();
        String originalModel = config.getDefaultModel();

        config.setDefaultModel("test-model");
        assertEquals("test-model", config.getDefaultModel());

        // Restore
        config.setDefaultModel(originalModel);
    }
}
