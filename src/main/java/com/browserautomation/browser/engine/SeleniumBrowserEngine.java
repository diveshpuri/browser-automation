package com.browserautomation.browser.engine;

import com.browserautomation.browser.BrowserProfile;
import com.browserautomation.browser.BrowserState;
import com.browserautomation.dom.DomElement;
import com.browserautomation.dom.DomState;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * Selenium WebDriver-based browser engine implementation.
 * Provides an alternative to Playwright for browser automation.
 *
 * <p>Supports all core browser operations through Selenium WebDriver,
 * including navigation, element interaction, screenshots, tab management,
 * and JavaScript execution.</p>
 */
public class SeleniumBrowserEngine implements BrowserEngine {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumBrowserEngine.class);

    /**
     * JavaScript for extracting interactive elements — identical logic to DomService
     * but executed via Selenium's JavascriptExecutor.
     */
    private static final String EXTRACT_ELEMENTS_JS = """
            var selector = 'a[href], button, input, textarea, select, [role=\\'button\\'], [role=\\'link\\'], ' +
                '[role=\\'tab\\'], [role=\\'menuitem\\'], [role=\\'checkbox\\'], [role=\\'radio\\'], ' +
                '[role=\\'switch\\'], [role=\\'combobox\\'], [role=\\'textbox\\'], [role=\\'searchbox\\'], ' +
                '[role=\\'option\\'], [role=\\'slider\\'], [role=\\'spinbutton\\'], ' +
                '[contenteditable=\\'true\\'], [tabindex], [onclick], summary, details';
            var elements = document.querySelectorAll(selector);
            var results = [];
            var index = 0;
            for (var i = 0; i < elements.length; i++) {
                var el = elements[i];
                var rect = el.getBoundingClientRect();
                var style = window.getComputedStyle(el);
                var isVisible = rect.width > 0 && rect.height > 0 &&
                    style.display !== 'none' && style.visibility !== 'hidden' &&
                    parseFloat(style.opacity) > 0;
                if (!isVisible) continue;
                var attrs = {};
                for (var j = 0; j < el.attributes.length; j++) {
                    attrs[el.attributes[j].name] = el.attributes[j].value;
                }
                var textContent = '';
                if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                    textContent = el.value || '';
                } else {
                    textContent = el.innerText || el.textContent || '';
                }
                textContent = textContent.trim().substring(0, 200);
                var tagName = el.tagName;
                var isInteractive = ['A', 'BUTTON', 'INPUT', 'TEXTAREA', 'SELECT'].indexOf(tagName) >= 0 ||
                    el.getAttribute('role') !== null || el.getAttribute('onclick') !== null ||
                    el.getAttribute('contenteditable') === 'true' || el.getAttribute('tabindex') !== null;
                var isScrollable = el.scrollHeight > el.clientHeight || el.scrollWidth > el.clientWidth;
                var role = el.getAttribute('role') || el.tagName.toLowerCase();
                var ariaLabel = el.getAttribute('aria-label') || '';
                results.push({
                    index: index, tagName: tagName, attributes: attrs, textContent: textContent,
                    isVisible: isVisible, isInteractive: isInteractive, isScrollable: isScrollable,
                    role: role, ariaLabel: ariaLabel,
                    boundingBox: { x: rect.x, y: rect.y, width: rect.width, height: rect.height }
                });
                index++;
            }
            return results;
            """;

    private WebDriver driver;
    private BrowserProfile profile;
    private boolean started;

    public SeleniumBrowserEngine() {
        this.started = false;
    }

    @Override
    public void start(BrowserProfile profile) {
        this.profile = profile;
        logger.info("[SELENIUM] Starting Selenium engine");
        long startTime = System.currentTimeMillis();

        WebDriverManager.chromedriver().setup();
        logger.info("[SELENIUM] ChromeDriver set up in {}ms", System.currentTimeMillis() - startTime);

        ChromeOptions options = buildChromeOptions(profile);

        long driverStart = System.currentTimeMillis();
        this.driver = new ChromeDriver(options);
        logger.info("[SELENIUM] ChromeDriver launched in {}ms", System.currentTimeMillis() - driverStart);

        // Set window size to match viewport
        driver.manage().window().setSize(
                new Dimension(profile.getViewportWidth(), profile.getViewportHeight()));

        // Set implicit wait
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(profile.getWaitBetweenActionsMs()));

        this.started = true;
        logger.info("[SELENIUM] Engine started in {}ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public void startWithVideoRecording(BrowserProfile profile, Path videoDir) {
        // Selenium doesn't have built-in video recording like Playwright.
        // Start normally; video recording would need a separate screen recorder.
        logger.warn("[SELENIUM] Video recording is not natively supported by Selenium. Starting without video.");
        start(profile);
    }

    private ChromeOptions buildChromeOptions(BrowserProfile profile) {
        ChromeOptions options = new ChromeOptions();

        if (profile.isHeadless()) {
            options.addArguments("--headless=new");
        }

        // Standard Chrome options for automation
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments(
                String.format("--window-size=%d,%d", profile.getViewportWidth(), profile.getViewportHeight()));

        if (profile.isDisableSecurity()) {
            options.addArguments("--disable-web-security");
            options.addArguments("--allow-running-insecure-content");
        }

        if (profile.getUserAgent() != null) {
            options.addArguments("--user-agent=" + profile.getUserAgent());
        }

        if (profile.getProxy() != null) {
            Proxy seleniumProxy = new Proxy();
            seleniumProxy.setHttpProxy(profile.getProxy().getServer());
            seleniumProxy.setSslProxy(profile.getProxy().getServer());
            options.setProxy(seleniumProxy);
        }

        // Add any custom args
        for (String arg : profile.getArgs()) {
            options.addArguments(arg);
        }

        if (profile.isAcceptDownloads() && profile.getDownloadsPath() != null) {
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", profile.getDownloadsPath().toAbsolutePath().toString());
            prefs.put("download.prompt_for_download", false);
            options.setExperimentalOption("prefs", prefs);
        }

        return options;
    }

    @Override
    public void navigateTo(String url) {
        ensureStarted();
        driver.get(url);
    }

    @Override
    public void waitForPageLoad(long minimumWaitMs) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(d -> ((JavascriptExecutor) d)
                    .executeScript("return document.readyState").equals("complete"));
            Thread.sleep(minimumWaitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.debug("[SELENIUM] Page load wait exception: {}", e.getMessage());
        }
    }

    @Override
    public String getCurrentUrl() {
        ensureStarted();
        return driver.getCurrentUrl();
    }

    @Override
    public String getPageTitle() {
        ensureStarted();
        return driver.getTitle();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DomState extractDomState() {
        ensureStarted();
        long start = System.currentTimeMillis();
        logger.info("[SELENIUM-DOM] Extracting DOM state from: {}", driver.getCurrentUrl());

        List<DomElement> elements = new ArrayList<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(EXTRACT_ELEMENTS_JS);

            if (result instanceof List) {
                List<Map<String, Object>> rawElements = (List<Map<String, Object>>) result;
                logger.info("[SELENIUM-DOM] Raw elements found: {}", rawElements.size());
                for (Map<String, Object> raw : rawElements) {
                    elements.add(parseElement(raw));
                }
            }
        } catch (Exception e) {
            logger.warn("[SELENIUM-DOM] Failed to extract DOM: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }

        logger.info("[SELENIUM-DOM] Extracted {} elements in {}ms", elements.size(), System.currentTimeMillis() - start);
        return new DomState(elements);
    }

    @SuppressWarnings("unchecked")
    private DomElement parseElement(Map<String, Object> raw) {
        int index = ((Number) raw.get("index")).intValue();
        String tagName = (String) raw.get("tagName");

        Map<String, String> attributes = new HashMap<>();
        Object attrsObj = raw.get("attributes");
        if (attrsObj instanceof Map) {
            Map<String, Object> rawAttrs = (Map<String, Object>) attrsObj;
            for (Map.Entry<String, Object> entry : rawAttrs.entrySet()) {
                attributes.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        String textContent = (String) raw.getOrDefault("textContent", "");
        boolean isVisible = (Boolean) raw.getOrDefault("isVisible", false);
        boolean isInteractive = (Boolean) raw.getOrDefault("isInteractive", false);
        boolean isScrollable = (Boolean) raw.getOrDefault("isScrollable", false);
        String role = (String) raw.getOrDefault("role", "");
        String ariaLabel = (String) raw.getOrDefault("ariaLabel", "");

        DomElement.BoundingBox boundingBox = null;
        Object bbObj = raw.get("boundingBox");
        if (bbObj instanceof Map) {
            Map<String, Object> bb = (Map<String, Object>) bbObj;
            boundingBox = new DomElement.BoundingBox(
                    ((Number) bb.get("x")).doubleValue(),
                    ((Number) bb.get("y")).doubleValue(),
                    ((Number) bb.get("width")).doubleValue(),
                    ((Number) bb.get("height")).doubleValue());
        }

        return new DomElement(index, tagName, attributes, textContent,
                isVisible, isInteractive, isScrollable, role, ariaLabel, boundingBox);
    }

    @Override
    public byte[] takeScreenshot() {
        ensureStarted();
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BrowserState.PageInfo getPageInfo() {
        ensureStarted();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Map<String, Object> result = (Map<String, Object>) js.executeScript(
                    "return { scrollY: window.scrollY, " +
                            "pageHeight: document.documentElement.scrollHeight, " +
                            "viewportHeight: window.innerHeight };");
            if (result != null) {
                double scrollY = ((Number) result.get("scrollY")).doubleValue();
                double pageHeight = ((Number) result.get("pageHeight")).doubleValue();
                double viewportHeight = ((Number) result.get("viewportHeight")).doubleValue();
                return new BrowserState.PageInfo(scrollY, pageHeight, viewportHeight);
            }
        } catch (Exception e) {
            logger.warn("[SELENIUM] Failed to get page info: {}", e.getMessage());
        }
        return new BrowserState.PageInfo(0, 0, profile != null ? profile.getViewportHeight() : 720);
    }

    @Override
    public void clickElement(String selector, DomElement element) {
        ensureStarted();
        try {
            WebElement webElement = findElement(selector);
            scrollIntoView(webElement);
            webElement.click();
        } catch (Exception e) {
            logger.warn("[SELENIUM] Direct click failed on '{}': {} — trying JS click", selector, e.getMessage());
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("document.querySelector(arguments[0])?.click();", selector);
            } catch (Exception jsE) {
                throw new RuntimeException("Failed to click element: " + jsE.getMessage(), jsE);
            }
        }
    }

    @Override
    public void typeText(String selector, String text) {
        ensureStarted();
        WebElement webElement = findElement(selector);
        scrollIntoView(webElement);
        webElement.click();
        webElement.clear();
        webElement.sendKeys(text);
    }

    @Override
    public void scroll(boolean down, int pixels) {
        ensureStarted();
        int scrollAmount = down ? pixels : -pixels;
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0, arguments[0]);", scrollAmount);
    }

    @Override
    public void goBack() {
        ensureStarted();
        driver.navigate().back();
    }

    @Override
    public void switchTab(int tabIndex) {
        ensureStarted();
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        if (tabIndex >= 0 && tabIndex < handles.size()) {
            driver.switchTo().window(handles.get(tabIndex));
            logger.info("[SELENIUM] Switched to tab {}: {}", tabIndex, driver.getCurrentUrl());
        } else {
            throw new RuntimeException("Invalid tab index: " + tabIndex);
        }
    }

    @Override
    public void closeTab(int tabIndex) {
        ensureStarted();
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        if (tabIndex >= 0 && tabIndex < handles.size()) {
            String currentHandle = driver.getWindowHandle();
            String handleToClose = handles.get(tabIndex);
            driver.switchTo().window(handleToClose);
            driver.close();
            // Switch back to another tab if we closed the current one
            if (handleToClose.equals(currentHandle) && handles.size() > 1) {
                int newIndex = tabIndex > 0 ? tabIndex - 1 : 0;
                List<String> remaining = new ArrayList<>(driver.getWindowHandles());
                if (newIndex < remaining.size()) {
                    driver.switchTo().window(remaining.get(newIndex));
                }
            }
            logger.info("[SELENIUM] Closed tab {}", tabIndex);
        }
    }

    @Override
    public void openNewTab(String url) {
        ensureStarted();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.open(arguments[0] || 'about:blank', '_blank');", url != null ? url : "");
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(handles.get(handles.size() - 1));
        logger.info("[SELENIUM] Opened new tab: {}", url);
    }

    @Override
    public void sendKeys(String keys) {
        ensureStarted();
        // Map common key names to Selenium Keys
        Keys seleniumKey = mapToSeleniumKey(keys);
        if (seleniumKey != null) {
            new Actions(driver).sendKeys(seleniumKey).perform();
        } else if (keys.contains("+")) {
            // Handle key combinations like "Control+a"
            sendKeyCombination(keys);
        } else {
            new Actions(driver).sendKeys(keys).perform();
        }
    }

    private Keys mapToSeleniumKey(String key) {
        return switch (key.toLowerCase()) {
            case "enter", "return" -> Keys.ENTER;
            case "tab" -> Keys.TAB;
            case "escape", "esc" -> Keys.ESCAPE;
            case "backspace" -> Keys.BACK_SPACE;
            case "delete" -> Keys.DELETE;
            case "arrowup", "up" -> Keys.ARROW_UP;
            case "arrowdown", "down" -> Keys.ARROW_DOWN;
            case "arrowleft", "left" -> Keys.ARROW_LEFT;
            case "arrowright", "right" -> Keys.ARROW_RIGHT;
            case "home" -> Keys.HOME;
            case "end" -> Keys.END;
            case "pageup" -> Keys.PAGE_UP;
            case "pagedown" -> Keys.PAGE_DOWN;
            case "space", " " -> Keys.SPACE;
            case "f1" -> Keys.F1;
            case "f5" -> Keys.F5;
            case "f11" -> Keys.F11;
            case "f12" -> Keys.F12;
            default -> null;
        };
    }

    private void sendKeyCombination(String keys) {
        String[] parts = keys.split("\\+");
        Actions actions = new Actions(driver);
        List<CharSequence> modifiers = new ArrayList<>();

        for (int i = 0; i < parts.length - 1; i++) {
            Keys mod = switch (parts[i].trim().toLowerCase()) {
                case "control", "ctrl" -> Keys.CONTROL;
                case "shift" -> Keys.SHIFT;
                case "alt" -> Keys.ALT;
                case "meta", "command", "cmd" -> Keys.META;
                default -> null;
            };
            if (mod != null) {
                modifiers.add(mod);
                actions = actions.keyDown(mod);
            }
        }

        String lastKey = parts[parts.length - 1].trim();
        Keys mappedKey = mapToSeleniumKey(lastKey);
        if (mappedKey != null) {
            actions = actions.sendKeys(mappedKey);
        } else {
            actions = actions.sendKeys(lastKey);
        }

        for (CharSequence mod : modifiers) {
            actions = actions.keyUp(mod);
        }

        actions.perform();
    }

    @Override
    public String extractContent() {
        ensureStarted();
        return driver.findElement(By.tagName("body")).getText();
    }

    @Override
    public void uploadFile(String selector, Path filePath) {
        ensureStarted();
        WebElement fileInput = findElement(selector);
        fileInput.sendKeys(filePath.toAbsolutePath().toString());
    }

    @Override
    public void selectDropdownOption(String selector, String value) {
        ensureStarted();
        WebElement element = findElement(selector);
        Select select = new Select(element);
        try {
            select.selectByValue(value);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            try {
                select.selectByVisibleText(value);
            } catch (org.openqa.selenium.NoSuchElementException e2) {
                // Try partial match by iterating options
                boolean found = false;
                for (WebElement opt : select.getOptions()) {
                    if (opt.getText().contains(value)) {
                        select.selectByVisibleText(opt.getText());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException("No dropdown option matching: " + value);
                }
            }
        }
    }

    @Override
    public List<String> getDropdownOptions(String selector) {
        ensureStarted();
        WebElement element = findElement(selector);
        Select select = new Select(element);
        List<String> options = new ArrayList<>();
        for (WebElement opt : select.getOptions()) {
            options.add(opt.getText() + " (" + opt.getAttribute("value") + ")");
        }
        return options;
    }

    @Override
    public void hoverElement(String selector) {
        ensureStarted();
        WebElement element = findElement(selector);
        scrollIntoView(element);
        new Actions(driver).moveToElement(element).perform();
    }

    @Override
    public void dragAndDrop(String sourceSelector, String targetSelector) {
        ensureStarted();
        WebElement source = findElement(sourceSelector);
        WebElement target = findElement(targetSelector);
        new Actions(driver).dragAndDrop(source, target).perform();
    }

    @Override
    public void mouseMove(double x, double y) {
        ensureStarted();
        new Actions(driver).moveByOffset((int) x, (int) y).perform();
    }

    @Override
    public Object executeJavaScript(String script) {
        ensureStarted();
        return ((JavascriptExecutor) driver).executeScript(script);
    }

    @Override
    public Object executeJavaScript(String script, Object arg) {
        ensureStarted();
        return ((JavascriptExecutor) driver).executeScript(script, arg);
    }

    @Override
    public List<TabInfo> getTabsInfo() {
        ensureStarted();
        List<TabInfo> tabs = new ArrayList<>();
        String currentHandle = driver.getWindowHandle();
        List<String> handles = new ArrayList<>(driver.getWindowHandles());

        for (int i = 0; i < handles.size(); i++) {
            driver.switchTo().window(handles.get(i));
            tabs.add(new TabInfo(i, driver.getCurrentUrl(), driver.getTitle()));
        }

        // Switch back to original
        driver.switchTo().window(currentHandle);
        return tabs;
    }

    @Override
    public int getActiveTabIndex() {
        ensureStarted();
        String currentHandle = driver.getWindowHandle();
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        return handles.indexOf(currentHandle);
    }

    @Override
    public boolean isStarted() {
        return started && driver != null;
    }

    @Override
    public String getEngineTypeName() {
        return "Selenium";
    }

    /**
     * Get the underlying Selenium WebDriver (for Selenium-specific features).
     */
    public WebDriver getWebDriver() {
        return driver;
    }

    private WebElement findElement(String selector) {
        try {
            return driver.findElement(By.cssSelector(selector));
        } catch (org.openqa.selenium.NoSuchElementException e) {
            // Try XPath fallback if CSS fails
            try {
                return driver.findElement(By.xpath("//*[@data-index='" + selector + "']"));
            } catch (org.openqa.selenium.NoSuchElementException e2) {
                throw new RuntimeException("Element not found with selector: " + selector);
            }
        }
    }

    private void scrollIntoView(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});", element);
            Thread.sleep(100);
        } catch (Exception e) {
            // Non-critical, continue
        }
    }

    private void ensureStarted() {
        if (!started || driver == null) {
            throw new IllegalStateException("Selenium engine not started. Call start() first.");
        }
    }

    @Override
    public void close() {
        try {
            if (driver != null) {
                driver.quit();
                driver = null;
                started = false;
            }
            logger.info("[SELENIUM] Engine closed");
        } catch (Exception e) {
            logger.warn("[SELENIUM] Error closing engine: {}", e.getMessage());
        }
    }
}
