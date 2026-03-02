package com.browserautomation.dom.snapshot;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EnhancedDomProcessorTest {

    @Test
    void testEnhancedElement() {
        var element = new EnhancedDomProcessor.EnhancedElement(
                0, "BUTTON", Map.of("id", "btn1", "class", "primary"),
                "Click me", true, true, true, 5, false,
                new double[]{10, 20, 100, 50});

        assertEquals(0, element.index());
        assertEquals("BUTTON", element.tagName());
        assertEquals("Click me", element.textContent());
        assertTrue(element.isVisible());
        assertTrue(element.isInteractive());
        assertTrue(element.isClickable());
        assertEquals(5, element.paintOrder());
        assertFalse(element.isInStackingContext());
        assertNotNull(element.boundingBox());
    }

    @Test
    void testEnhancedElementSignature() {
        var element1 = new EnhancedDomProcessor.EnhancedElement(
                0, "BUTTON", Map.of("id", "btn1"), "Click", true, true, true, 0, false, null);
        var element2 = new EnhancedDomProcessor.EnhancedElement(
                1, "BUTTON", Map.of("id", "btn1"), "Click", true, true, true, 0, false, null);

        assertEquals(element1.getSignature(), element2.getSignature());
    }

    @Test
    void testEnhancedElementDifferentSignatures() {
        var element1 = new EnhancedDomProcessor.EnhancedElement(
                0, "BUTTON", Map.of("id", "btn1"), "Click", true, true, true, 0, false, null);
        var element2 = new EnhancedDomProcessor.EnhancedElement(
                1, "A", Map.of("id", "link1"), "Navigate", true, true, true, 0, false, null);

        assertNotEquals(element1.getSignature(), element2.getSignature());
    }

    @Test
    void testEnhancedDomState() {
        var visible = new EnhancedDomProcessor.EnhancedElement(
                0, "BUTTON", Map.of(), "Click", true, true, true, 5, false, null);
        var hidden = new EnhancedDomProcessor.EnhancedElement(
                1, "DIV", Map.of(), "", false, false, false, 0, false, null);
        var clickable = new EnhancedDomProcessor.EnhancedElement(
                2, "A", Map.of(), "Link", true, true, true, 3, false, null);

        var state = new EnhancedDomProcessor.EnhancedDomState(
                List.of(visible, hidden, clickable), Set.of(2));

        assertEquals(3, state.getElementCount());
        assertEquals(2, state.getVisibleElements().size());
        assertEquals(2, state.getClickableElements().size());
        assertTrue(state.isNewNode(2));
        assertFalse(state.isNewNode(0));
    }

    @Test
    void testFilterByPaintOrder() {
        var processor = new EnhancedDomProcessor();

        var low = new EnhancedDomProcessor.EnhancedElement(
                0, "DIV", Map.of(), "Low", true, true, true, 1, false, null);
        var high = new EnhancedDomProcessor.EnhancedElement(
                1, "BUTTON", Map.of(), "High", true, true, true, 10, false, null);
        var mid = new EnhancedDomProcessor.EnhancedElement(
                2, "A", Map.of(), "Mid", true, true, true, 5, false, null);
        var hiddenElement = new EnhancedDomProcessor.EnhancedElement(
                3, "SPAN", Map.of(), "Hidden", false, false, false, 20, false, null);

        var filtered = processor.filterByPaintOrder(List.of(low, high, mid, hiddenElement));
        assertEquals(3, filtered.size());
        assertEquals(10, filtered.get(0).paintOrder());
        assertEquals(5, filtered.get(1).paintOrder());
        assertEquals(1, filtered.get(2).paintOrder());
    }

    @Test
    void testMarkNewNodes() {
        var processor = new EnhancedDomProcessor();

        var elem1 = new EnhancedDomProcessor.EnhancedElement(
                0, "BUTTON", Map.of("id", "btn1"), "Click", true, true, true, 0, false, null);
        var elem2 = new EnhancedDomProcessor.EnhancedElement(
                1, "A", Map.of("id", "link1"), "Link", true, true, true, 0, false, null);

        var previous = new EnhancedDomProcessor.EnhancedDomState(List.of(elem1), Set.of());

        var elem3 = new EnhancedDomProcessor.EnhancedElement(
                2, "INPUT", Map.of("id", "input1"), "Search", true, true, true, 0, false, null);

        var current = new EnhancedDomProcessor.EnhancedDomState(List.of(elem1, elem3), Set.of());

        Set<Integer> newNodes = processor.markNewNodes(previous, current);
        assertTrue(newNodes.contains(2)); // elem3 is new
        assertFalse(newNodes.contains(0)); // elem1 was in previous
    }

    @Test
    void testMarkNewNodesWithNullPrevious() {
        var processor = new EnhancedDomProcessor();

        var elem = new EnhancedDomProcessor.EnhancedElement(
                0, "BUTTON", Map.of(), "Click", true, true, true, 0, false, null);
        var current = new EnhancedDomProcessor.EnhancedDomState(List.of(elem), Set.of());

        Set<Integer> newNodes = processor.markNewNodes(null, current);
        assertEquals(1, newNodes.size());
        assertTrue(newNodes.contains(0));
    }

    @Test
    void testCompoundComponent() {
        var component = new EnhancedDomProcessor.CompoundComponent(
                "select", "select#country", 0, "US",
                List.of("US (us)", "UK (uk)", "CA (ca)"), false);

        assertEquals("select", component.type());
        assertEquals("select#country", component.selector());
        assertEquals(0, component.index());
        assertEquals("US", component.value());
        assertEquals(3, component.options().size());
        assertFalse(component.expanded());
    }

    @Test
    void testCompoundComponentDetails() {
        var component = new EnhancedDomProcessor.CompoundComponent(
                "details", "details", 1, "FAQ Section",
                List.of(), true);

        assertEquals("details", component.type());
        assertTrue(component.expanded());
    }

    @Test
    void testSetNewNodeIndices() {
        var state = new EnhancedDomProcessor.EnhancedDomState(List.of(), Set.of());
        assertFalse(state.isNewNode(0));

        state.setNewNodeIndices(Set.of(0, 1, 2));
        assertTrue(state.isNewNode(0));
        assertTrue(state.isNewNode(1));
        assertFalse(state.isNewNode(5));
    }
}
