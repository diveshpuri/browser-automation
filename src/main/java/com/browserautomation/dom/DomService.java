package com.browserautomation.dom;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for extracting DOM state from a Playwright page.
 * Identifies interactive elements, assigns indices, and builds a
 * serialized representation suitable for LLM consumption.
 */
public class DomService {

    private static final Logger logger = LoggerFactory.getLogger(DomService.class);

    /**
     * Interactive element selectors to find on the page.
     */
    private static final String INTERACTIVE_SELECTOR = String.join(", ",
            "a[href]",
            "button",
            "input",
            "textarea",
            "select",
            "[role='button']",
            "[role='link']",
            "[role='tab']",
            "[role='menuitem']",
            "[role='checkbox']",
            "[role='radio']",
            "[role='switch']",
            "[role='combobox']",
            "[role='textbox']",
            "[role='searchbox']",
            "[role='option']",
            "[role='slider']",
            "[role='spinbutton']",
            "[contenteditable='true']",
            "[tabindex]",
            "[onclick]",
            "summary",
            "details"
    );

    /**
     * JavaScript to extract comprehensive element information from the page.
     */
    private static final String EXTRACT_ELEMENTS_JS = """
            () => {
                const selector = `SELECTOR_PLACEHOLDER`;
                const elements = document.querySelectorAll(selector);
                const results = [];
                let index = 0;
                
                for (const el of elements) {
                    const rect = el.getBoundingClientRect();
                    const style = window.getComputedStyle(el);
                    
                    // Visibility check
                    const isVisible = rect.width > 0 && rect.height > 0 &&
                        style.display !== 'none' &&
                        style.visibility !== 'hidden' &&
                        parseFloat(style.opacity) > 0;
                    
                    if (!isVisible) continue;
                    
                    // Check if within viewport (with some threshold)
                    const viewportHeight = window.innerHeight;
                    const viewportWidth = window.innerWidth;
                    const threshold = 1000;
                    
                    const inExtendedViewport = rect.top < viewportHeight + threshold &&
                        rect.bottom > -threshold &&
                        rect.left < viewportWidth + threshold &&
                        rect.right > -threshold;
                    
                    // Collect attributes
                    const attrs = {};
                    for (const attr of el.attributes) {
                        attrs[attr.name] = attr.value;
                    }
                    
                    // Get text content (limited)
                    let textContent = '';
                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                        textContent = el.value || '';
                    } else {
                        textContent = el.innerText || el.textContent || '';
                    }
                    textContent = textContent.trim().substring(0, 200);
                    
                    // Determine interactivity
                    const tagName = el.tagName;
                    const isInteractive = ['A', 'BUTTON', 'INPUT', 'TEXTAREA', 'SELECT'].includes(tagName) ||
                        el.getAttribute('role') !== null ||
                        el.getAttribute('onclick') !== null ||
                        el.getAttribute('contenteditable') === 'true' ||
                        el.getAttribute('tabindex') !== null;
                    
                    // Check scrollability
                    const isScrollable = el.scrollHeight > el.clientHeight || el.scrollWidth > el.clientWidth;
                    
                    // Get ARIA properties
                    const role = el.getAttribute('role') || el.tagName.toLowerCase();
                    const ariaLabel = el.getAttribute('aria-label') || '';
                    
                    results.push({
                        index: index,
                        tagName: tagName,
                        attributes: attrs,
                        textContent: textContent,
                        isVisible: isVisible,
                        isInteractive: isInteractive,
                        isScrollable: isScrollable,
                        role: role,
                        ariaLabel: ariaLabel,
                        boundingBox: {
                            x: rect.x,
                            y: rect.y,
                            width: rect.width,
                            height: rect.height
                        },
                        inViewport: inExtendedViewport
                    });
                    index++;
                }
                return results;
            }
            """;

    /**
     * Extract the DOM state from the given page.
     */
    @SuppressWarnings("unchecked")
    public DomState extractState(Page page) {
        long start = System.currentTimeMillis();
        logger.info("[DOM] Extracting DOM state from page: {}", page.url());

        String js = EXTRACT_ELEMENTS_JS.replace("SELECTOR_PLACEHOLDER",
                INTERACTIVE_SELECTOR.replace("'", "\\'"));

        List<DomElement> elements = new ArrayList<>();
        try {
            long evalStart = System.currentTimeMillis();
            Object result = page.evaluate(js);
            long evalDuration = System.currentTimeMillis() - evalStart;
            logger.info("[DOM] JavaScript evaluation completed in {}ms", evalDuration);

            if (result instanceof List) {
                List<Map<String, Object>> rawElements = (List<Map<String, Object>>) result;
                logger.info("[DOM] Raw elements found: {}", rawElements.size());
                for (Map<String, Object> raw : rawElements) {
                    DomElement element = parseElement(raw);
                    elements.add(element);
                }
            }
        } catch (Exception e) {
            logger.warn("[DOM] Failed to extract DOM state: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;
        logger.info("[DOM] Extracted {} interactive elements in {}ms", elements.size(), elapsed);

        // Log element breakdown by tag
        if (!elements.isEmpty() && logger.isDebugEnabled()) {
            Map<String, Integer> tagCounts = new java.util.LinkedHashMap<>();
            for (DomElement el : elements) {
                tagCounts.merge(el.getTagName(), 1, Integer::sum);
            }
            logger.debug("[DOM] Element breakdown: {}", tagCounts);
        }

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
                    ((Number) bb.get("height")).doubleValue()
            );
        }

        return new DomElement(index, tagName, attributes, textContent,
                isVisible, isInteractive, isScrollable, role, ariaLabel, boundingBox);
    }
}
