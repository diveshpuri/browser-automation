package com.browserautomation.cdp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * CDP-based enhanced DOM snapshot capture using DOMSnapshot.captureSnapshot.
 * Provides visibility, paint order, bounding boxes, and device pixel ratio info
 * that isn't available through Playwright's high-level API.
 *
 * Equivalent to browser-use's dom/enhanced_snapshot.py.
 */
public class CdpDomSnapshot {

    private static final Logger logger = LoggerFactory.getLogger(CdpDomSnapshot.class);

    private final CdpConnection cdp;

    public CdpDomSnapshot(CdpConnection cdp) {
        this.cdp = cdp;
    }

    /**
     * Capture an enhanced DOM snapshot via CDP.
     */
    public EnhancedSnapshot captureSnapshot() {
        if (!cdp.isConnected()) {
            logger.warn("CDP not connected, returning empty snapshot");
            return new EnhancedSnapshot(List.of(), 1.0, new ViewportInfo(0, 0, 0, 0));
        }

        try {
            // Get device pixel ratio
            double devicePixelRatio = getDevicePixelRatio();

            // Get viewport info
            ViewportInfo viewport = getViewportInfo();

            // Capture DOM snapshot
            JsonObject result = cdp.sendCommand("DOMSnapshot.captureSnapshot",
                    Map.of("computedStyles", List.of(
                            "display", "visibility", "opacity", "overflow",
                            "position", "z-index", "pointer-events"
                    )));

            if (result == null) {
                return new EnhancedSnapshot(List.of(), devicePixelRatio, viewport);
            }

            List<SnapshotNode> nodes = parseSnapshot(result, devicePixelRatio);
            return new EnhancedSnapshot(nodes, devicePixelRatio, viewport);
        } catch (Exception e) {
            logger.warn("Failed to capture DOM snapshot: {}", e.getMessage());
            return new EnhancedSnapshot(List.of(), 1.0, new ViewportInfo(0, 0, 0, 0));
        }
    }

