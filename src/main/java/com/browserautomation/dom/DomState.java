package com.browserautomation.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the serialized DOM state of a page, containing all interactive
 * elements indexed for LLM consumption.
 * Equivalent to browser-use's SerializedDOMState.
 */
public class DomState {

    private final List<DomElement> elements;
    private final Map<Integer, DomElement> elementsByIndex;

    public DomState(List<DomElement> elements) {
        this.elements = elements;
        this.elementsByIndex = new HashMap<>();
        for (DomElement element : elements) {
            elementsByIndex.put(element.getIndex(), element);
        }
    }

    /**
     * Get an element by its assigned index.
     */
    public DomElement getElementByIndex(int index) {
        return elementsByIndex.get(index);
    }

    /**
     * Get all interactive elements.
     */
    public List<DomElement> getInteractiveElements() {
        List<DomElement> interactive = new ArrayList<>();
        for (DomElement element : elements) {
            if (element.isInteractive()) {
                interactive.add(element);
            }
        }
        return interactive;
    }

    /**
     * Get the total number of elements.
     */
    public int getElementCount() {
        return elements.size();
    }

    /**
     * Get the LLM-friendly representation of the DOM state.
     */
    public String toLlmRepresentation() {
        StringBuilder sb = new StringBuilder();
        for (DomElement element : elements) {
            if (element.isInteractive() || element.isVisible()) {
                sb.append(element.toLlmRepresentation()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Get all elements.
     */
    public List<DomElement> getElements() {
        return elements;
    }
}
