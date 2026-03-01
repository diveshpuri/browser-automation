package com.browserautomation.dom;

import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced DOM extraction service with full Shadow DOM support.
 *
 * Recursively traverses shadow roots in Angular, React, and other
 * web component frameworks to find interactive elements that would
 * be invisible to standard DOM queries.
 *
 * Works with:
 * - Angular Material (mat-*, cdk-*)
 * - Angular CDK overlays
 * - Ionic components
 * - Lit/LitElement components
 * - Shoelace, Vaadin, and other web component libraries
 * - Custom elements with shadow DOM
 */
public class ShadowDomService extends DomService {

    private static final Logger logger = LoggerFactory.getLogger(ShadowDomService.class);

    private final SelectorScorer selectorScorer;
    private boolean shadowDomEnabled;

    public ShadowDomService() {
        this(true);
    }

    public ShadowDomService(boolean shadowDomEnabled) {
        this.shadowDomEnabled = shadowDomEnabled;
        this.selectorScorer = new SelectorScorer();
    }

    /**
     * Extract DOM state, including elements inside Shadow DOM trees.
     */
    @Override
    @SuppressWarnings("unchecked")
    public DomState extractState(Page page) {
        if (!shadowDomEnabled) {
            return super.extractState(page);
        }

        logger.debug("Extracting DOM state with Shadow DOM support from: {}", page.url());

        List<DomElement> elements = new ArrayList<>();
        try {
            Object result = page.evaluate(SHADOW_DOM_EXTRACT_JS);
            if (result instanceof List) {
                List<Map<String, Object>> rawElements = (List<Map<String, Object>>) result;
                for (Map<String, Object> raw : rawElements) {
                    DomElement element = parseShadowElement(raw);
                    elements.add(element);
                }
            }
        } catch (Exception e) {
            logger.warn("Shadow DOM extraction failed, falling back to standard: {}", e.getMessage());
            return super.extractState(page);
        }

        logger.debug("Extracted {} elements (including shadow DOM)", elements.size());
        return new DomState(elements);
    }

    /**
     * Get the best Playwright-compatible selector for an element.
     * Uses the SelectorScorer to find the most robust selector.
     *
     * @param element the element to build a selector for
     * @return the best scored selector string
     */
    public String getBestSelector(DomElement element) {
        boolean inShadow = element.getAttributes().containsKey("data-shadow-host");
        String shadowHost = element.getAttributes().get("data-shadow-host");

        SelectorStrategy best = selectorScorer.getBestSelector(element, inShadow);
        if (best == null) {
            return element.buildSelector();
        }

        // For shadow DOM elements, use Playwright's >> piercing combinator
        if (inShadow && shadowHost != null && !shadowHost.isEmpty()) {
            return shadowHost + " >> " + best.getSelector();
        }

        return best.getSelector();
    }

    /**
     * Get all scored selector candidates for an element.
     *
     * @param element the element
     * @return list of scored selectors, sorted by score descending
     */
    public List<SelectorStrategy> getScoredSelectors(DomElement element) {
        boolean inShadow = element.getAttributes().containsKey("data-shadow-host");
        return selectorScorer.generateAndScoreCandidates(element, inShadow);
    }

    /**
     * Get the underlying SelectorScorer.
     */
    public SelectorScorer getSelectorScorer() {
        return selectorScorer;
    }

    /**
     * Enable or disable Shadow DOM traversal.
     */
    public void setShadowDomEnabled(boolean enabled) {
        this.shadowDomEnabled = enabled;
    }

    public boolean isShadowDomEnabled() {
        return shadowDomEnabled;
    }

