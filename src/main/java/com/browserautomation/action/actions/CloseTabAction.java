package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Close a browser tab.
 */
public class CloseTabAction implements BrowserAction {

    @Override
    public String getName() {
        return "close_tab";
    }

    @Override
    public String getDescription() {
        return "Close a browser tab by its index.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"tab_index\":{\"type\":\"integer\",\"description\":\"The index of the tab to close\"}},\"required\":[\"tab_index\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer tabIndex = params.getInt("tab_index");
        if (tabIndex == null) {
            return ActionResult.error("Tab index is required");
        }
        try {
            session.closeTab(tabIndex);
            return ActionResult.success("Closed tab " + tabIndex);
        } catch (Exception e) {
            return ActionResult.error("Failed to close tab: " + e.getMessage());
        }
    }
}
