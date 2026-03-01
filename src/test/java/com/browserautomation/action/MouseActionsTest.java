package com.browserautomation.action;

import com.browserautomation.action.actions.DragAndDropAction;
import com.browserautomation.action.actions.HoverAction;
import com.browserautomation.action.actions.MouseMoveAction;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for mouse action classes.
 */
class MouseActionsTest {

    @Test
    void testHoverActionProperties() {
        HoverAction action = new HoverAction();
        assertEquals("hover", action.getName());
        assertNotNull(action.getDescription());
        assertTrue(action.getDescription().contains("Hover"));
        assertNotNull(action.getParameterSchema());
        assertTrue(action.getParameterSchema().contains("index"));
    }

    @Test
    void testHoverActionMissingIndex() {
        HoverAction action = new HoverAction();
        ActionResult result = action.execute(null, new ActionParameters(Map.of()));
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("index"));
    }

    @Test
    void testDragAndDropActionProperties() {
        DragAndDropAction action = new DragAndDropAction();
        assertEquals("drag_and_drop", action.getName());
        assertNotNull(action.getDescription());
        assertTrue(action.getDescription().contains("Drag"));
        assertNotNull(action.getParameterSchema());
        assertTrue(action.getParameterSchema().contains("source_index"));
        assertTrue(action.getParameterSchema().contains("target_index"));
    }

    @Test
    void testDragAndDropMissingParams() {
        DragAndDropAction action = new DragAndDropAction();
        ActionResult result = action.execute(null, new ActionParameters(Map.of()));
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("required"));
    }

    @Test
    void testDragAndDropPartialParams() {
        DragAndDropAction action = new DragAndDropAction();
        ActionResult result = action.execute(null, new ActionParameters(Map.of("source_index", 1)));
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("required"));
    }

    @Test
    void testMouseMoveActionProperties() {
        MouseMoveAction action = new MouseMoveAction();
        assertEquals("mouse_move", action.getName());
        assertNotNull(action.getDescription());
        assertTrue(action.getDescription().contains("Move"));
        assertNotNull(action.getParameterSchema());
        assertTrue(action.getParameterSchema().contains("\"x\""));
        assertTrue(action.getParameterSchema().contains("\"y\""));
    }

    @Test
    void testMouseMoveMissingCoordinates() {
        MouseMoveAction action = new MouseMoveAction();
        ActionResult result = action.execute(null, new ActionParameters(Map.of()));
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("required"));
    }

    @Test
    void testMouseMovePartialCoordinates() {
        MouseMoveAction action = new MouseMoveAction();
        ActionResult result = action.execute(null, new ActionParameters(Map.of("x", 100.0)));
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("required"));
    }

    @Test
    void testActionsRegisteredInRegistry() {
        ActionRegistry registry = new ActionRegistry();
        assertNotNull(registry.getAction("hover"));
        assertNotNull(registry.getAction("drag_and_drop"));
        assertNotNull(registry.getAction("mouse_move"));
    }
}
