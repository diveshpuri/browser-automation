package com.browserautomation.dom;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DomElementTest {

    @Test
    void testBuildSelectorWithId() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "submit-btn");
        DomElement element = new DomElement(0, "BUTTON", attrs, "Submit", true, true, false, "button", null, null);

        assertEquals("#submit-btn", element.buildSelector());
    }

    @Test
    void testBuildSelectorWithName() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("name", "email");
        DomElement element = new DomElement(1, "INPUT", attrs, "", true, true, false, "textbox", null, null);

        assertEquals("input[name=\"email\"]", element.buildSelector());
    }

    @Test
    void testBuildSelectorWithAriaLabel() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("aria-label", "Search");
        DomElement element = new DomElement(2, "INPUT", attrs, "", true, true, false, "searchbox", "Search", null);

        assertEquals("input[aria-label=\"Search\"]", element.buildSelector());
    }

    @Test
    void testBuildSelectorFallback() {
        Map<String, String> attrs = new HashMap<>();
        DomElement element = new DomElement(3, "DIV", attrs, "content", true, false, false, "div", null, null);

        assertEquals("div", element.buildSelector());
    }

    @Test
    void testGetDescription() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "login");
        DomElement element = new DomElement(0, "BUTTON", attrs, "Log In", true, true, false, "button", null, null);

        String desc = element.getDescription();
        assertTrue(desc.contains("button"));
        assertTrue(desc.contains("login"));
        assertTrue(desc.contains("Log In"));
    }

    @Test
    void testToLlmRepresentation() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("type", "text");
        attrs.put("placeholder", "Enter email");
        DomElement element = new DomElement(5, "INPUT", attrs, "", true, true, false, "textbox", null, null);

        String repr = element.toLlmRepresentation();
        assertTrue(repr.startsWith("[5]"));
        assertTrue(repr.contains("input"));
        assertTrue(repr.contains("type=\"text\""));
        assertTrue(repr.contains("placeholder=\"Enter email\""));
    }

    @Test
    void testBoundingBox() {
        DomElement.BoundingBox bb = new DomElement.BoundingBox(10, 20, 100, 50);
        assertEquals(10, bb.getX());
        assertEquals(20, bb.getY());
        assertEquals(100, bb.getWidth());
        assertEquals(50, bb.getHeight());
    }
}