    @SuppressWarnings("unchecked")
    private DomElement parseShadowElement(Map<String, Object> raw) {
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

        // Store shadow DOM metadata in attributes for later use
        Boolean inShadow = (Boolean) raw.getOrDefault("inShadowDom", false);
        if (Boolean.TRUE.equals(inShadow)) {
            String shadowHost = (String) raw.getOrDefault("shadowHostSelector", "");
            attributes.put("data-shadow-host", shadowHost);
            attributes.put("data-in-shadow-dom", "true");
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

    /**
     * JavaScript that recursively traverses the DOM including Shadow DOM trees
     * to find all interactive elements.
     */
    private static final String SHADOW_DOM_EXTRACT_JS = """
            () => {
                const INTERACTIVE_SELECTOR = 'a[href], button, input, textarea, select, ' +
                    '[role="button"], [role="link"], [role="tab"], [role="menuitem"], ' +
                    '[role="checkbox"], [role="radio"], [role="switch"], [role="combobox"], ' +
                    '[role="textbox"], [role="searchbox"], [role="option"], [role="slider"], ' +
                    '[role="spinbutton"], [contenteditable="true"], [tabindex], [onclick], ' +
                    'summary, details';
            
                const results = [];
                let globalIndex = 0;
            
                function isVisible(el) {
                    const rect = el.getBoundingClientRect();
                    if (rect.width === 0 && rect.height === 0) return false;
                    const style = window.getComputedStyle(el);
                    return style.display !== 'none' &&
                           style.visibility !== 'hidden' &&
                           parseFloat(style.opacity) > 0;
                }
            
                function getAttributes(el) {
                    const attrs = {};
                    for (const attr of el.attributes) {
                        attrs[attr.name] = attr.value;
                    }
                    return attrs;
                }
            
                function getTextContent(el) {
                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                        return (el.value || '').trim().substring(0, 200);
                    }
                    return (el.innerText || el.textContent || '').trim().substring(0, 200);
                }
            
                function buildShadowHostSelector(el) {
                    // Build a selector for the shadow host element
                    if (el.id) return '#' + CSS.escape(el.id);
                    if (el.getAttribute('data-testid')) return '[data-testid="' + el.getAttribute('data-testid') + '"]';
                    if (el.className) {
                        const classes = el.className.split(' ').filter(c => c.length > 0).slice(0, 2);
                        if (classes.length > 0) return el.tagName.toLowerCase() + '.' + classes.join('.');
                    }
                    return el.tagName.toLowerCase();
                }
            
                function processElement(el, inShadowDom, shadowHostSelector) {
                    const rect = el.getBoundingClientRect();
                    const style = window.getComputedStyle(el);
                    const visible = isVisible(el);
                    if (!visible) return;
            
                    const viewportH = window.innerHeight;
                    const viewportW = window.innerWidth;
                    const threshold = 1000;
                    const inView = rect.top < viewportH + threshold && rect.bottom > -threshold &&
                                   rect.left < viewportW + threshold && rect.right > -threshold;
            
                    const tagName = el.tagName;
                    const isInteractive = ['A', 'BUTTON', 'INPUT', 'TEXTAREA', 'SELECT'].includes(tagName) ||
                        el.getAttribute('role') !== null ||
                        el.getAttribute('onclick') !== null ||
                        el.getAttribute('contenteditable') === 'true' ||
                        el.getAttribute('tabindex') !== null;
                    const isScrollable = el.scrollHeight > el.clientHeight || el.scrollWidth > el.clientWidth;
                    const role = el.getAttribute('role') || el.tagName.toLowerCase();
                    const ariaLabel = el.getAttribute('aria-label') || '';
            
                    results.push({
                        index: globalIndex++,
                        tagName: tagName,
                        attributes: getAttributes(el),
                        textContent: getTextContent(el),
                        isVisible: visible,
                        isInteractive: isInteractive,
                        isScrollable: isScrollable,
                        role: role,
                        ariaLabel: ariaLabel,
                        boundingBox: { x: rect.x, y: rect.y, width: rect.width, height: rect.height },
                        inViewport: inView,
                        inShadowDom: inShadowDom,
                        shadowHostSelector: shadowHostSelector || ''
                    });
                }
            
                function traverseNode(root, inShadowDom, shadowHostSelector) {
                    // Query interactive elements in this DOM tree
                    let elements;
                    try {
                        elements = root.querySelectorAll(INTERACTIVE_SELECTOR);
                    } catch(e) {
                        return;
                    }
            
                    for (const el of elements) {
                        processElement(el, inShadowDom, shadowHostSelector);
                    }
            
                    // Recursively traverse shadow roots
                    const allElements = root.querySelectorAll('*');
                    for (const el of allElements) {
                        if (el.shadowRoot) {
                            const hostSelector = buildShadowHostSelector(el);
                            traverseNode(el.shadowRoot, true, hostSelector);
                        }
                    }
                }
            
                // Start from document root
                traverseNode(document, false, '');
            
                return results;
            }
            """;
}
