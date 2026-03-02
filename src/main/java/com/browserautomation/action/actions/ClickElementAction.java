package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Click on a DOM element by its index.
 */
public class ClickElementAction implements BrowserAction {

    private static final Logger logger = LoggerFactory.getLogger(ClickElementAction.class);

    @Override
    public String getName() {
        return "click";
    }

    @Override
    public String getDescription() {
        return "Click on an element identified by its index number from the DOM state.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"The index of the element to click\"}},\"required\":[\"index\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer index = params.getInt("index");
        if (index == null) {
            logger.warn("[ACTION:click] Element index parameter is missing");
            return ActionResult.error("Element index is required");
        }
        try {
            logger.info("[ACTION:click] Clicking element [{}]", index);
            long start = System.currentTimeMillis();
            session.clickElement(index);
            long elapsed = System.currentTimeMillis() - start;
            logger.info("[ACTION:click] Click completed on element [{}] in {}ms", index, elapsed);
            return ActionResult.success("Clicked element [" + index + "]");
        } catch (Exception e) {
            logger.error("[ACTION:click] Failed to click element [{}]: {}", index, e.getMessage());
            return ActionResult.error("Failed to click element [" + index + "]: " + e.getMessage());
        }
    }
}
