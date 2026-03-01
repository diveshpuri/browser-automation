package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Send keyboard keys (e.g., Enter, Tab, shortcuts).
 */
public class SendKeysAction implements BrowserAction {

    @Override
    public String getName() {
        return "send_keys";
    }

    @Override
    public String getDescription() {
        return "Send keyboard keys or shortcuts (e.g., 'Enter', 'Tab', 'Control+a', 'Escape').";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"keys\":{\"type\":\"string\",\"description\":\"The key or key combination to send (e.g., 'Enter', 'Control+a')\"}},\"required\":[\"keys\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        String keys = params.getString("keys");
        if (keys == null || keys.isEmpty()) {
            return ActionResult.error("Keys are required");
        }
        try {
            session.sendKeys(keys);
            return ActionResult.success("Sent keys: " + keys);
        } catch (Exception e) {
            return ActionResult.error("Failed to send keys: " + e.getMessage());
        }
    }
}
