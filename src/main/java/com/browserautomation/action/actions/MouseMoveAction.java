package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Move the mouse to specific coordinates on the page.
 * Equivalent to browser-use's mouse.py move functionality.
 */
public class MouseMoveAction implements BrowserAction {

    @Override
    public String getName() {
        return "mouse_move";
    }

    @Override
    public String getDescription() {
        return "Move the mouse cursor to specific x,y coordinates on the page.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"number\",\"description\":\"X coordinate\"},\"y\":{\"type\":\"number\",\"description\":\"Y coordinate\"}},\"required\":[\"x\",\"y\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Object xVal = params.get("x");
        Object yVal = params.get("y");
        if (xVal == null || yVal == null) {
            return ActionResult.error("Both x and y coordinates are required");
        }
        double x;
        double y;
        try {
            x = (xVal instanceof Number) ? ((Number) xVal).doubleValue() : Double.parseDouble(String.valueOf(xVal));
            y = (yVal instanceof Number) ? ((Number) yVal).doubleValue() : Double.parseDouble(String.valueOf(yVal));
        } catch (NumberFormatException e) {
            return ActionResult.error("x and y must be valid numbers");
        }
        try {
            session.mouseMove(x, y);
            return ActionResult.success("Moved mouse to (" + x + ", " + y + ")");
        } catch (Exception e) {
            return ActionResult.error("Failed to move mouse: " + e.getMessage());
        }
    }
}
