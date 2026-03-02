package com.browserautomation.exception;

/**
 * Exception thrown when browser operations fail (launch, navigation, crash).
 */
public class BrowserException extends BrowserAutomationException {

    public BrowserException(String message) {
        super(message);
    }

    public BrowserException(String message, Throwable cause) {
        super(message, cause);
    }
}
