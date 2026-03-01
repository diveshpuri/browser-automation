package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Extract text content from the current page.
 */
public class ExtractContentAction implements BrowserAction {

    @Override
    public String getName() {
        return "extract_content";
    }

    @Override
    public String getDescription() {
        return "Extract the text content from the current page.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        try {
            String content = session.extractContent();
            // Truncate if too long
            if (content != null && content.length() > 10000) {
                content = content.substring(0, 10000) + "\n... (truncated)";
            }
            return ActionResult.success(content);
        } catch (Exception e) {
            return ActionResult.error("Failed to extract content: " + e.getMessage());
        }
    }
}
