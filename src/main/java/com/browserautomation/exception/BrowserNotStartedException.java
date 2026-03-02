package com.browserautomation.exception;

/**
 * Exception thrown when attempting to use a browser session that hasn't been started.
 */
public class BrowserNotStartedException extends BrowserException {

    public BrowserNotStartedException() {
        super("Browser session not started. Call start() first.");
    }

    public BrowserNotStartedException(String message) {
        super(message);
    }
}
