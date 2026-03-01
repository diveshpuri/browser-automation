package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Scroll the page up or down.
 */
public class ScrollAction implements BrowserAction {

    @Override
    public String getName() {
        return "scroll";
    }

    @Override
    public String getDescription() {
        return "Scroll the page up or down by a specified number of pixels.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"direction\":{\"type\":\"string\",\"enum\":[\"up\",\"down\"],\"description\":\"Direction to scroll\"},\"pixels\":{\"type\":\"integer\",\"description\":\"Number of pixels to scroll (default: 500)\"}},\"required\":[\"direction\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        String direction = params.getString("direction", "down");
        int pixels = params.getInt("pixels", 500);
        boolean down = "down".equalsIgnoreCase(direction);
        try {
            session.scroll(down, pixels);
            return ActionResult.success("Scrolled " + direction + " by " + pixels + " pixels");
        } catch (Exception e) {
            return ActionResult.error("Failed to scroll: " + e.getMessage());
        }
    }
}
