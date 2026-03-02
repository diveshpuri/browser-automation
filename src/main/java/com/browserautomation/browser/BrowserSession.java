package com.browserautomation.browser;

import com.browserautomation.dom.DomElement;
import com.browserautomation.dom.DomService;
import com.browserautomation.dom.DomState;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ViewportSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Manages the Playwright browser lifecycle, pages, and tabs.
 * Equivalent to browser-use's BrowserSession.
 */
public class BrowserSession implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(BrowserSession.class);

    private final BrowserProfile profile;
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page currentPage;
    private final DomService domService;

    public BrowserSession(BrowserProfile profile) {
        this.profile = profile;
        this.domService = new DomService();
    }

    public BrowserSession() {
        this(new BrowserProfile());
    }

    /**
     * Start the browser session by launching Playwright and the browser.
     */
    public void start() {
        logger.info("[SESSION] Starting browser session");
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
        this.playwright = Playwright.create();
        logger.info("[SESSION] Playwright instance created in {}ms", System.currentTimeMillis() - startTime);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(profile.isHeadless());

        if (!profile.getArgs().isEmpty()) {
            launchOptions.setArgs(profile.getArgs());
        }
        if (profile.getChannel() != null) {
            launchOptions.setChannel(profile.getChannel());
        }

        long browserLaunchStart = System.currentTimeMillis();
        this.browser = playwright.chromium().launch(launchOptions);
        logger.info("[SESSION] Chromium browser launched in {}ms", System.currentTimeMillis() - browserLaunchStart);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(profile.getViewportWidth(), profile.getViewportHeight())
                .setAcceptDownloads(profile.isAcceptDownloads());

        if (profile.getUserAgent() != null) {
            contextOptions.setUserAgent(profile.getUserAgent());
        }

        if (profile.getProxy() != null) {
            BrowserProfile.ProxySettings ps = profile.getProxy();
            contextOptions.setProxy(new com.microsoft.playwright.options.Proxy(ps.getServer())
                    .setUsername(ps.getUsername())
                    .setPassword(ps.getPassword()));
        }

        this.context = browser.newContext(contextOptions);
        logger.info("[SESSION] Browser context created");

        if (!profile.getExtraHeaders().isEmpty()) {
            context.setExtraHTTPHeaders(profile.getExtraHeaders());
            logger.info("[SESSION] Extra headers set: {}", profile.getExtraHeaders().keySet());
        }

        this.currentPage = context.newPage();
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("[SESSION] Browser session started successfully in {}ms (page ready)", totalTime);
    }

    /**
     * Start the browser session with video recording enabled.
     * Videos are saved to the specified directory when the context is closed.
     *
     * @param videoDir the directory to save recorded videos
     */
    public void startWithVideoRecording(Path videoDir) {
        logger.info("[SESSION] Starting browser session with VIDEO RECORDING");
        logger.info("[SESSION]   headless={}, viewport={}x{}, videoDir={}",
                profile.isHeadless(), profile.getViewportWidth(), profile.getViewportHeight(), videoDir);

        long startTime = System.currentTimeMillis();
        this.playwright = Playwright.create();
        logger.info("[SESSION] Playwright instance created in {}ms", System.currentTimeMillis() - startTime);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(profile.isHeadless());

        if (!profile.getArgs().isEmpty()) {
            launchOptions.setArgs(profile.getArgs());
        }
        if (profile.getChannel() != null) {
            launchOptions.setChannel(profile.getChannel());
        }

        long browserLaunchStart = System.currentTimeMillis();
        this.browser = playwright.chromium().launch(launchOptions);
        logger.info("[SESSION] Chromium browser launched in {}ms", System.currentTimeMillis() - browserLaunchStart);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(profile.getViewportWidth(), profile.getViewportHeight())
                .setAcceptDownloads(profile.isAcceptDownloads())
                .setRecordVideoDir(videoDir)
                .setRecordVideoSize(profile.getViewportWidth(), profile.getViewportHeight());

        if (profile.getUserAgent() != null) {
            contextOptions.setUserAgent(profile.getUserAgent());
        }

        if (profile.getProxy() != null) {
            BrowserProfile.ProxySettings ps = profile.getProxy();
            contextOptions.setProxy(new com.microsoft.playwright.options.Proxy(ps.getServer())
                    .setUsername(ps.getUsername())
                    .setPassword(ps.getPassword()));
        }

        this.context = browser.newContext(contextOptions);
        logger.info("[SESSION] Browser context created with video recording enabled ({}x{})",
                profile.getViewportWidth(), profile.getViewportHeight());

        if (!profile.getExtraHeaders().isEmpty()) {
            context.setExtraHTTPHeaders(profile.getExtraHeaders());
        }

        this.currentPage = context.newPage();
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("[SESSION] Browser session started with video recording in {}ms", totalTime);
    }

    /**
     * Navigate the current page to a URL.
     */
    public void navigateTo(String url) {
        logger.info("[NAV] Navigating to: {}", url);
        ensureStarted();
        long start = System.currentTimeMillis();
        currentPage.navigate(url);
        waitForPageLoad();
        logger.info("[NAV] Navigation completed in {}ms — final URL: {}",
                System.currentTimeMillis() - start, currentPage.url());
    }

    /**
     * Wait for the page to reach a stable state.
     */
    public void waitForPageLoad() {
        try {
            currentPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
            Thread.sleep(profile.getMinimumPageLoadWaitMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the current browser state including DOM, screenshot, and page info.
     */
    public BrowserState getState(boolean includeScreenshot) {
        ensureStarted();
        long start = System.currentTimeMillis();
        logger.debug("[STATE] Extracting browser state (includeScreenshot={})", includeScreenshot);

        String url = currentPage.url();
        String title = currentPage.title();
        logger.debug("[STATE] Current page — URL: {}, Title: '{}'", url, title);

        // Collect tab info
        List<BrowserState.TabInfo> tabs = new ArrayList<>();
        List<Page> pages = context.pages();
        int activeIndex = 0;
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            tabs.add(new BrowserState.TabInfo(i, p.url(), p.title()));
            if (p == currentPage) {
                activeIndex = i;
            }
        }

        // Extract DOM state
        DomState domState = domService.extractState(currentPage);

        // Take screenshot
        byte[] screenshot = null;
        if (includeScreenshot) {
            screenshot = currentPage.screenshot(new Page.ScreenshotOptions().setFullPage(false));
        }

        // Get page info
        BrowserState.PageInfo pageInfo = getPageInfo();

        long elapsed = System.currentTimeMillis() - start;
        logger.debug("[STATE] Browser state extracted in {}ms — {} tabs, {} elements, screenshot={}",
                elapsed, tabs.size(),
                domState != null ? domState.getElements().size() : 0,
                screenshot != null ? screenshot.length + " bytes" : "none");
        return new BrowserState(url, title, tabs, activeIndex, screenshot, domState, pageInfo);
    }

    /**
     * Get page scroll and dimension information.
     */
    private BrowserState.PageInfo getPageInfo() {
        try {
            Object result = currentPage.evaluate(
                    "() => ({ scrollY: window.scrollY, pageHeight: document.documentElement.scrollHeight, viewportHeight: window.innerHeight })"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) result;
                double scrollY = ((Number) map.get("scrollY")).doubleValue();
                double pageHeight = ((Number) map.get("pageHeight")).doubleValue();
                double viewportHeight = ((Number) map.get("viewportHeight")).doubleValue();
                return new BrowserState.PageInfo(scrollY, pageHeight, viewportHeight);
            }
        } catch (Exception e) {
            logger.warn("Failed to get page info: {}", e.getMessage());
        }
        return new BrowserState.PageInfo(0, 0, profile.getViewportHeight());
    }

    /**
     * Click on a DOM element by its index.
     */
    public void clickElement(int elementIndex) {
        ensureStarted();
        DomState state = domService.extractState(currentPage);
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            logger.warn("[CLICK] Element with index {} not found in {} available elements",
                    elementIndex, state.getElements().size());
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        logger.info("[CLICK] Clicking element [{}]: {} (tag={}, selector={})",
                elementIndex, element.getDescription(), element.getTagName(), element.buildSelector());

        String selector = element.buildSelector();
        try {
            currentPage.locator(selector).first().click(new Locator.ClickOptions().setTimeout(5000));
            logger.info("[CLICK] Direct click succeeded on element [{}]", elementIndex);
        } catch (Exception e) {
            // Fallback: try JavaScript click
            logger.warn("[CLICK] Direct click failed on element [{}]: {} — falling back to JS click",
                    elementIndex, e.getMessage());
            currentPage.evaluate("(selector) => { document.querySelector(selector)?.click(); }", selector);
            logger.info("[CLICK] JS click fallback executed for element [{}]", elementIndex);
        }
        waitAfterAction();
    }

    /**
     * Type text into a DOM element by its index.
     */
    public void typeText(int elementIndex, String text) {
        ensureStarted();
        DomState state = domService.extractState(currentPage);
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            logger.warn("[TYPE] Element with index {} not found in {} available elements",
                    elementIndex, state.getElements().size());
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        logger.info("[TYPE] Typing into element [{}]: '{}' (tag={}, selector={})",
                elementIndex, text, element.getTagName(), element.buildSelector());

        String selector = element.buildSelector();
        Locator locator = currentPage.locator(selector).first();
        locator.click();
        locator.fill(text);
        logger.info("[TYPE] Text input completed for element [{}]", elementIndex);
        waitAfterAction();
    }

    /**
     * Scroll the page or a specific element.
     */
    public void scroll(boolean down, int pixels) {
        ensureStarted();
        int scrollAmount = down ? pixels : -pixels;
        currentPage.evaluate("(amount) => window.scrollBy(0, amount)", scrollAmount);
        waitAfterAction();
    }

    /**
     * Go back in browser history.
     */
    public void goBack() {
        ensureStarted();
        currentPage.goBack();
        waitForPageLoad();
    }

    /**
     * Switch to a tab by index.
     */
    public void switchTab(int tabIndex) {
        ensureStarted();
        List<Page> pages = context.pages();
        if (tabIndex >= 0 && tabIndex < pages.size()) {
            currentPage = pages.get(tabIndex);
            currentPage.bringToFront();
            logger.info("Switched to tab {}: {}", tabIndex, currentPage.url());
        } else {
            throw new RuntimeException("Invalid tab index: " + tabIndex);
        }
    }

    /**
     * Close a tab by index.
     */
    public void closeTab(int tabIndex) {
        ensureStarted();
        List<Page> pages = context.pages();
        if (tabIndex >= 0 && tabIndex < pages.size()) {
            Page pageToClose = pages.get(tabIndex);
            if (pageToClose == currentPage && pages.size() > 1) {
                // Switch to another tab first
                int newIndex = tabIndex > 0 ? tabIndex - 1 : 1;
                currentPage = pages.get(newIndex);
            }
            pageToClose.close();
            logger.info("Closed tab {}", tabIndex);
        }
    }

    /**
     * Open a new tab with the given URL.
     */
    public void openNewTab(String url) {
        ensureStarted();
        currentPage = context.newPage();
        if (url != null && !url.isEmpty()) {
            currentPage.navigate(url);
            waitForPageLoad();
        }
        logger.info("Opened new tab: {}", url);
    }

    /**
     * Send keyboard keys (e.g., "Enter", "Tab", "Control+a").
     */
    public void sendKeys(String keys) {
        ensureStarted();
        currentPage.keyboard().press(keys);
        waitAfterAction();
    }

    /**
     * Extract the text content of the current page.
     */
    public String extractContent() {
        ensureStarted();
        return currentPage.innerText("body");
    }

    /**
     * Take a screenshot and return as base64 string.
     */
    public String takeScreenshotBase64() {
        ensureStarted();
        byte[] bytes = currentPage.screenshot(new Page.ScreenshotOptions().setFullPage(false));
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Upload a file to an element.
     */
    public void uploadFile(int elementIndex, Path filePath) {
        ensureStarted();
        DomState state = domService.extractState(currentPage);
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        String selector = element.buildSelector();
        currentPage.locator(selector).first().setInputFiles(filePath);
        waitAfterAction();
    }

    /**
     * Select a dropdown option by value or label.
     */
    public void selectDropdownOption(int elementIndex, String value) {
        ensureStarted();
        DomState state = domService.extractState(currentPage);
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        String selector = element.buildSelector();
        try {
            currentPage.locator(selector).first().selectOption(value);
        } catch (Exception e) {
            // Try selecting by label
            currentPage.locator(selector).first().selectOption(
                    new com.microsoft.playwright.options.SelectOption().setLabel(value));
        }
        waitAfterAction();
    }

    /**
     * Get the dropdown options for an element.
     */
    public List<String> getDropdownOptions(int elementIndex) {
        ensureStarted();
        DomState state = domService.extractState(currentPage);
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        String selector = element.buildSelector();
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) currentPage.evaluate(
                "(sel) => Array.from(document.querySelector(sel)?.options || []).map(o => o.text + ' (' + o.value + ')')",
                selector);
        return options != null ? options : new ArrayList<>();
    }

    /**
     * Hover over a DOM element by its index.
     */
    public void hoverElement(int elementIndex) {
        ensureStarted();
        DomState state = domService.extractState(currentPage);
        DomElement element = state.getElementByIndex(elementIndex);
        if (element == null) {
            throw new RuntimeException("Element with index " + elementIndex + " not found");
        }
        logger.info("Hovering over element [{}]: {}", elementIndex, element.getDescription());
        String selector = element.buildSelector();
        currentPage.locator(selector).first().hover();
        waitAfterAction();
    }

    /**
     * Drag one element and drop it onto another.
     */
    public void dragAndDrop(int sourceIndex, int targetIndex) {
        ensureStarted();
        DomState state = domService.extractState(currentPage);
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
        currentPage.locator(sourceSelector).first().dragTo(currentPage.locator(targetSelector).first());
        waitAfterAction();
    }

    /**
     * Move the mouse to specific coordinates.
     */
    public void mouseMove(double x, double y) {
        ensureStarted();
        currentPage.mouse().move(x, y);
        waitAfterAction();
    }

    /**
     * Execute JavaScript on the page.
     */
    public Object executeJavaScript(String script) {
        ensureStarted();
        return currentPage.evaluate(script);
    }

    /**
     * Get the underlying Playwright Page object.
     */
    public Page getCurrentPage() {
        return currentPage;
    }

    /**
     * Get the underlying Playwright BrowserContext.
     */
    public BrowserContext getContext() {
        return context;
    }

    public BrowserProfile getProfile() {
        return profile;
    }

    /**
     * Check if the browser session is started.
     */
    public boolean isStarted() {
        return browser != null && currentPage != null;
    }

    private void waitAfterAction() {
        try {
            Thread.sleep(profile.getWaitBetweenActionsMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void ensureStarted() {
        if (browser == null || currentPage == null) {
            throw new IllegalStateException("Browser session not started. Call start() first.");
        }
    }

    @Override
    public void close() {
        logger.info("[SESSION] Closing browser session");
        long start = System.currentTimeMillis();
        try {
            if (context != null) {
                logger.info("[SESSION] Closing browser context (videos will be finalized)");
                context.close();
                logger.info("[SESSION] Browser context closed");
            }
            if (browser != null) {
                browser.close();
                logger.info("[SESSION] Browser closed");
            }
            if (playwright != null) {
                playwright.close();
                logger.info("[SESSION] Playwright closed");
            }
            logger.info("[SESSION] Browser session closed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.warn("[SESSION] Error closing browser session: {}", e.getMessage());
        }
    }
}
