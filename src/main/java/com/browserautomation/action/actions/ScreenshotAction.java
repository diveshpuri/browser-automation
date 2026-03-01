package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Take a screenshot of the current page.
 */
public class ScreenshotAction implements BrowserAction {

    @Override
    public String getName() {
        return "screenshot";
    }

    @Override
    public String getDescription() {
        return "Take a screenshot of the current page and return it as a base64 encoded string.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        try {
            String base64 = session.takeScreenshotBase64();
            return ActionResult.success("Screenshot taken (base64 length: " + base64.length() + ")");
        } catch (Exception e) {
            return ActionResult.error("Failed to take screenshot: " + e.getMessage());
        }
    }
}
