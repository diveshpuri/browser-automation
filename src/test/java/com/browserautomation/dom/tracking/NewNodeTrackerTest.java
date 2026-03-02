package com.browserautomation.dom.tracking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NewNodeTrackerTest {

    private NewNodeTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new NewNodeTracker();
    }

    @Test
    void testFirstSnapshotAllNew() {
        List<Map<String, Object>> elements = List.of(
                createElement("BUTTON", "btn1", "Click Me"),
                createElement("INPUT", "input1", "")
        );

        NewNodeTracker.TrackingResult result = tracker.processSnapshot(elements);

        assertEquals(2, result.newNodes().size());
        assertTrue(result.removedNodes().isEmpty());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.snapshotNumber());
        assertTrue(result.hasChanges());
    }

    @Test
    void testNoChanges() {
        List<Map<String, Object>> elements = List.of(
                createElement("BUTTON", "btn1", "Click Me")
        );

        tracker.processSnapshot(elements);
        NewNodeTracker.TrackingResult result = tracker.processSnapshot(elements);

        assertTrue(result.newNodes().isEmpty());
        assertTrue(result.removedNodes().isEmpty());
        assertFalse(result.hasChanges());
    }

    @Test
    void testNewNodeDetected() {
        List<Map<String, Object>> initial = List.of(
                createElement("BUTTON", "btn1", "Click Me")
        );
        tracker.processSnapshot(initial);

        List<Map<String, Object>> updated = List.of(
                createElement("BUTTON", "btn1", "Click Me"),
                createElement("INPUT", "input1", "")
        );
        NewNodeTracker.TrackingResult result = tracker.processSnapshot(updated);

        assertEquals(1, result.newNodes().size());
        assertEquals("INPUT", result.newNodes().get(0).tagName());
    }

    @Test
    void testRemovedNodeDetected() {
        List<Map<String, Object>> initial = List.of(
                createElement("BUTTON", "btn1", "Click Me"),
                createElement("INPUT", "input1", "")
        );
        tracker.processSnapshot(initial);

        List<Map<String, Object>> updated = List.of(
                createElement("BUTTON", "btn1", "Click Me")
        );
        NewNodeTracker.TrackingResult result = tracker.processSnapshot(updated);

        assertTrue(result.newNodes().isEmpty());
        assertEquals(1, result.removedNodes().size());
    }

    @Test
    void testIsNew() {
        List<Map<String, Object>> initial = List.of(
                createElement("BUTTON", "btn1", "Click Me")
        );
        tracker.processSnapshot(initial);

        Map<String, Object> existingElement = createElement("BUTTON", "btn1", "Click Me");
        Map<String, Object> newElement = createElement("INPUT", "input1", "");

        assertFalse(tracker.isNew(existingElement));
        assertTrue(tracker.isNew(newElement));
    }

    @Test
    void testSnapshotCount() {
        assertEquals(0, tracker.getSnapshotCount());

        tracker.processSnapshot(List.of(createElement("BUTTON", "btn1", "Click")));
        assertEquals(1, tracker.getSnapshotCount());

        tracker.processSnapshot(List.of(createElement("BUTTON", "btn1", "Click")));
        assertEquals(2, tracker.getSnapshotCount());
    }

    @Test
    void testReset() {
        tracker.processSnapshot(List.of(createElement("BUTTON", "btn1", "Click")));
        assertEquals(1, tracker.getSnapshotCount());

        tracker.reset();
        assertEquals(0, tracker.getSnapshotCount());
        assertTrue(tracker.getNewNodes().isEmpty());
        assertTrue(tracker.getRemovedNodes().isEmpty());
    }

    @Test
    void testComputeFingerprint() {
        Map<String, Object> element = createElement("BUTTON", "btn1", "Click Me");
        String fp1 = tracker.computeFingerprint(element);
        String fp2 = tracker.computeFingerprint(element);

        assertEquals(fp1, fp2);
    }

    @Test
    void testDifferentElementsDifferentFingerprints() {
        Map<String, Object> button = createElement("BUTTON", "btn1", "Click");
        Map<String, Object> input = createElement("INPUT", "input1", "");

        String fp1 = tracker.computeFingerprint(button);
        String fp2 = tracker.computeFingerprint(input);

        assertNotEquals(fp1, fp2);
    }

    @Test
    void testTextChangeDetected() {
        List<Map<String, Object>> initial = List.of(
                createElement("BUTTON", "btn1", "Click Me")
        );
        tracker.processSnapshot(initial);

        List<Map<String, Object>> updated = List.of(
                createElement("BUTTON", "btn1", "Updated Text")
        );
        NewNodeTracker.TrackingResult result = tracker.processSnapshot(updated);

        // Text change creates a new fingerprint
        assertEquals(1, result.newNodes().size());
        assertEquals(1, result.removedNodes().size());
    }

    @Test
    void testEmptySnapshot() {
        NewNodeTracker.TrackingResult result = tracker.processSnapshot(List.of());

        assertTrue(result.newNodes().isEmpty());
        assertTrue(result.removedNodes().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    void testTrackedNodeRecord() {
        NewNodeTracker.TrackedNode node = new NewNodeTracker.TrackedNode(
                "fp1", "BUTTON", Map.of("id", "btn1"), "Click", 0);

        assertEquals("fp1", node.fingerprint());
        assertEquals("BUTTON", node.tagName());
        assertEquals("Click", node.textContent());
        assertEquals(0, node.index());
    }

    private Map<String, Object> createElement(String tagName, String id, String text) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("tagName", tagName);
        element.put("textContent", text);
        element.put("index", 0);
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", id);
        element.put("attributes", attrs);
        return element;
    }
}
