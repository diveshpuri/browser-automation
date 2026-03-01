package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Switch to a different browser tab.
 */
public class SwitchTabAction implements BrowserAction {

    @Override
    public String getName() {
        return "switch_tab";
    }

    @Override
    public String getDescription() {
        return "Switch to a different browser tab by its index.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"tab_index\":{\"type\":\"integer\",\"description\":\"The index of the tab to switch to\"}},\"required\":[\"tab_index\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer tabIndex = params.getInt("tab_index");
        if (tabIndex == null) {
            return ActionResult.error("Tab index is required");
        }
        try {
            session.switchTab(tabIndex);
            return ActionResult.success("Switched to tab " + tabIndex);
        } catch (Exception e) {
            return ActionResult.error("Failed to switch tab: " + e.getMessage());
        }
    }
}
