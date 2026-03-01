package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Click on a DOM element by its index.
 */
public class ClickElementAction implements BrowserAction {

    @Override
    public String getName() {
        return "click";
    }

    @Override
    public String getDescription() {
        return "Click on an element identified by its index number from the DOM state.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"The index of the element to click\"}},\"required\":[\"index\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer index = params.getInt("index");
        if (index == null) {
            return ActionResult.error("Element index is required");
        }
        try {
            session.clickElement(index);
            return ActionResult.success("Clicked element [" + index + "]");
        } catch (Exception e) {
            return ActionResult.error("Failed to click element [" + index + "]: " + e.getMessage());
        }
    }
}
