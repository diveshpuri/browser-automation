package com.browserautomation.exception;

/**
 * Exception thrown when a browser action fails to execute.
 */
public class ActionException extends BrowserAutomationException {

    private final String actionName;

    public ActionException(String actionName, String message) {
        super("Action '" + actionName + "' failed: " + message);
        this.actionName = actionName;
    }

    public ActionException(String actionName, String message, Throwable cause) {
        super("Action '" + actionName + "' failed: " + message, cause);
        this.actionName = actionName;
    }

    public String getActionName() {
        return actionName;
    }
}
