package com.browserautomation.cdp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * CDP-based element interaction with multiple fallback click strategies.
 * Uses DOM.getContentQuads, DOM.getBoxModel, and getBoundingClientRect
 * for more reliable element clicking than Playwright's high-level API alone.
 *
 */
public class CdpElementInteractor {

    private static final Logger logger = LoggerFactory.getLogger(CdpElementInteractor.class);

    private final CdpConnection cdp;
    private final Page page;

    public CdpElementInteractor(CdpConnection cdp, Page page) {
        this.cdp = cdp;
        this.page = page;
    }

    /**
     * Click an element using CDP with multiple fallback strategies.
     * Strategy order:
     * 1. CDP DOM.getContentQuads -> Input.dispatchMouseEvent
     * 2. CDP DOM.getBoxModel -> Input.dispatchMouseEvent
     * 3. JavaScript getBoundingClientRect -> Input.dispatchMouseEvent
     * 4. JavaScript element.click() fallback
     */
    public boolean clickElement(int backendNodeId) {
        // Strategy 1: Use content quads
        try {
            double[] center = getElementCenterViaContentQuads(backendNodeId);
            if (center != null) {
                return dispatchClick(center[0], center[1]);
            }
        } catch (Exception e) {
            logger.debug("Content quads click failed: {}", e.getMessage());
        }

        // Strategy 2: Use box model
        try {
            double[] center = getElementCenterViaBoxModel(backendNodeId);
            if (center != null) {
                return dispatchClick(center[0], center[1]);
            }
        } catch (Exception e) {
            logger.debug("Box model click failed: {}", e.getMessage());
        }

        // Strategy 3: JS getBoundingClientRect
        try {
            return clickViaJavaScript(backendNodeId);
        } catch (Exception e) {
            logger.debug("JS click failed: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Get element center coordinates using CDP DOM.getContentQuads.
     */
    public double[] getElementCenterViaContentQuads(int backendNodeId) {
        if (!cdp.isConnected()) return null;
        try {
            JsonObject result = cdp.sendCommand("DOM.getContentQuads",
                    Map.of("backendNodeId", backendNodeId));
            if (result == null) return null;

            JsonArray quads = result.getAsJsonArray("quads");
            if (quads == null || quads.isEmpty()) return null;

            JsonArray firstQuad = quads.get(0).getAsJsonArray();
            if (firstQuad.size() < 8) return null;

            // Calculate center from quad points (x1,y1, x2,y2, x3,y3, x4,y4)
            double centerX = 0, centerY = 0;
            for (int i = 0; i < 8; i += 2) {
                centerX += firstQuad.get(i).getAsDouble();
                centerY += firstQuad.get(i + 1).getAsDouble();
            }
            centerX /= 4;
            centerY /= 4;

            return new double[]{centerX, centerY};
        } catch (Exception e) {
            logger.debug("getContentQuads failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get element center coordinates using CDP DOM.getBoxModel.
     */
    public double[] getElementCenterViaBoxModel(int backendNodeId) {
        if (!cdp.isConnected()) return null;
        try {
            JsonObject result = cdp.sendCommand("DOM.getBoxModel",
                    Map.of("backendNodeId", backendNodeId));
            if (result == null) return null;

            JsonObject model = result.getAsJsonObject("model");
            if (model == null) return null;

            JsonArray content = model.getAsJsonArray("content");
            if (content == null || content.size() < 8) return null;

            double centerX = 0, centerY = 0;
            for (int i = 0; i < 8; i += 2) {
                centerX += content.get(i).getAsDouble();
                centerY += content.get(i + 1).getAsDouble();
            }
            centerX /= 4;
            centerY /= 4;

            return new double[]{centerX, centerY};
        } catch (Exception e) {
            logger.debug("getBoxModel failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Click element via JavaScript as final fallback.
     */
    public boolean clickViaJavaScript(int backendNodeId) {
        try {
            JsonObject resolveResult = cdp.sendCommand("DOM.resolveNode",
                    Map.of("backendNodeId", backendNodeId));
            if (resolveResult == null) return false;

            JsonObject remoteObject = resolveResult.getAsJsonObject("object");
            if (remoteObject == null) return false;

            String objectId = remoteObject.get("objectId").getAsString();

            cdp.sendCommand("Runtime.callFunctionOn",
                    Map.of("objectId", objectId,
                            "functionDeclaration", "function() { this.click(); }"));
            return true;
        } catch (Exception e) {
            logger.debug("JS click via CDP failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Dispatch a mouse click event at specific coordinates via CDP.
     */
    public boolean dispatchClick(double x, double y) {
        try {
            // Mouse pressed
            cdp.sendCommand("Input.dispatchMouseEvent", Map.of(
                    "type", "mousePressed",
                    "x", x, "y", y,
                    "button", "left",
                    "clickCount", 1
            ));
            // Mouse released
            cdp.sendCommand("Input.dispatchMouseEvent", Map.of(
                    "type", "mouseReleased",
                    "x", x, "y", y,
                    "button", "left",
                    "clickCount", 1
            ));
            return true;
        } catch (Exception e) {
            logger.debug("Dispatch click failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Scroll element into view using CDP.
     */
    public void scrollIntoView(int backendNodeId) {
        try {
            cdp.sendCommand("DOM.scrollIntoViewIfNeeded",
                    Map.of("backendNodeId", backendNodeId));
        } catch (Exception e) {
            logger.debug("scrollIntoView failed: {}", e.getMessage());
        }
    }

    /**
     * Focus an element using CDP.
     */
    public void focusElement(int backendNodeId) {
        try {
            cdp.sendCommand("DOM.focus",
                    Map.of("backendNodeId", backendNodeId));
        } catch (Exception e) {
            logger.debug("Focus failed: {}", e.getMessage());
        }
    }

    /**
     * Get element attributes via CDP.
     */
    public JsonObject getElementAttributes(int backendNodeId) {
        try {
            return cdp.sendCommand("DOM.getAttributes",
                    Map.of("nodeId", backendNodeId));
        } catch (Exception e) {
            logger.debug("getAttributes failed: {}", e.getMessage());
            return null;
        }
    }
}
