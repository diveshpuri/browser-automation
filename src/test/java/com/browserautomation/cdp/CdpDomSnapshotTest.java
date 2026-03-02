package com.browserautomation.cdp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CdpDomSnapshotTest {

    @Test
    void testSnapshotNodeVisibility() {
        var node = new CdpDomSnapshot.SnapshotNode(0, "DIV", 100, -1,
                new double[]{10, 20, 100, 50}, 5);
        assertTrue(node.isVisible());
        assertEquals(0, node.getNodeIndex());
        assertEquals("DIV", node.getNodeName());
        assertEquals(100, node.getBackendNodeId());
        assertEquals(-1, node.getParentIndex());
        assertEquals(5, node.getPaintOrder());
    }

    @Test
    void testSnapshotNodeNotVisible() {
        var node = new CdpDomSnapshot.SnapshotNode(0, "DIV", 100, -1,
                new double[]{10, 20, 0, 0}, 0);
        assertFalse(node.isVisible());
    }

    @Test
    void testSnapshotNodeNullBoundingBox() {
        var node = new CdpDomSnapshot.SnapshotNode(0, "DIV", 100, -1, null, -1);
        assertFalse(node.isVisible());
        assertFalse(node.isInViewport(1920, 1080));
    }

    @Test
    void testSnapshotNodeInViewport() {
        var node = new CdpDomSnapshot.SnapshotNode(0, "DIV", 100, -1,
                new double[]{100, 200, 50, 30}, 0);
        assertTrue(node.isInViewport(1920, 1080));
    }

    @Test
    void testSnapshotNodeOutOfViewport() {
        var node = new CdpDomSnapshot.SnapshotNode(0, "DIV", 100, -1,
                new double[]{2000, 2000, 50, 30}, 0);
        assertFalse(node.isInViewport(1920, 1080));
    }

    @Test
    void testEnhancedSnapshot() {
        var visible = new CdpDomSnapshot.SnapshotNode(0, "BUTTON", 101, -1,
                new double[]{10, 20, 100, 50}, 5);
        var hidden = new CdpDomSnapshot.SnapshotNode(1, "DIV", 102, -1,
                new double[]{10, 20, 0, 0}, 0);
        var offscreen = new CdpDomSnapshot.SnapshotNode(2, "A", 103, -1,
                new double[]{5000, 5000, 100, 50}, 3);

        var viewport = new CdpDomSnapshot.ViewportInfo(0, 0, 1920, 1080);
        var snapshot = new CdpDomSnapshot.EnhancedSnapshot(List.of(visible, hidden, offscreen), 2.0, viewport);

        assertEquals(3, snapshot.getNodeCount());
        assertEquals(2.0, snapshot.getDevicePixelRatio());
        assertEquals(2, snapshot.getVisibleNodes().size()); // visible + offscreen (has non-zero dimensions)
        assertEquals(2, snapshot.getNodesInViewport().size()); // visible + hidden (hidden is at 10,20 which is in viewport bounds)
    }

    @Test
    void testNodesByPaintOrder() {
        var node1 = new CdpDomSnapshot.SnapshotNode(0, "DIV", 100, -1,
                new double[]{10, 20, 100, 50}, 3);
        var node2 = new CdpDomSnapshot.SnapshotNode(1, "BUTTON", 101, -1,
                new double[]{10, 20, 100, 50}, 10);
        var node3 = new CdpDomSnapshot.SnapshotNode(2, "A", 102, -1,
                new double[]{10, 20, 100, 50}, 1);

        var viewport = new CdpDomSnapshot.ViewportInfo(0, 0, 1920, 1080);
        var snapshot = new CdpDomSnapshot.EnhancedSnapshot(List.of(node1, node2, node3), 1.0, viewport);

        var sorted = snapshot.getNodesByPaintOrder();
        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0).getPaintOrder()); // Sorted ascending by paint order
        assertEquals(3, sorted.get(1).getPaintOrder());
        assertEquals(10, sorted.get(2).getPaintOrder());
    }

    @Test
    void testViewportInfo() {
        var viewport = new CdpDomSnapshot.ViewportInfo(100, 200, 1920, 1080);
        assertEquals(100, viewport.scrollX());
        assertEquals(200, viewport.scrollY());
        assertEquals(1920, viewport.viewportWidth());
        assertEquals(1080, viewport.viewportHeight());
    }

    @Test
    void testEmptySnapshot() {
        var viewport = new CdpDomSnapshot.ViewportInfo(0, 0, 1920, 1080);
        var snapshot = new CdpDomSnapshot.EnhancedSnapshot(List.of(), 1.0, viewport);
        assertEquals(0, snapshot.getNodeCount());
        assertTrue(snapshot.getVisibleNodes().isEmpty());
        assertTrue(snapshot.getNodesInViewport().isEmpty());
        assertTrue(snapshot.getNodesByPaintOrder().isEmpty());
    }
}
