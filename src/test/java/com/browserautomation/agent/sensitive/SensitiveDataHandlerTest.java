package com.browserautomation.agent.sensitive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataHandlerTest {

    private SensitiveDataHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SensitiveDataHandler();
    }

    @Test
    void testMaskSecrets() {
        handler.registerSecret("login", "password", "myP@ss123");
        String masked = handler.maskSecrets("Type myP@ss123 into the password field");
        assertEquals("Type <secret>login.password</secret> into the password field", masked);
    }

    @Test
    void testUnmaskSecrets() {
        handler.registerSecret("login", "password", "myP@ss123");
        String unmasked = handler.unmaskSecrets("Type <secret>login.password</secret> into the field");
        assertEquals("Type myP@ss123 into the field", unmasked);
    }

    @Test
    void testMultipleSecrets() {
        handler.registerSecret("login", "username", "admin");
        handler.registerSecret("login", "password", "secret123");
        handler.registerSecret("api", "token", "tk_abc123");

        String text = "Login with admin and secret123, then use tk_abc123";
        String masked = handler.maskSecrets(text);

        assertTrue(masked.contains("<secret>login.username</secret>"));
        assertTrue(masked.contains("<secret>login.password</secret>"));
        assertTrue(masked.contains("<secret>api.token</secret>"));
        assertFalse(masked.contains("admin"));
        assertFalse(masked.contains("secret123"));
        assertFalse(masked.contains("tk_abc123"));
    }

    @Test
    void testContainsSecrets() {
        handler.registerSecret("login", "password", "myP@ss123");

        assertTrue(handler.containsSecrets("Enter myP@ss123 here"));
        assertFalse(handler.containsSecrets("Enter your password here"));
    }

    @Test
    void testRemoveSecret() {
        handler.registerSecret("login", "password", "myP@ss123");
        assertEquals(1, handler.getSecretCount());

        handler.removeSecret("login", "password");
        assertEquals(0, handler.getSecretCount());

        String result = handler.maskSecrets("Type myP@ss123");
        assertEquals("Type myP@ss123", result);
    }

    @Test
    void testGetDomains() {
        handler.registerSecret("login", "password", "pass1");
        handler.registerSecret("api", "key", "key1");

        assertEquals(2, handler.getDomains().size());
        assertTrue(handler.getDomains().contains("login"));
        assertTrue(handler.getDomains().contains("api"));
    }

    @Test
    void testGetKeys() {
        handler.registerSecret("login", "username", "user1");
        handler.registerSecret("login", "password", "pass1");

        assertEquals(2, handler.getKeys("login").size());
        assertTrue(handler.getKeys("login").contains("username"));
        assertTrue(handler.getKeys("login").contains("password"));
        assertTrue(handler.getKeys("nonexistent").isEmpty());
    }

    @Test
    void testNullAndEmptyInput() {
        assertNull(handler.maskSecrets(null));
        assertEquals("", handler.maskSecrets(""));
        assertNull(handler.unmaskSecrets(null));
        assertEquals("", handler.unmaskSecrets(""));
        assertFalse(handler.containsSecrets(null));
        assertFalse(handler.containsSecrets(""));
    }

    @Test
    void testEmptySecretValue() {
        handler.registerSecret("login", "password", "");
        assertEquals(0, handler.getSecretCount());
    }

    @Test
    void testOverlappingSecrets() {
        handler.registerSecret("login", "full", "mypassword123");
        handler.registerSecret("login", "partial", "mypassword");

        // Longer secret should match first
        String masked = handler.maskSecrets("Enter mypassword123 here");
        assertTrue(masked.contains("<secret>login.full</secret>"));
    }

    @Test
    void testRoundTrip() {
        handler.registerSecret("db", "host", "prod-db.example.com");
        handler.registerSecret("db", "password", "sup3rs3cret!");

        String original = "Connect to prod-db.example.com with password sup3rs3cret!";
        String masked = handler.maskSecrets(original);
        String restored = handler.unmaskSecrets(masked);

        assertEquals(original, restored);
    }

    @Test
    void testUnmaskUnknownTag() {
        String text = "Use <secret>unknown.key</secret> here";
        String result = handler.unmaskSecrets(text);
        assertEquals(text, result);
    }
}