    /**
     * Get device pixel ratio via CDP.
     */
    public double getDevicePixelRatio() {
        try {
            JsonObject result = cdp.sendCommand("Runtime.evaluate",
                    Map.of("expression", "window.devicePixelRatio"));
            if (result != null && result.has("result")) {
                JsonObject evalResult = result.getAsJsonObject("result");
                if (evalResult.has("value")) {
                    return evalResult.get("value").getAsDouble();
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get device pixel ratio: {}", e.getMessage());
        }
        return 1.0;
    }

    /**
     * Get viewport and scroll information via CDP.
     */
    public ViewportInfo getViewportInfo() {
        try {
            JsonObject result = cdp.sendCommand("Runtime.evaluate",
                    Map.of("expression",
                            "JSON.stringify({scrollX:window.scrollX,scrollY:window.scrollY," +
                                    "viewportWidth:window.innerWidth,viewportHeight:window.innerHeight})"));
            if (result != null && result.has("result")) {
                JsonObject evalResult = result.getAsJsonObject("result");
                if (evalResult.has("value")) {
                    String json = evalResult.get("value").getAsString();
                    JsonObject info = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                    return new ViewportInfo(
                            info.get("scrollX").getAsDouble(),
                            info.get("scrollY").getAsDouble(),
                            info.get("viewportWidth").getAsDouble(),
                            info.get("viewportHeight").getAsDouble()
                    );
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get viewport info: {}", e.getMessage());
        }
        return new ViewportInfo(0, 0, 0, 0);
    }

    private List<SnapshotNode> parseSnapshot(JsonObject result, double devicePixelRatio) {
        List<SnapshotNode> nodes = new ArrayList<>();
        try {
            JsonArray documents = result.getAsJsonArray("documents");
            if (documents == null || documents.isEmpty()) return nodes;

            JsonObject doc = documents.get(0).getAsJsonObject();
            JsonObject nodesObj = doc.getAsJsonObject("nodes");
            if (nodesObj == null) return nodes;

            JsonArray nodeNames = nodesObj.getAsJsonArray("nodeName");
            JsonArray backendNodeIds = nodesObj.getAsJsonArray("backendNodeId");
            JsonArray parentIndices = nodesObj.getAsJsonArray("parentIndex");

            JsonObject layout = doc.getAsJsonObject("layout");
            JsonArray layoutNodeIndices = layout != null ? layout.getAsJsonArray("nodeIndex") : null;
            JsonArray bounds = layout != null ? layout.getAsJsonArray("bounds") : null;
            JsonArray paintOrders = layout != null ? layout.getAsJsonArray("paintOrders") : null;

            // Build layout index map
            Map<Integer, Integer> nodeToLayoutIndex = new HashMap<>();
            if (layoutNodeIndices != null) {
                for (int i = 0; i < layoutNodeIndices.size(); i++) {
                    nodeToLayoutIndex.put(layoutNodeIndices.get(i).getAsInt(), i);
                }
            }

            int nodeCount = nodeNames != null ? nodeNames.size() : 0;
            for (int i = 0; i < nodeCount; i++) {
                String nodeName = nodeNames.get(i).getAsString();
                int backendNodeId = backendNodeIds != null ? backendNodeIds.get(i).getAsInt() : -1;
                int parentIndex = parentIndices != null ? parentIndices.get(i).getAsInt() : -1;

                double[] boundingBox = null;
                int paintOrder = -1;

                Integer layoutIdx = nodeToLayoutIndex.get(i);
                if (layoutIdx != null && bounds != null && layoutIdx < bounds.size()) {
                    JsonArray boundsArray = bounds.get(layoutIdx).getAsJsonArray();
                    if (boundsArray.size() == 4) {
                        boundingBox = new double[]{
                                boundsArray.get(0).getAsDouble() / devicePixelRatio,
                                boundsArray.get(1).getAsDouble() / devicePixelRatio,
                                boundsArray.get(2).getAsDouble() / devicePixelRatio,
                                boundsArray.get(3).getAsDouble() / devicePixelRatio
                        };
                    }
                    if (paintOrders != null && layoutIdx < paintOrders.size()) {
                        paintOrder = paintOrders.get(layoutIdx).getAsInt();
                    }
                }

                nodes.add(new SnapshotNode(i, nodeName, backendNodeId, parentIndex,
                        boundingBox, paintOrder));
            }
        } catch (Exception e) {
            logger.warn("Error parsing DOM snapshot: {}", e.getMessage());
        }
        return nodes;
    }

    /**
     * A single node from a CDP DOM snapshot.
     */
    public static class SnapshotNode {
        private final int nodeIndex;
        private final String nodeName;
        private final int backendNodeId;
        private final int parentIndex;
        private final double[] boundingBox; // [x, y, width, height]
        private final int paintOrder;

        public SnapshotNode(int nodeIndex, String nodeName, int backendNodeId, int parentIndex,
                            double[] boundingBox, int paintOrder) {
            this.nodeIndex = nodeIndex;
            this.nodeName = nodeName;
            this.backendNodeId = backendNodeId;
            this.parentIndex = parentIndex;
            this.boundingBox = boundingBox;
            this.paintOrder = paintOrder;
        }

        public int getNodeIndex() { return nodeIndex; }
        public String getNodeName() { return nodeName; }
        public int getBackendNodeId() { return backendNodeId; }
        public int getParentIndex() { return parentIndex; }
        public double[] getBoundingBox() { return boundingBox; }
        public int getPaintOrder() { return paintOrder; }

        public boolean isVisible() {
            return boundingBox != null && boundingBox[2] > 0 && boundingBox[3] > 0;
        }

        public boolean isInViewport(double viewportWidth, double viewportHeight) {
            if (boundingBox == null) return false;
            return boundingBox[0] < viewportWidth && boundingBox[1] < viewportHeight &&
                    boundingBox[0] + boundingBox[2] > 0 && boundingBox[1] + boundingBox[3] > 0;
        }
    }

    /**
     * Enhanced DOM snapshot result containing all nodes with visibility/paint info.
     */
    public static class EnhancedSnapshot {
        private final List<SnapshotNode> nodes;
        private final double devicePixelRatio;
        private final ViewportInfo viewport;

        public EnhancedSnapshot(List<SnapshotNode> nodes, double devicePixelRatio, ViewportInfo viewport) {
            this.nodes = nodes;
            this.devicePixelRatio = devicePixelRatio;
            this.viewport = viewport;
        }

        public List<SnapshotNode> getNodes() { return nodes; }
        public double getDevicePixelRatio() { return devicePixelRatio; }
        public ViewportInfo getViewport() { return viewport; }
        public int getNodeCount() { return nodes.size(); }

        public List<SnapshotNode> getVisibleNodes() {
            return nodes.stream().filter(SnapshotNode::isVisible).toList();
        }

        public List<SnapshotNode> getNodesInViewport() {
            return nodes.stream()
                    .filter(n -> n.isInViewport(viewport.viewportWidth(), viewport.viewportHeight()))
                    .toList();
        }

        public List<SnapshotNode> getNodesByPaintOrder() {
            return nodes.stream()
                    .filter(n -> n.getPaintOrder() >= 0)
                    .sorted(Comparator.comparingInt(SnapshotNode::getPaintOrder))
                    .toList();
        }
    }

    /**
     * Viewport and scroll position information.
     */
    public record ViewportInfo(double scrollX, double scrollY, double viewportWidth, double viewportHeight) {}
}
