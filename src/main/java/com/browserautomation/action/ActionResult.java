package com.browserautomation.action;

/**
 * Result of executing a browser action.
 */
public class ActionResult {

    private final boolean success;
    private final String extractedContent;
    private final String error;
    private final boolean isDone;

    private ActionResult(boolean success, String extractedContent, String error, boolean isDone) {
        this.success = success;
        this.extractedContent = extractedContent;
        this.error = error;
        this.isDone = isDone;
    }

    public static ActionResult success() {
        return new ActionResult(true, null, null, false);
    }

    public static ActionResult success(String extractedContent) {
        return new ActionResult(true, extractedContent, null, false);
    }

    public static ActionResult error(String error) {
        return new ActionResult(false, null, error, false);
    }

    public static ActionResult done(String extractedContent) {
        return new ActionResult(true, extractedContent, null, true);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getExtractedContent() {
        return extractedContent;
    }

    public String getError() {
        return error;
    }

    public boolean isDone() {
        return isDone;
    }

    @Override
    public String toString() {
        if (isDone) {
            return "ActionResult[DONE: " + extractedContent + "]";
        }
        if (success) {
            return "ActionResult[SUCCESS" + (extractedContent != null ? ": " + extractedContent : "") + "]";
        }
        return "ActionResult[ERROR: " + error + "]";
    }
}
