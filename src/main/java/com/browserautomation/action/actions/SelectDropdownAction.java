package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Select a dropdown option.
 */
public class SelectDropdownAction implements BrowserAction {

    @Override
    public String getName() {
        return "select_dropdown_option";
    }

    @Override
    public String getDescription() {
        return "Select an option from a dropdown/select element by its value or label.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"The index of the select element\"},\"value\":{\"type\":\"string\",\"description\":\"The value or label of the option to select\"}},\"required\":[\"index\",\"value\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer index = params.getInt("index");
        String value = params.getString("value");
        if (index == null || value == null) {
            return ActionResult.error("Element index and value are required");
        }
        try {
            session.selectDropdownOption(index, value);
            return ActionResult.success("Selected option '" + value + "' from element [" + index + "]");
        } catch (Exception e) {
            return ActionResult.error("Failed to select dropdown option: " + e.getMessage());
        }
    }
}
