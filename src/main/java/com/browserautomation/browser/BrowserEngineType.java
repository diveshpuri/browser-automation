package com.browserautomation.browser;

/**
 * Enumeration of supported browser automation engines.
 * Users can choose between Playwright and Selenium for browser control.
 */
public enum BrowserEngineType {

    /**
     * Microsoft Playwright for Java.
     * Default engine. Supports auto-wait, network interception, CDP, video recording.
     */
    PLAYWRIGHT,

    /**
     * Selenium WebDriver.
     * Industry-standard browser automation. Wide browser and language support.
     */
    SELENIUM
}
