package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Navigate to a URL.
 */
public class NavigateAction implements BrowserAction {

    @Override
    public String getName() {
        return "navigate";
    }

    @Override
    public String getDescription() {
        return "Navigate to a URL in the current tab.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"url\":{\"type\":\"string\",\"description\":\"The URL to navigate to\"}},\"required\":[\"url\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        String url = params.getString("url");
        if (url == null || url.isEmpty()) {
            return ActionResult.error("URL is required");
        }
        try {
            session.navigateTo(url);
            return ActionResult.success("Navigated to " + url);
        } catch (Exception e) {
            return ActionResult.error("Failed to navigate: " + e.getMessage());
        }
    }
}
