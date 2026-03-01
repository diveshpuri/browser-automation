package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Go back in browser history.
 */
public class GoBackAction implements BrowserAction {

    @Override
    public String getName() {
        return "go_back";
    }

    @Override
    public String getDescription() {
        return "Go back to the previous page in browser history.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        try {
            session.goBack();
            return ActionResult.success("Went back to previous page");
        } catch (Exception e) {
            return ActionResult.error("Failed to go back: " + e.getMessage());
        }
    }
}
