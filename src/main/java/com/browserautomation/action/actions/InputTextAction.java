package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type text into a DOM element.
 */
public class InputTextAction implements BrowserAction {

    private static final Logger logger = LoggerFactory.getLogger(InputTextAction.class);

    @Override
    public String getName() {
        return "input_text";
    }

    @Override
    public String getDescription() {
        return "Type text into an input element identified by its index. This clears existing text and types the new text.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"The index of the element to type into\"},\"text\":{\"type\":\"string\",\"description\":\"The text to type\"}},\"required\":[\"index\",\"text\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer index = params.getInt("index");
        String text = params.getString("text");
        if (index == null) {
            logger.warn("[ACTION:input_text] Element index parameter is missing");
            return ActionResult.error("Element index is required");
        }
        if (text == null) {
            logger.warn("[ACTION:input_text] Text parameter is missing");
            return ActionResult.error("Text is required");
        }
        try {
            logger.info("[ACTION:input_text] Typing into element [{}]: '{}'", index, text);
            long start = System.currentTimeMillis();
            session.typeText(index, text);
            long elapsed = System.currentTimeMillis() - start;
            logger.info("[ACTION:input_text] Text input completed on element [{}] in {}ms", index, elapsed);
            return ActionResult.success("Typed text into element [" + index + "]");
        } catch (Exception e) {
            logger.error("[ACTION:input_text] Failed to type into element [{}]: {}", index, e.getMessage());
            return ActionResult.error("Failed to type text: " + e.getMessage());
        }
    }
}
