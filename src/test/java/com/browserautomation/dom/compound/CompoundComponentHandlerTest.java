package com.browserautomation.dom.compound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CompoundComponentHandlerTest {

    private CompoundComponentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CompoundComponentHandler();
    }

    @Test
    void testDetectSelectComponent() {
        Map<String, Object> element = createElement("SELECT", Map.of());
        List<Map<String, Object>> options = List.of(
                Map.of("value", "opt1", "text", "Option 1", "selected", false),
                Map.of("value", "opt2", "text", "Option 2", "selected", true)
        );
        element.put("options", options);

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertEquals(1, components.size());
        assertEquals(CompoundComponentHandler.CompoundComponent.Type.SELECT, components.get(0).getType());
        assertEquals(2, components.get(0).getOptions().size());
        assertTrue(components.get(0).getOptions().get(1).selected());
    }

    @Test
    void testDetectDateInput() {
        Map<String, Object> element = createElement("INPUT",
                Map.of("type", "date", "min", "2024-01-01", "max", "2025-12-31"));

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertEquals(1, components.size());
        assertEquals(CompoundComponentHandler.CompoundComponent.Type.DATE_TIME_INPUT, components.get(0).getType());
        assertEquals("date", components.get(0).getProperty("inputType"));
        assertEquals("2024-01-01", components.get(0).getProperty("min"));
        assertEquals("2025-12-31", components.get(0).getProperty("max"));
    }

    @Test
    void testDetectTimeInput() {
        Map<String, Object> element = createElement("INPUT", Map.of("type", "time"));

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertEquals(1, components.size());
        assertEquals(CompoundComponentHandler.CompoundComponent.Type.DATE_TIME_INPUT, components.get(0).getType());
        assertEquals("time", components.get(0).getProperty("inputType"));
    }

    @Test
    void testDetectDatetimeLocalInput() {
        Map<String, Object> element = createElement("INPUT", Map.of("type", "datetime-local"));

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertEquals(1, components.size());
        assertEquals("datetime-local", components.get(0).getProperty("inputType"));
    }

    @Test
    void testDetectDetailsComponent() {
        Map<String, Object> element = createElement("DETAILS", Map.of("open", "true"));
        element.put("summaryText", "Click to expand");

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertEquals(1, components.size());
        assertEquals(CompoundComponentHandler.CompoundComponent.Type.DETAILS_SUMMARY, components.get(0).getType());
        assertEquals("true", components.get(0).getProperty("open"));
        assertEquals("Click to expand", components.get(0).getProperty("summaryText"));
    }

    @Test
    void testDetectRangeInput() {
        Map<String, Object> element = createElement("INPUT",
                Map.of("type", "range", "min", "0", "max", "100", "value", "50"));

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertEquals(1, components.size());
        assertEquals(CompoundComponentHandler.CompoundComponent.Type.RANGE_INPUT, components.get(0).getType());
        assertEquals("50", components.get(0).getProperty("currentValue"));
    }

    @Test
    void testDetectColorInput() {
        Map<String, Object> element = createElement("INPUT",
                Map.of("type", "color", "value", "#FF0000"));

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertEquals(1, components.size());
        assertEquals(CompoundComponentHandler.CompoundComponent.Type.COLOR_INPUT, components.get(0).getType());
        assertEquals("#FF0000", components.get(0).getProperty("currentValue"));
    }

    @Test
    void testDetectFileInput() {
        Map<String, Object> element = createElement("INPUT",
                Map.of("type", "file", "accept", ".pdf,.doc", "multiple", "true"));

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertEquals(1, components.size());
        assertEquals(CompoundComponentHandler.CompoundComponent.Type.FILE_INPUT, components.get(0).getType());
        assertEquals(".pdf,.doc", components.get(0).getProperty("accept"));
    }

    @Test
    void testNonCompoundElementIgnored() {
        Map<String, Object> element = createElement("DIV", Map.of());

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertTrue(components.isEmpty());
    }

    @Test
    void testTextInputIgnored() {
        Map<String, Object> element = createElement("INPUT", Map.of("type", "text"));

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(List.of(element));

        assertTrue(components.isEmpty());
    }

    @Test
    void testGetInteractionInstructionsSelect() {
        CompoundComponentHandler.CompoundComponent component =
                new CompoundComponentHandler.CompoundComponent(
                        CompoundComponentHandler.CompoundComponent.Type.SELECT, "SELECT", Map.of());
        component.addOption(new CompoundComponentHandler.CompoundComponent.Option("v1", "Text 1", false));
        component.addOption(new CompoundComponentHandler.CompoundComponent.Option("v2", "Text 2", true));

        String instructions = handler.getInteractionInstructions(component);
        assertTrue(instructions.contains("select_dropdown"));
        assertTrue(instructions.contains("v1"));
        assertTrue(instructions.contains("[current]"));
    }

    @Test
    void testGetInteractionInstructionsDate() {
        CompoundComponentHandler.CompoundComponent component =
                new CompoundComponentHandler.CompoundComponent(
                        CompoundComponentHandler.CompoundComponent.Type.DATE_TIME_INPUT, "INPUT", Map.of());
        component.setProperty("inputType", "date");
        component.setProperty("min", "2024-01-01");

        String instructions = handler.getInteractionInstructions(component);
        assertTrue(instructions.contains("date"));
        assertTrue(instructions.contains("YYYY-MM-DD"));
    }

    @Test
    void testGetInteractionInstructionsDetails() {
        CompoundComponentHandler.CompoundComponent component =
                new CompoundComponentHandler.CompoundComponent(
                        CompoundComponentHandler.CompoundComponent.Type.DETAILS_SUMMARY, "DETAILS", Map.of());
        component.setProperty("open", "false");

        String instructions = handler.getInteractionInstructions(component);
        assertTrue(instructions.contains("closed"));
        assertTrue(instructions.contains("expand"));
    }

    @Test
    void testMultipleComponents() {
        List<Map<String, Object>> elements = List.of(
                createElement("SELECT", Map.of()),
                createElement("INPUT", Map.of("type", "date")),
                createElement("DETAILS", Map.of()),
                createElement("DIV", Map.of()),
                createElement("INPUT", Map.of("type", "text"))
        );

        List<CompoundComponentHandler.CompoundComponent> components =
                handler.detectComponents(elements);

        assertEquals(3, components.size());
    }

    @Test
    void testComponentToString() {
        CompoundComponentHandler.CompoundComponent component =
                new CompoundComponentHandler.CompoundComponent(
                        CompoundComponentHandler.CompoundComponent.Type.SELECT, "SELECT", Map.of());
        String str = component.toString();
        assertTrue(str.contains("SELECT"));
    }

    private Map<String, Object> createElement(String tagName, Map<String, String> attrs) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("tagName", tagName);
        Map<String, Object> attributes = new LinkedHashMap<>(attrs);
        element.put("attributes", attributes);
        return element;
    }
}
