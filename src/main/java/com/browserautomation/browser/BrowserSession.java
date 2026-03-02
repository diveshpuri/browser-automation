package com.browserautomation.browser;

import com.browserautomation.browser.engine.BrowserEngine;
import com.browserautomation.browser.engine.PlaywrightBrowserEngine;
import com.browserautomation.browser.engine.SeleniumBrowserEngine;
import com.browserautomation.dom.DomElement;
import com.browserautomation.dom.DomState;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Manages the browser lifecycle, pages, and tabs.
 * Delegates to either a Playwright or Selenium engine based on the BrowserProfile configuration.
 *
 * this class, which internally delegates to the configured {@link BrowserEngine}.</p>
 *
 * <h2>Engine Selection</h2>
 * <pre>{@code
 * // Playwright (default)
 * BrowserSession session = new BrowserSession(new BrowserProfile().headless(true));
 *
 * // Selenium
 * BrowserSession session = new BrowserSession(new BrowserProfile().useSelenium().headless(true));
 * }</pre>
 */
public class BrowserSession implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BrowserSession.class);

    private final BrowserProfile profile;
    private BrowserEngine engine;

    public BrowserSession(BrowserProfile profile) {
        this.profile = profile;
    }

    public BrowserSession() {
        this(new BrowserProfile());
    }

    /**
     * Create the appropriate engine based on the profile's engine type.
     */
    private BrowserEngine createEngine() {
        return switch (profile.getEngineType()) {
            case SELENIUM -> new SeleniumBrowserEngine();
            case PLAYWRIGHT -> new PlaywrightBrowserEngine();
        };
    }

    /**
     * Start the browser session by launching the configured engine.
     */
    public void start() {
        logger.info("[SESSION] Starting browser session (engine={})", profile.getEngineType());
        logger.info("[SESSION]   headless={}, viewport={}x{}, acceptDownloads={}",
                profile.isHeadless(), profile.getViewportWidth(), profile.getViewportHeight(),
                profile.isAcceptDownloads());
        if (profile.getUserAgent() != null) {
            logger.info("[SESSION]   userAgent={}", profile.getUserAgent());
        }
        if (profile.getProxy() != null) {
            logger.info("[SESSION]   proxy={}", profile.getProxy().getServer());
        }
        if (!profile.getArgs().isEmpty()) {
            logger.info("[SESSION]   args={}", profile.getArgs());
        }
        if (profile.getChannel() != null) {
            logger.info("[SESSION]   channel={}", profile.getChannel());
        }

        long startTime = System.currentTimeMillis();
        this.engine = createEngine();
        engine.start(profile);
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("[SESSION] Browser session started with {} engine in {}ms",
                engine.getEngineTypeName(), totalTime);
    }

    /**
     * Start the browser session with video recording enabled.
     * Videos are saved to the specified directory when the context is closed.
     *
     * @param videoDir the directory to save recorded videos
     */
    public void startWithVideoRecording(Path videoDir) {
        logger.info("[SESSION] Starting browser session with VIDEO RECORDING (engine={})", profile.getEngineType());
        logger.info("[SESSION]   headless={}, viewport={}x{}, videoDir={}",
                profile.isHeadless(), profile.getViewportWidth(), profile.getViewportHeight(), videoDir);

        long startTime = System.currentTimeMillis();
        this.engine = createEngine();
        engine.startWithVideoRecording(profile, videoDir);
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("[SESSION] Browser session started with {} engine and video recording in {}ms",
                engine.getEngineTypeName(), totalTime);
    }

    /**
     * Navigate the current page to a URL.
     */
    public void navigateTo(String url) {
        logger.info("[NAV] Navigating to: {}", url);
        ensureStarted();
        long start = System.currentTimeMillis();
        engine.navigateTo(url);
        waitForPageLoad();
        logger.info("[NAV] Navigation completed in {}ms — final URL: {}",
                System.currentTimeMillis() - start, engine.getCurrentUrl());
    }

    /**
     * Wait for the page to reach a stable state.
     */
    public void waitForPageLoad() {
        engine.waitForPageLoad(profile.getMinimumPageLoadWaitMs());
    }

    /**
     * Get the current browser state including DOM, screenshot, and page info.
     */
    public BrowserState getState(boolean includeScreenshot) {
        ensureStarted();
        long start = System.currentTimeMillis();
        logger.debug("[STATE] Extracting browser state (includeScreenshot={})", includeScreenshot);

        String url = engine.getCurrentUrl();
        String title = engine.getPageTitle();
        logger.debug("[STATE] Current page — URL: {}, Title: '{}'", url, title);

        // Collect tab info
        List<BrowserState.TabInfo> tabs = new ArrayList<>();
        List<BrowserEngine.TabInfo> engineTabs = engine.getTabsInfo();
        for (BrowserEngine.TabInfo t : engineTabs) {
            tabs.add(new BrowserState.TabInfo(t.index(), t.url(), t.title()));
        }
        int activeIndex = engine.getActiveTabIndex();

        // Extract DOM state
        DomState domState = engine.extractDomState();

        // Take screenshot
        byte[] screenshot = null;
        if (includeScreenshot) {
            screenshot = engine.takeScreenshot();
        }

        // Get page info
        BrowserState.PageInfo pageInfo = engine.getPageInfo();

        long elapsed = System.currentTimeMillis() - start;
        logger.debug("[STATE] Browser state extracted in {}ms — {} tabs, {} elements, screenshot={}",
                elapsed, tabs.size(),
                domState != null ? domState.getElements().size() : 0,
                screenshot != null ? screenshot.length + " bytes" : "none");
        return new BrowserState(url, title, tabs, activeIndex, screenshot, domState, pageInfo);
    }

    /**
     * Click on a DOM element by its index.
     */
    public void clickElement(int elementIndex) {
        ensureStarted();
        DomState state = engine.extractDomState();
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            logger.warn("[CLICK] Element with index {} not found in {} available elements",
                    elementIndex, state.getElements().size());
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        logger.info("[CLICK] Clicking element [{}]: {} (tag={}, selector={})",
                elementIndex, element.getDescription(), element.getTagName(), element.buildSelector());

        String selector = element.buildSelector();
        engine.clickElement(selector, element);
        logger.info("[CLICK] Click completed on element [{}]", elementIndex);
        waitAfterAction();
    }

    /**
     * Type text into a DOM element by its index.
     */
    public void typeText(int elementIndex, String text) {
        ensureStarted();
        DomState state = engine.extractDomState();
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            logger.warn("[TYPE] Element with index {} not found in {} available elements",
                    elementIndex, state.getElements().size());
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        logger.info("[TYPE] Typing into element [{}]: '{}' (tag={}, selector={})",
                elementIndex, text, element.getTagName(), element.buildSelector());

        String selector = element.buildSelector();
        engine.typeText(selector, text);
        logger.info("[TYPE] Text input completed for element [{}]", elementIndex);
        waitAfterAction();
    }

    /**
     * Scroll the page or a specific element.
     */
    public void scroll(boolean down, int pixels) {
        ensureStarted();
        engine.scroll(down, pixels);
        waitAfterAction();
    }

    /**
     * Go back in browser history.
     */
    public void goBack() {
        ensureStarted();
        engine.goBack();
        waitForPageLoad();
    }

    /**
     * Switch to a tab by index.
     */
    public void switchTab(int tabIndex) {
        ensureStarted();
        engine.switchTab(tabIndex);
        logger.info("Switched to tab {}: {}", tabIndex, engine.getCurrentUrl());
    }

    /**
     * Close a tab by index.
     */
    public void closeTab(int tabIndex) {
        ensureStarted();
        engine.closeTab(tabIndex);
        logger.info("Closed tab {}", tabIndex);
    }

    /**
     * Open a new tab with the given URL.
     */
    public void openNewTab(String url) {
        ensureStarted();
        engine.openNewTab(url);
        if (url != null && !url.isEmpty()) {
            waitForPageLoad();
        }
        logger.info("Opened new tab: {}", url);
    }

    /**
     * Send keyboard keys (e.g., "Enter", "Tab", "Control+a").
     */
    public void sendKeys(String keys) {
        ensureStarted();
        engine.sendKeys(keys);
        waitAfterAction();
    }

    /**
     * Extract the text content of the current page.
     */
    public String extractContent() {
        ensureStarted();
        return engine.extractContent();
    }

    /**
     * Take a screenshot and return as base64 string.
     */
    public String takeScreenshotBase64() {
        ensureStarted();
        byte[] bytes = engine.takeScreenshot();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Upload a file to an element.
     */
    public void uploadFile(int elementIndex, Path filePath) {
        ensureStarted();
        DomState state = engine.extractDomState();
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        String selector = element.buildSelector();
        engine.uploadFile(selector, filePath);
        waitAfterAction();
    }

    /**
     * Select a dropdown option by value or label.
     */
    public void selectDropdownOption(int elementIndex, String value) {
        ensureStarted();
        DomState state = engine.extractDomState();
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        String selector = element.buildSelector();
        engine.selectDropdownOption(selector, value);
        waitAfterAction();
    }

    /**
     * Get the dropdown options for an element.
     */
    public List<String> getDropdownOptions(int elementIndex) {
        ensureStarted();
        DomState state = engine.extractDomState();
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        String selector = element.buildSelector();
        return engine.getDropdownOptions(selector);
    }

    /**
     * Hover over a DOM element by its index.
     */
    public void hoverElement(int elementIndex) {
        ensureStarted();
        DomState state = engine.extractDomState();
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        logger.info("Hovering over element [{}]: {}", elementIndex, element.getDescription());
        String selector = element.buildSelector();
        engine.hoverElement(selector);
        waitAfterAction();
    }

    /**
     * Drag one element and drop it onto another.
     */
    public void dragAndDrop(int sourceIndex, int targetIndex) {
        ensureStarted();
        DomState state = engine.extractDomState();
        DomElement sourceElement = state.getElementByIndex(sourceIndex);
        DomElement targetElement = state.getElementByIndex(targetIndex);
        if (sourceElement == null) {
            throw new RuntimeException("Source element with index " + sourceIndex + " not found");
        }
        if (targetElement == null) {
            throw new RuntimeException("Target element with index " + targetIndex + " not found");
        }
        logger.info("Dragging element [{}] to element [{}]", sourceIndex, targetIndex);
        String sourceSelector = sourceElement.buildSelector();
        String targetSelector = targetElement.buildSelector();
        engine.dragAndDrop(sourceSelector, targetSelector);
        waitAfterAction();
    }

    /**
     * Move the mouse to specific coordinates.
     */
    public void mouseMove(double x, double y) {
        ensureStarted();
        engine.mouseMove(x, y);
        waitAfterAction();
    }

    /**
     * Execute JavaScript on the page.
     */
    public Object executeJavaScript(String script) {
        ensureStarted();
        return engine.executeJavaScript(script);
    }

    /**
     * Get the current URL of the active page.
     * Engine-agnostic alternative to getCurrentPage().url().
     */
    public String getCurrentUrl() {
        ensureStarted();
        return engine.getCurrentUrl();
    }

    /**
     * Get the title of the current page.
     * Engine-agnostic alternative to getCurrentPage().title().
     */
    public String getPageTitle() {
        ensureStarted();
        return engine.getPageTitle();
    }

    /**
     * Get the underlying Playwright Page object.
     * Only available when using the Playwright engine.
     *
     * @return the Playwright Page, or null if not using Playwright
     */
    public Page getCurrentPage() {
        if (engine instanceof PlaywrightBrowserEngine playwrightEngine) {
            return playwrightEngine.getPlaywrightPage();
        }
        return null;
    }

    /**
     * Get the underlying Playwright BrowserContext.
     * Only available when using the Playwright engine.
     *
     * @return the Playwright BrowserContext, or null if not using Playwright
     */
    public BrowserContext getContext() {
        if (engine instanceof PlaywrightBrowserEngine playwrightEngine) {
            return playwrightEngine.getPlaywrightContext();
        }
        return null;
    }

    /**
     * Get the underlying BrowserEngine instance.
     * Use this to access engine-specific features.
     */
    public BrowserEngine getEngine() {
        return engine;
    }

    /**
     * Get the engine type in use.
     */
    public BrowserEngineType getEngineType() {
        return profile.getEngineType();
    }

    public BrowserProfile getProfile() {
        return profile;
    }

    /**
     * Check if the browser session is started.
     */
    public boolean isStarted() {
        return engine != null && engine.isStarted();
    }

    private void waitAfterAction() {
        try {
            Thread.sleep(profile.getWaitBetweenActionsMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void ensureStarted() {
        if (engine == null || !engine.isStarted()) {
            throw new IllegalStateException("Browser session not started. Call start() first.");
        }
    }

    @Override
    public void close() {
        logger.info("[SESSION] Closing browser session (engine={})",
                engine != null ? engine.getEngineTypeName() : "none");
        long start = System.currentTimeMillis();
        try {
            if (engine != null) {
                engine.close();
            }
            logger.info("[SESSION] Browser session closed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.warn("[SESSION] Error closing browser session: {}", e.getMessage());
        }
    }
}
