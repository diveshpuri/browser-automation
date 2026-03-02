package com.browserautomation.browser.engine;

import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserState;
import com.browserautomation.dom.DomElement;
import com.browserautomation.dom.DomState;

import java.nio.file.Path;
import java.util.List;

/**
 * Abstraction over browser automation engines (Playwright, Selenium).
 * BrowserSession delegates all browser operations to an implementation of this interface.
 *
 * <p>This allows the library to support multiple browser engines while keeping
 * the action layer and agent logic engine-agnostic.</p>
 */
public interface BrowserEngine extends AutoCloseable {

    /**
     * Start the browser engine and open an initial page.
     */
    void start(BrowserProfile profile);

    /**
     * Start the browser engine with video recording enabled.
     *
     * @param profile  the browser profile
     * @param videoDir the directory to save recorded videos
     */
    void startWithVideoRecording(BrowserProfile profile, Path videoDir);

    /**
     * Navigate the current page to a URL.
     */
    void navigateTo(String url);

    /**
     * Wait for the page to reach a stable loaded state.
     */
    void waitForPageLoad(long minimumWaitMs);

    /**
     * Get the current page URL.
     */
    String getCurrentUrl();

    /**
     * Get the current page title.
     */
    String getPageTitle();

    /**
     * Extract the DOM state (interactive elements) from the current page.
     */
    DomState extractDomState();

    /**
     * Take a screenshot and return as raw PNG bytes.
     */
    byte[] takeScreenshot();

    /**
     * Get page scroll and dimension information.
     */
    BrowserState.PageInfo getPageInfo();

    /**
     * Click on a DOM element by selector.
     *
     * @param selector the CSS selector
     * @param element  the DOM element (for fallback strategies)
     */
    void clickElement(String selector, DomElement element);

    /**
     * Type text into a DOM element by selector.
     *
     * @param selector the CSS selector
     * @param text     the text to type
     */
    void typeText(String selector, String text);

    /**
     * Scroll the page by a given number of pixels.
     *
     * @param down   true to scroll down, false to scroll up
     * @param pixels number of pixels to scroll
     */
    void scroll(boolean down, int pixels);

    /**
     * Go back in browser history.
     */
    void goBack();

    /**
     * Switch to a tab by index.
     */
    void switchTab(int tabIndex);

    /**
     * Close a tab by index.
     */
    void closeTab(int tabIndex);

    /**
     * Open a new tab, optionally navigating to a URL.
     */
    void openNewTab(String url);

    /**
     * Send keyboard keys.
     *
     * @param keys the key combination (e.g., "Enter", "Tab", "Control+a")
     */
    void sendKeys(String keys);

    /**
     * Extract the text content of the current page body.
     */
    String extractContent();

    /**
     * Upload a file to an element identified by selector.
     */
    void uploadFile(String selector, Path filePath);

    /**
     * Select a dropdown option by value or label.
     */
    void selectDropdownOption(String selector, String value);

    /**
     * Get the options from a dropdown element.
     */
    List<String> getDropdownOptions(String selector);

    /**
     * Hover over an element identified by selector.
     */
    void hoverElement(String selector);

    /**
     * Drag one element and drop it onto another.
     */
    void dragAndDrop(String sourceSelector, String targetSelector);

    /**
     * Move the mouse to specific coordinates.
     */
    void mouseMove(double x, double y);

    /**
     * Execute JavaScript on the page.
     */
    Object executeJavaScript(String script);

    /**
     * Execute JavaScript with an argument.
     */
    Object executeJavaScript(String script, Object arg);

    /**
     * Get tab information (index, url, title) for all open tabs.
     */
    List<TabInfo> getTabsInfo();

    /**
     * Get the index of the currently active tab.
     */
    int getActiveTabIndex();

    /**
     * Check if the browser engine is started and ready.
     */
    boolean isStarted();

    /**
     * Get the engine type identifier.
     */
    String getEngineTypeName();

    /**
     * Information about a single browser tab.
     */
    record TabInfo(int index, String url, String title) {}
}
