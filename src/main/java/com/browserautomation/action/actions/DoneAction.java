package com.browserautomation.action.actions;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Signal that the task is complete and provide the final result.
 */
public class DoneAction implements BrowserAction {

    private static final Logger logger = LoggerFactory.getLogger(DoneAction.class);

    @Override
    public String getName() {
        return "done";
    }

    @Override
    public String getDescription() {
        return "Signal that the task is complete. Provide the final text result/answer of the task.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\",\"description\":\"The final result or answer for the task\"}},\"required\":[\"text\"]}";
    }

    @Override
    public ActionResult execute(BrowserSession session, ActionParameters params) {
        String text = params.getString("text", "Task completed");
        logger.info("[ACTION:done] Task signaled as complete — result: {}", text);
        return ActionResult.done(text);
    }
}
