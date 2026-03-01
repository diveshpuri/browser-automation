package com.browserautomation.browser;

import com.browserautomation.dom.DomState;

import java.util.List;

/**
 * Captures the current state of the browser, including the current page URL,
 * title, tab information, screenshot, and DOM state.
 * Equivalent to browser-use's BrowserStateSummary.
 */
public class BrowserState {

    private final String url;
    private final String title;
    private final List<TabInfo> tabs;
    private final int activeTabIndex;
    private final byte[] screenshot;
    private final DomState domState;
    private final PageInfo pageInfo;

    public BrowserState(String url, String title, List<TabInfo> tabs, int activeTabIndex,
                        byte[] screenshot, DomState domState, PageInfo pageInfo) {
        this.url = url;
        this.title = title;
        this.tabs = tabs;
        this.activeTabIndex = activeTabIndex;
        this.screenshot = screenshot;
        this.domState = domState;
        this.pageInfo = pageInfo;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public List<TabInfo> getTabs() {
        return tabs;
    }

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public byte[] getScreenshot() {
        return screenshot;
    }

    public DomState getDomState() {
        return domState;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    /**
     * Information about a single browser tab.
     */
    public static class TabInfo {
        private final int index;
        private final String url;
        private final String title;

        public TabInfo(int index, String url, String title) {
            this.index = index;
            this.url = url;
            this.title = title;
        }

        public int getIndex() {
            return index;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return String.format("Tab[%d]: %s - %s", index, title, url);
        }
    }

    /**
     * Information about page scroll position and dimensions.
     */
    public static class PageInfo {
        private final double scrollY;
        private final double pageHeight;
        private final double viewportHeight;
        private final double pixelsAbove;
        private final double pixelsBelow;

        public PageInfo(double scrollY, double pageHeight, double viewportHeight) {
            this.scrollY = scrollY;
            this.pageHeight = pageHeight;
            this.viewportHeight = viewportHeight;
            this.pixelsAbove = scrollY;
            this.pixelsBelow = Math.max(0, pageHeight - scrollY - viewportHeight);
        }

        public double getScrollY() {
            return scrollY;
        }

        public double getPageHeight() {
            return pageHeight;
        }

        public double getViewportHeight() {
            return viewportHeight;
        }

        public double getPixelsAbove() {
            return pixelsAbove;
        }

        public double getPixelsBelow() {
            return pixelsBelow;
        }

        public double getPagesAbove() {
            return viewportHeight > 0 ? pixelsAbove / viewportHeight : 0;
        }

        public double getPagesBelow() {
            return viewportHeight > 0 ? pixelsBelow / viewportHeight : 0;
        }
    }
}
