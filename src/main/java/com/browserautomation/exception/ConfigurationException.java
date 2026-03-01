package com.browserautomation.exception;

/**
 * Exception thrown when configuration is invalid or missing.
 */
public class ConfigurationException extends BrowserAutomationException {

    private final String configKey;

    public ConfigurationException(String message) {
        super(message);
        this.configKey = null;
    }

    public ConfigurationException(String configKey, String message) {
        super("Configuration '" + configKey + "': " + message);
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
