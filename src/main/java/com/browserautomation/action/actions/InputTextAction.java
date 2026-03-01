package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Type text into a DOM element.
 */
public class InputTextAction implements BrowserAction {

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
            return ActionResult.error("Element index is required");
        }
        if (text == null) {
            return ActionResult.error("Text is required");
        }
        try {
            session.typeText(index, text);
            return ActionResult.success("Typed text into element [" + index + "]");
        } catch (Exception e) {
            return ActionResult.error("Failed to type text: " + e.getMessage());
        }
    }
}
