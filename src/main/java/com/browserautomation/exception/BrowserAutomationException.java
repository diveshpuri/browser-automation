package com.browserautomation.exception;

/**
 * Base exception for all browser automation errors.
 */
public class BrowserAutomationException extends RuntimeException {

    public BrowserAutomationException(String message) {
        super(message);
    }

    public BrowserAutomationException(String message, Throwable cause) {
        super(message, cause);
    }
}
