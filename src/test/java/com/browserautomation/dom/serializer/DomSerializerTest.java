package com.browserautomation.dom.serializer;

import com.browserautomation.dom.DomElement;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DomSerializerTest {

    @Test
    void testIndexedSerialization() {
        DomSerializer serializer = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.INDEXED)
                .build();

        List<DomElement> elements = createTestElements();
        String result = serializer.serialize(elements);

        assertTrue(result.contains("[0]"));
        assertTrue(result.contains("[1]"));
        assertTrue(result.contains("button"));
        assertTrue(result.contains("Click Me"));
    }

    @Test
    void testMarkdownSerialization() {
        DomSerializer serializer = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.MARKDOWN)
                .build();

        List<DomElement> elements = createTestElements();
        String result = serializer.serialize(elements);

        assertTrue(result.contains("|"));
        assertTrue(result.contains("Element"));
        assertTrue(result.contains("Clickable"));
    }

    @Test
    void testAccessibilityTreeSerialization() {
        DomSerializer serializer = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.ACCESSIBILITY_TREE)
                .build();

        List<DomElement> elements = createTestElements();
        String result = serializer.serialize(elements);

        assertTrue(result.contains("[clickable]"));
    }

    @Test
    void testCompactSerialization() {
        DomSerializer serializer = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.COMPACT)
                .build();

        List<DomElement> elements = createTestElements();
        String result = serializer.serialize(elements);

        assertTrue(result.contains("0:button"));
        assertFalse(result.contains("\n"));
    }

    @Test
    void testJsonSerialization() {
        DomSerializer serializer = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.JSON)
                .build();

        List<DomElement> elements = createTestElements();
        String result = serializer.serialize(elements);

        assertTrue(result.startsWith("["));
        assertTrue(result.contains("\"tag\""));
        assertTrue(result.contains("\"clickable\""));
    }

    @Test
    void testDetectClickableElements() {
        DomSerializer serializer = new DomSerializer();
        List<DomElement> elements = createTestElements();

        List<DomElement> clickable = serializer.detectClickableElements(elements);

        assertFalse(clickable.isEmpty());
    }

    @Test
    void testIsClickableByTag() {
        DomSerializer serializer = new DomSerializer();

        DomElement button = createDomElement(0, "BUTTON", Map.of(), "Click", true, true, "button");
        DomElement link = createDomElement(1, "A", Map.of("href", "/page"), "Link", true, true, "link");
        DomElement div = createDomElement(2, "DIV", Map.of(), "Text", true, false, "");

        assertTrue(serializer.isClickable(button));
        assertTrue(serializer.isClickable(link));
        assertFalse(serializer.isClickable(div));
    }

    @Test
    void testIsClickableByRole() {
        DomSerializer serializer = new DomSerializer();

        DomElement menuItem = createDomElement(0, "DIV", Map.of(), "Menu", true, true, "menuitem");
        assertTrue(serializer.isClickable(menuItem));

        DomElement tab = createDomElement(1, "DIV", Map.of(), "Tab", true, true, "tab");
        assertTrue(serializer.isClickable(tab));
    }

    @Test
    void testIsClickableByAttribute() {
        DomSerializer serializer = new DomSerializer();

        DomElement onclick = createDomElement(0, "DIV", Map.of("onclick", "doSomething()"), "Click", true, true, "");
        assertTrue(serializer.isClickable(onclick));

        DomElement tabindex = createDomElement(1, "SPAN", Map.of("tabindex", "0"), "Focus", true, true, "");
        assertTrue(serializer.isClickable(tabindex));
    }

    @Test
    void testBoundingBoxFilter() {
        DomSerializer.BoundingBoxFilter filter = new DomSerializer.BoundingBoxFilter(5.0, 5.0, 1920.0, 1080.0);

        DomElement.BoundingBox validBb = new DomElement.BoundingBox(100, 200, 50, 30);
        assertTrue(filter.passes(validBb));

        DomElement.BoundingBox tooSmall = new DomElement.BoundingBox(100, 200, 2, 2);
        assertFalse(filter.passes(tooSmall));

        DomElement.BoundingBox offScreen = new DomElement.BoundingBox(5000, 200, 50, 30);
        assertFalse(filter.passes(offScreen));

        assertFalse(filter.passes(null));
    }

    @Test
    void testBoundingBoxFilterDisabled() {
        DomSerializer.BoundingBoxFilter filter = new DomSerializer.BoundingBoxFilter();
        filter.setFilterEnabled(false);

        assertTrue(filter.passes(null));
        assertTrue(filter.passes(new DomElement.BoundingBox(0, 0, 0, 0)));
    }

    @Test
    void testMaxElements() {
        DomSerializer serializer = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.INDEXED)
                .maxElements(2)
                .build();

        List<DomElement> elements = List.of(
                createDomElement(0, "BUTTON", Map.of(), "Btn 1", true, true, "button"),
                createDomElement(1, "BUTTON", Map.of(), "Btn 2", true, true, "button"),
                createDomElement(2, "BUTTON", Map.of(), "Btn 3", true, true, "button")
        );

        String result = serializer.serialize(elements);
        assertTrue(result.contains("[0]"));
        assertTrue(result.contains("[1]"));
        assertFalse(result.contains("[2]"));
    }

    @Test
    void testIncludeNonInteractive() {
        DomSerializer withNonInteractive = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.INDEXED)
                .includeNonInteractive(true)
                .build();

        DomSerializer withoutNonInteractive = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.INDEXED)
                .includeNonInteractive(false)
                .build();

        List<DomElement> elements = List.of(
                createDomElement(0, "BUTTON", Map.of(), "Click", true, true, "button"),
                createDomElement(1, "DIV", Map.of(), "Text", true, false, "")
        );

        String withResult = withNonInteractive.serialize(elements);
        String withoutResult = withoutNonInteractive.serialize(elements);

        assertTrue(withResult.contains("[1]"));
        assertFalse(withoutResult.contains("[1]"));
    }

    @Test
    void testTextTruncation() {
        DomSerializer serializer = new DomSerializer.Builder()
                .strategy(DomSerializer.SerializationStrategy.INDEXED)
                .maxTextLength(10)
                .build();

        List<DomElement> elements = List.of(
                createDomElement(0, "BUTTON", Map.of(), "This is a very long text that should be truncated", true, true, "button")
        );

        String result = serializer.serialize(elements);
        assertTrue(result.contains("..."));
    }

    @Test
    void testEmptyElements() {
        DomSerializer serializer = new DomSerializer();
        String result = serializer.serialize(List.of());
        assertNotNull(result);
    }

    @Test
    void testBuilderDefaults() {
        DomSerializer serializer = new DomSerializer.Builder().build();
        assertNotNull(serializer);
    }

    @Test
    void testBoundingBoxFilterGetters() {
        DomSerializer.BoundingBoxFilter filter = new DomSerializer.BoundingBoxFilter(10, 20, 1000, 800);
        assertEquals(10, filter.getMinWidth());
        assertEquals(20, filter.getMinHeight());
        assertEquals(1000, filter.getMaxX());
        assertEquals(800, filter.getMaxY());
        assertTrue(filter.isFilterEnabled());
    }

    private List<DomElement> createTestElements() {
        return List.of(
                createDomElement(0, "BUTTON", Map.of("id", "btn1"), "Click Me", true, true, "button"),
                createDomElement(1, "INPUT", Map.of("type", "text", "placeholder", "Enter text"), "", true, true, "textbox"),
                createDomElement(2, "A", Map.of("href", "/page"), "Link Text", true, true, "link")
        );
    }

    private DomElement createDomElement(int index, String tag, Map<String, String> attrs,
                                         String text, boolean visible, boolean interactive, String role) {
        DomElement.BoundingBox bb = new DomElement.BoundingBox(100, 200, 50, 30);
        return new DomElement(index, tag, attrs, text, visible, interactive, false, role, "", bb);
    }
}
