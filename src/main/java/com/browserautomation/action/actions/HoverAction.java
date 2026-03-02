package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Hover over a DOM element by its index.
 */
public class HoverAction implements BrowserAction {

    @Override
    public String getName() {
        return "hover";
    }

    @Override
    public String getDescription() {
        return "Hover over an element identified by its index number to reveal tooltips, dropdowns, or submenus.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"The index of the element to hover over\"}},\"required\":[\"index\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer index = params.getInt("index");
        if (index == null) {
            return ActionResult.error("Element index is required");
        }
        try {
            session.hoverElement(index);
            return ActionResult.success("Hovered over element [" + index + "]");
        } catch (Exception e) {
            return ActionResult.error("Failed to hover element [" + index + "]: " + e.getMessage());
        }
    }
}
