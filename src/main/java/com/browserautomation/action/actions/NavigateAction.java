package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Navigate to a URL.
 */
public class NavigateAction implements BrowserAction {

    private static final Logger logger = LoggerFactory.getLogger(NavigateAction.class);

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
            logger.warn("[ACTION:navigate] URL parameter is missing or empty");
            return ActionResult.error("URL is required");
        }
        try {
            logger.info("[ACTION:navigate] Navigating to: {}", url);
            long start = System.currentTimeMillis();
            session.navigateTo(url);
            long elapsed = System.currentTimeMillis() - start;
            String finalUrl = session.getCurrentPage().url();
            logger.info("[ACTION:navigate] Navigation completed in {}ms — final URL: {}", elapsed, finalUrl);
            return ActionResult.success("Navigated to " + url);
        } catch (Exception e) {
            logger.error("[ACTION:navigate] Failed to navigate to {}: {}", url, e.getMessage());
            return ActionResult.error("Failed to navigate: " + e.getMessage());
        }
    }
}
