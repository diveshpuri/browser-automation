package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Open a new browser tab.
 */
public class OpenTabAction implements BrowserAction {

    @Override
    public String getName() {
        return "open_tab";
    }

    @Override
    public String getDescription() {
        return "Open a new browser tab, optionally navigating to a URL.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"url\":{\"type\":\"string\",\"description\":\"Optional URL to navigate to in the new tab\"}}}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        String url = params.getString("url", "");
        try {
            session.openNewTab(url);
            return ActionResult.success("Opened new tab" + (url.isEmpty() ? "" : " with URL: " + url));
        } catch (Exception e) {
            return ActionResult.error("Failed to open new tab: " + e.getMessage());
        }
    }
}
