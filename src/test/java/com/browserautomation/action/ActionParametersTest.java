package com.browserautomation.action;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionParametersTest {

    @Test
    void testGetString() {
        ActionParameters params = new ActionParameters(Map.of("url", "https://example.com"));
        assertEquals("https://example.com", params.getString("url"));
        assertNull(params.getString("missing"));
        assertEquals("default", params.getString("missing", "default"));
    }

    @Test
    void testGetInt() {
        ActionParameters params = new ActionParameters(Map.of("index", 5));
        assertEquals(5, params.getInt("index"));
        assertNull(params.getInt("missing"));
        assertEquals(10, params.getInt("missing", 10));
    }

    @Test
    void testGetIntFromString() {
        ActionParameters params = new ActionParameters(Map.of("index", "42"));
        assertEquals(42, params.getInt("index"));
    }

    @Test
    void testGetBoolean() {
        ActionParameters params = new ActionParameters(Map.of("enabled", true));
        assertTrue(params.getBoolean("enabled"));
        assertNull(params.getBoolean("missing"));
        assertFalse(params.getBoolean("missing", false));
    }

    @Test
    void testHas() {
        ActionParameters params = new ActionParameters(Map.of("key", "value"));
        assertTrue(params.has("key"));
        assertFalse(params.has("other"));
    }

    @Test
    void testPut() {
        ActionParameters params = new ActionParameters();
        params.put("key", "value");
        assertEquals("value", params.getString("key"));
    }
}
