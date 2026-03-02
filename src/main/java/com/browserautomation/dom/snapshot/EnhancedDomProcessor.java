package com.browserautomation.dom.snapshot;

import com.browserautomation.dom.DomElement;
import com.browserautomation.dom.DomState;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Enhanced DOM processing with CDP-level snapshot data.
 * Supports compound component detection, paint order filtering,
 * new node marking, and enhanced visibility/clickability detection.
 *
 */
public class EnhancedDomProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDomProcessor.class);

    /**
     * Process DOM state with enhanced visibility and clickability detection.
     */
    @SuppressWarnings("unchecked")
    public EnhancedDomState processEnhanced(Page page, DomState basicState) {
        List<EnhancedElement> enhancedElements = new ArrayList<>();
        Set<Integer> newNodeIndices = new HashSet<>();

        try {
            // Enhanced element detection with clickability and stacking context
            Object result = page.evaluate(ENHANCED_EXTRACT_JS);
            if (result instanceof List) {
                List<Map<String, Object>> rawElements = (List<Map<String, Object>>) result;
                for (Map<String, Object> raw : rawElements) {
                    EnhancedElement element = parseEnhancedElement(raw);
                    enhancedElements.add(element);
                }
            }
        } catch (Exception e) {
            logger.warn("Enhanced DOM processing failed, falling back to basic: {}", e.getMessage());
            // Convert basic elements to enhanced
            for (DomElement basic : basicState.getElements()) {
                enhancedElements.add(new EnhancedElement(
                        basic.getIndex(), basic.getTagName(), basic.getAttributes(),
                        basic.getTextContent(), basic.isVisible(), basic.isInteractive(),
                        true, 0, false, null));
            }
        }

        return new EnhancedDomState(enhancedElements, newNodeIndices);
    }

    /**
     * Detect compound components (select, date/time inputs, details/summary).
     */
    @SuppressWarnings("unchecked")
    public List<CompoundComponent> detectCompoundComponents(Page page) {
        List<CompoundComponent> components = new ArrayList<>();
        try {
            Object result = page.evaluate(COMPOUND_DETECT_JS);
            if (result instanceof List) {
                List<Map<String, Object>> rawComponents = (List<Map<String, Object>>) result;
                for (Map<String, Object> raw : rawComponents) {
                    String type = (String) raw.get("type");
                    String selector = (String) raw.get("selector");
                    int index = ((Number) raw.get("index")).intValue();
                    String value = (String) raw.getOrDefault("value", "");
                    List<String> options = raw.containsKey("options") ?
                            (List<String>) raw.get("options") : List.of();
                    boolean expanded = Boolean.TRUE.equals(raw.get("expanded"));

                    components.add(new CompoundComponent(type, selector, index, value, options, expanded));
                }
            }
        } catch (Exception e) {
            logger.debug("Compound component detection failed: {}", e.getMessage());
        }
        return components;
    }

    /**
     * Filter elements by paint order to handle z-index correctly.
     */
    public List<EnhancedElement> filterByPaintOrder(List<EnhancedElement> elements) {
        return elements.stream()
                .filter(e -> e.isVisible() && e.isClickable())
                .sorted(Comparator.comparingInt(EnhancedElement::paintOrder).reversed())
                .toList();
    }

    /**
     * Mark which nodes are new since the last snapshot.
     */
    public Set<Integer> markNewNodes(EnhancedDomState previous, EnhancedDomState current) {
        Set<Integer> newNodes = new HashSet<>();
        Set<String> previousSignatures = new HashSet<>();

        if (previous != null) {
            for (EnhancedElement elem : previous.getElements()) {
                previousSignatures.add(elem.getSignature());
            }
        }

        for (EnhancedElement elem : current.getElements()) {
            if (!previousSignatures.contains(elem.getSignature())) {
                newNodes.add(elem.index());
            }
        }

        current.setNewNodeIndices(newNodes);
        return newNodes;
    }

    @SuppressWarnings("unchecked")
    private EnhancedElement parseEnhancedElement(Map<String, Object> raw) {
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
        boolean isVisible = Boolean.TRUE.equals(raw.get("isVisible"));
        boolean isInteractive = Boolean.TRUE.equals(raw.get("isInteractive"));
        boolean isClickable = Boolean.TRUE.equals(raw.get("isClickable"));
        int paintOrder = ((Number) raw.getOrDefault("paintOrder", 0)).intValue();
        boolean isInStackingContext = Boolean.TRUE.equals(raw.get("isInStackingContext"));

        double[] boundingBox = null;
        Object bbObj = raw.get("boundingBox");
        if (bbObj instanceof Map) {
            Map<String, Object> bb = (Map<String, Object>) bbObj;
            boundingBox = new double[]{
                    ((Number) bb.get("x")).doubleValue(),
                    ((Number) bb.get("y")).doubleValue(),
                    ((Number) bb.get("width")).doubleValue(),
                    ((Number) bb.get("height")).doubleValue()
            };
        }

        return new EnhancedElement(index, tagName, attributes, textContent,
                isVisible, isInteractive, isClickable, paintOrder, isInStackingContext, boundingBox);
    }

    private static final String ENHANCED_EXTRACT_JS = """
        () => {
            const interactiveSelector = 'a[href], button, input, textarea, select, ' +
                '[role="button"], [role="link"], [role="tab"], [role="menuitem"], ' +
                '[role="checkbox"], [role="radio"], [role="switch"], [role="combobox"], ' +
                '[role="textbox"], [role="searchbox"], [role="option"], [role="slider"], ' +
                '[contenteditable="true"], [tabindex], [onclick], summary, details';
            const elements = document.querySelectorAll(interactiveSelector);
            const results = [];
            let index = 0;
            
            for (const el of elements) {
                const rect = el.getBoundingClientRect();
                const style = window.getComputedStyle(el);
                
                const isVisible = rect.width > 0 && rect.height > 0 &&
                    style.display !== 'none' && style.visibility !== 'hidden' &&
                    parseFloat(style.opacity) > 0;
                
                if (!isVisible) continue;
                
                // Enhanced clickability detection
                const isClickable = (() => {
                    const centerX = rect.left + rect.width / 2;
                    const centerY = rect.top + rect.height / 2;
                    const topElement = document.elementFromPoint(centerX, centerY);
                    return topElement === el || el.contains(topElement) || (topElement && topElement.contains(el));
                })();
                
                // Paint order (approximation via z-index and position)
                const zIndex = parseInt(style.zIndex) || 0;
                const position = style.position;
                const paintOrder = position !== 'static' ? zIndex + 1000 : zIndex;
                
                // Stacking context detection
                const isInStackingContext = style.position !== 'static' || 
                    parseFloat(style.opacity) < 1 || 
                    style.transform !== 'none' ||
                    style.zIndex !== 'auto';
                
                const attrs = {};
                for (const attr of el.attributes) { attrs[attr.name] = attr.value; }
                
                let textContent = '';
                if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                    textContent = el.value || '';
                } else {
                    textContent = el.innerText || el.textContent || '';
                }
                textContent = textContent.trim().substring(0, 200);
                
                const tagName = el.tagName;
                const isInteractive = ['A', 'BUTTON', 'INPUT', 'TEXTAREA', 'SELECT'].includes(tagName) ||
                    el.getAttribute('role') !== null || el.getAttribute('onclick') !== null ||
                    el.getAttribute('contenteditable') === 'true' || el.getAttribute('tabindex') !== null;
                
                results.push({
                    index, tagName, attributes: attrs, textContent,
                    isVisible, isInteractive, isClickable, paintOrder,
                    isInStackingContext,
                    boundingBox: { x: rect.x, y: rect.y, width: rect.width, height: rect.height }
                });
                index++;
            }
            return results;
        }
    """;

    private static final String COMPOUND_DETECT_JS = """
        () => {
            const components = [];
            let idx = 0;
            
            // Select elements
            document.querySelectorAll('select').forEach(el => {
                const options = Array.from(el.options).map(o => o.text + ' (' + o.value + ')');
                components.push({
                    type: 'select', selector: 'select', index: idx++,
                    value: el.value, options: options
                });
            });
            
            // Date/time inputs
            document.querySelectorAll('input[type="date"], input[type="time"], input[type="datetime-local"]').forEach(el => {
                components.push({
                    type: 'datetime', selector: el.type, index: idx++,
                    value: el.value
                });
            });
            
            // Details/summary
            document.querySelectorAll('details').forEach(el => {
                const summary = el.querySelector('summary');
                components.push({
                    type: 'details', selector: 'details', index: idx++,
                    value: summary ? summary.innerText : '',
                    expanded: el.open
                });
            });
            
            // Range inputs
            document.querySelectorAll('input[type="range"]').forEach(el => {
                components.push({
                    type: 'range', selector: 'input[type="range"]', index: idx++,
                    value: el.value
                });
            });
            
            // Color inputs
            document.querySelectorAll('input[type="color"]').forEach(el => {
                components.push({
                    type: 'color', selector: 'input[type="color"]', index: idx++,
                    value: el.value
                });
            });
            
            return components;
        }
    """;

    /**
     * Enhanced element with visibility, clickability, and paint order info.
     */
    public record EnhancedElement(
            int index, String tagName, Map<String, String> attributes, String textContent,
            boolean isVisible, boolean isInteractive, boolean isClickable,
            int paintOrder, boolean isInStackingContext, double[] boundingBox
    ) {
        public String getSignature() {
            return tagName + ":" + attributes.getOrDefault("id", "") + ":" +
                    attributes.getOrDefault("class", "") + ":" + textContent.hashCode();
        }
    }

    /**
     * Compound component (select, date/time, details/summary, etc.)
     */
    public record CompoundComponent(String type, String selector, int index,
                                     String value, List<String> options, boolean expanded) {}

    /**
     * Enhanced DOM state with additional metadata.
     */
    public static class EnhancedDomState {
        private final List<EnhancedElement> elements;
        private Set<Integer> newNodeIndices;

        public EnhancedDomState(List<EnhancedElement> elements, Set<Integer> newNodeIndices) {
            this.elements = elements;
            this.newNodeIndices = newNodeIndices;
        }

        public List<EnhancedElement> getElements() { return elements; }
        public Set<Integer> getNewNodeIndices() { return newNodeIndices; }
        public void setNewNodeIndices(Set<Integer> indices) { this.newNodeIndices = indices; }
        public int getElementCount() { return elements.size(); }

        public boolean isNewNode(int index) { return newNodeIndices.contains(index); }

        public List<EnhancedElement> getClickableElements() {
            return elements.stream().filter(EnhancedElement::isClickable).toList();
        }

        public List<EnhancedElement> getVisibleElements() {
            return elements.stream().filter(EnhancedElement::isVisible).toList();
        }
    }
}
