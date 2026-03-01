package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

import java.util.List;

/**
 * Get the options from a dropdown/select element.
 */
public class GetDropdownOptionsAction implements BrowserAction {

    @Override
    public String getName() {
        return "get_dropdown_options";
    }

    @Override
    public String getDescription() {
        return "Get all available options from a dropdown/select element.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"The index of the select element\"}},\"required\":[\"index\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer index = params.getInt("index");
        if (index == null) {
            return ActionResult.error("Element index is required");
        }
        try {
            List<String> options = session.getDropdownOptions(index);
            String result = String.join("\n", options);
            return ActionResult.success("Dropdown options:\n" + result);
        } catch (Exception e) {
            return ActionResult.error("Failed to get dropdown options: " + e.getMessage());
        }
    }
}
