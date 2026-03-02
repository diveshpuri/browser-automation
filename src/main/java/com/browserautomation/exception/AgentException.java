package com.browserautomation.exception;

/**
 * Exception thrown when the agent encounters an unrecoverable error.
 */
public class AgentException extends BrowserAutomationException {

    private final int stepNumber;

    public AgentException(String message) {
        super(message);
        this.stepNumber = -1;
    }

    public AgentException(String message, int stepNumber) {
        super("Step " + stepNumber + ": " + message);
        this.stepNumber = stepNumber;
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
        this.stepNumber = -1;
    }

    public int getStepNumber() {
        return stepNumber;
    }
}
