package com.browserautomation.dom;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DomStateTest {

    @Test
    void testGetElementByIndex() {
        DomElement e1 = createElement(0, "BUTTON", true);
        DomElement e2 = createElement(1, "INPUT", true);
        DomElement e3 = createElement(2, "A", true);

        DomState state = new DomState(Arrays.asList(e1, e2, e3));

        assertEquals(e1, state.getElementByIndex(0));
        assertEquals(e2, state.getElementByIndex(1));
        assertEquals(e3, state.getElementByIndex(2));
        assertNull(state.getElementByIndex(99));
    }

    @Test
    void testGetInteractiveElements() {
        DomElement interactive = createElement(0, "BUTTON", true);
        DomElement nonInteractive = new DomElement(1, "DIV", new HashMap<>(), "text",
                true, false, false, "div", null, null);

        DomState state = new DomState(Arrays.asList(interactive, nonInteractive));

        List<DomElement> result = state.getInteractiveElements();
        assertEquals(1, result.size());
        assertEquals(interactive, result.get(0));
    }

    @Test
    void testGetElementCount() {
        DomState state = new DomState(Arrays.asList(
                createElement(0, "A", true),
                createElement(1, "INPUT", true),
                createElement(2, "BUTTON", true)
        ));
        assertEquals(3, state.getElementCount());
    }

    @Test
    void testToLlmRepresentation() {
        DomElement element = createElement(0, "BUTTON", true);
        DomState state = new DomState(List.of(element));

        String repr = state.toLlmRepresentation();
        assertNotNull(repr);
        assertTrue(repr.contains("[0]"));
        assertTrue(repr.contains("button"));
    }

    private DomElement createElement(int index, String tagName, boolean interactive) {
        Map<String, String> attrs = new HashMap<>();
        return new DomElement(index, tagName, attrs, "text", true, interactive, false, tagName.toLowerCase(), null, null);
    }
}
