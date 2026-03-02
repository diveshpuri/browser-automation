package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;

/**
 * Drag one DOM element and drop it onto another.
 */
public class DragAndDropAction implements BrowserAction {

    @Override
    public String getName() {
        return "drag_and_drop";
    }

    @Override
    public String getDescription() {
        return "Drag an element and drop it onto another element. Both elements are identified by their index numbers.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"source_index\":{\"type\":\"integer\",\"description\":\"The index of the element to drag\"},\"target_index\":{\"type\":\"integer\",\"description\":\"The index of the element to drop onto\"}},\"required\":[\"source_index\",\"target_index\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        Integer sourceIndex = params.getInt("source_index");
        Integer targetIndex = params.getInt("target_index");
        if (sourceIndex == null || targetIndex == null) {
            return ActionResult.error("Both source_index and target_index are required");
        }
        try {
            session.dragAndDrop(sourceIndex, targetIndex);
            return ActionResult.success("Dragged element [" + sourceIndex + "] to element [" + targetIndex + "]");
        } catch (Exception e) {
            return ActionResult.error("Failed to drag and drop: " + e.getMessage());
        }
    }
}
