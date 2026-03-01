package com.browserautomation.exception;

/**
 * Exception thrown when LLM provider operations fail.
 */
public class LlmException extends BrowserAutomationException {

    private final String provider;
    private final int statusCode;

    public LlmException(String message) {
        super(message);
        this.provider = null;
        this.statusCode = -1;
    }

    public LlmException(String provider, String message) {
        super(provider + ": " + message);
        this.provider = provider;
        this.statusCode = -1;
    }

    public LlmException(String provider, int statusCode, String message) {
        super(provider + " (HTTP " + statusCode + "): " + message);
        this.provider = provider;
        this.statusCode = statusCode;
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
        this.provider = null;
        this.statusCode = -1;
    }

    public String getProvider() {
        return provider;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
