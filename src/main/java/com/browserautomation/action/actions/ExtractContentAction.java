package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract text content from the current page.
 */
public class ExtractContentAction implements BrowserAction {

    private static final Logger logger = LoggerFactory.getLogger(ExtractContentAction.class);

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
            logger.info("[ACTION:extract_content] Extracting page content");
            long start = System.currentTimeMillis();
            String content = session.extractContent();
            long elapsed = System.currentTimeMillis() - start;
            int originalLength = content != null ? content.length() : 0;
            // Truncate if too long
            if (content != null && content.length() > 10000) {
                content = content.substring(0, 10000) + "\n... (truncated)";
            }
            logger.info("[ACTION:extract_content] Extracted {} chars in {}ms (truncated={})",
                    originalLength, elapsed, originalLength > 10000);
            return ActionResult.success(content);
        } catch (Exception e) {
            logger.error("[ACTION:extract_content] Failed: {}", e.getMessage());
            return ActionResult.error("Failed to extract content: " + e.getMessage());
        }
    }
}
