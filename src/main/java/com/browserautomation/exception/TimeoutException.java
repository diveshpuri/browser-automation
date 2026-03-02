package com.browserautomation.exception;

/**
 * Exception thrown when an operation exceeds its timeout.
 */
public class TimeoutException extends BrowserAutomationException {

    private final long timeoutMs;

    public TimeoutException(String message) {
        super(message);
        this.timeoutMs = -1;
    }

    public TimeoutException(String operation, long timeoutMs) {
        super("Operation '" + operation + "' timed out after " + timeoutMs + "ms");
        this.timeoutMs = timeoutMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}
