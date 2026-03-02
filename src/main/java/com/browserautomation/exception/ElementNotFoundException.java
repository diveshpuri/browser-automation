package com.browserautomation.exception;

/**
 * Exception thrown when a DOM element cannot be found by index.
 */
public class ElementNotFoundException extends BrowserException {

    private final int elementIndex;

    public ElementNotFoundException(int elementIndex) {
        super("Element with index " + elementIndex + " not found");
        this.elementIndex = elementIndex;
    }

    public ElementNotFoundException(int elementIndex, String context) {
        super(context + " element with index " + elementIndex + " not found");
        this.elementIndex = elementIndex;
    }

    public int getElementIndex() {
        return elementIndex;
    }
}
