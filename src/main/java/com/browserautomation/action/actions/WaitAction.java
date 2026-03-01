package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Wait for a specified duration.
 */
public class WaitAction implements BrowserAction {

    @Override
    public String getName() {
        return "wait";
    }

    @Override
    public String getDescription() {
        return "Wait for a specified number of seconds before continuing (max 10 seconds).";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"seconds\":{\"type\":\"integer\",\"description\":\"Number of seconds to wait (max 10)\"}},\"required\":[\"seconds\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        int seconds = params.getInt("seconds", 2);
        seconds = Math.min(seconds, 10); // Cap at 10 seconds
        try {
            Thread.sleep(seconds * 1000L);
            return ActionResult.success("Waited for " + seconds + " seconds");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ActionResult.error("Wait interrupted");
        }
    }
}
