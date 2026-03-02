package com.browserautomation.dom.tracking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Tracks which DOM nodes are new since the last snapshot.
 * Maintains a fingerprint of previously seen nodes and marks new ones
 * in subsequent snapshots.
 *
 * Equivalent to browser-use's new node marking feature.
 */
public class NewNodeTracker {

    private static final Logger logger = LoggerFactory.getLogger(NewNodeTracker.class);

    private Set<String> previousFingerprints;
    private Set<String> currentFingerprints;
    private final List<TrackedNode> newNodes;
    private final List<TrackedNode> removedNodes;
    private int snapshotCount;

    public NewNodeTracker() {
        this.previousFingerprints = new HashSet<>();
        this.currentFingerprints = new HashSet<>();
        this.newNodes = new ArrayList<>();
        this.removedNodes = new ArrayList<>();
        this.snapshotCount = 0;
    }

    /**
     * Process a new DOM snapshot and identify new/removed nodes.
     *
     * @param elements the current list of DOM elements as maps
     * @return the result containing new and removed nodes
     */
    public TrackingResult processSnapshot(List<Map<String, Object>> elements) {
        snapshotCount++;
        currentFingerprints = new HashSet<>();
        newNodes.clear();
        removedNodes.clear();

        Map<String, Map<String, Object>> currentElements = new LinkedHashMap<>();

        for (Map<String, Object> element : elements) {
            String fingerprint = computeFingerprint(element);
            currentFingerprints.add(fingerprint);
            currentElements.put(fingerprint, element);

            if (!previousFingerprints.contains(fingerprint)) {
                TrackedNode node = createTrackedNode(element, fingerprint);
                newNodes.add(node);
            }
        }

        // Find removed nodes
        for (String prevFingerprint : previousFingerprints) {
            if (!currentFingerprints.contains(prevFingerprint)) {
                removedNodes.add(new TrackedNode(prevFingerprint, null, null, null, -1));
            }
        }

        logger.debug("Snapshot #{}: {} total, {} new, {} removed",
                snapshotCount, elements.size(), newNodes.size(), removedNodes.size());

        // Update previous for next comparison
        previousFingerprints = new HashSet<>(currentFingerprints);

        return new TrackingResult(
                Collections.unmodifiableList(new ArrayList<>(newNodes)),
                Collections.unmodifiableList(new ArrayList<>(removedNodes)),
                elements.size(),
                snapshotCount
        );
    }

    /**
     * Check if a specific element is new (not seen in previous snapshot).
     *
     * @param element the element to check
     * @return true if the element is new
     */
    public boolean isNew(Map<String, Object> element) {
        String fingerprint = computeFingerprint(element);
        return !previousFingerprints.contains(fingerprint);
    }

    /**
     * Get the list of new nodes from the last snapshot.
     */
    public List<TrackedNode> getNewNodes() {
        return Collections.unmodifiableList(newNodes);
    }

    /**
     * Get the list of removed nodes from the last snapshot.
     */
    public List<TrackedNode> getRemovedNodes() {
        return Collections.unmodifiableList(removedNodes);
    }

    /**
     * Get the total number of snapshots processed.
     */
    public int getSnapshotCount() {
        return snapshotCount;
    }

    /**
     * Reset the tracker state.
     */
    public void reset() {
        previousFingerprints.clear();
        currentFingerprints.clear();
        newNodes.clear();
        removedNodes.clear();
        snapshotCount = 0;
    }

    /**
     * Compute a fingerprint for a DOM element based on its stable properties.
     */
    String computeFingerprint(Map<String, Object> element) {
        StringBuilder fp = new StringBuilder();

        String tagName = getStringValue(element, "tagName", "");
        fp.append(tagName).append("|");

        // Use attributes for fingerprinting
        Object attrsObj = element.get("attributes");
        if (attrsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = (Map<String, Object>) attrsObj;
            // Use stable attributes: id, class, name, role, href, src
            appendIfPresent(fp, attrs, "id");
            appendIfPresent(fp, attrs, "class");
            appendIfPresent(fp, attrs, "name");
            appendIfPresent(fp, attrs, "role");
            appendIfPresent(fp, attrs, "href");
            appendIfPresent(fp, attrs, "src");
            appendIfPresent(fp, attrs, "data-testid");
            appendIfPresent(fp, attrs, "aria-label");
        }

        // Include text content (truncated for stability)
        String text = getStringValue(element, "textContent", "");
        if (text.length() > 50) {
            text = text.substring(0, 50);
        }
        fp.append("text:").append(text.trim());

        return fp.toString();
    }

    private TrackedNode createTrackedNode(Map<String, Object> element, String fingerprint) {
        String tagName = getStringValue(element, "tagName", "");
        String text = getStringValue(element, "textContent", "");

        Object attrsObj = element.get("attributes");
        Map<String, String> attributes = new LinkedHashMap<>();
        if (attrsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = (Map<String, Object>) attrsObj;
            attrs.forEach((k, v) -> attributes.put(k, String.valueOf(v)));
        }

        int index = -1;
        Object indexObj = element.get("index");
        if (indexObj instanceof Number) {
            index = ((Number) indexObj).intValue();
        }

        return new TrackedNode(fingerprint, tagName, attributes, text, index);
    }

    private void appendIfPresent(StringBuilder sb, Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        if (val != null) {
            sb.append(key).append("=").append(val).append("|");
        }
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    /**
     * Represents a tracked DOM node.
     */
    public record TrackedNode(
            String fingerprint,
            String tagName,
            Map<String, String> attributes,
            String textContent,
            int index
    ) {}

    /**
     * Result of a tracking comparison between snapshots.
     */
    public record TrackingResult(
            List<TrackedNode> newNodes,
            List<TrackedNode> removedNodes,
            int totalElements,
            int snapshotNumber
    ) {
        public boolean hasChanges() {
            return !newNodes.isEmpty() || !removedNodes.isEmpty();
        }
    }
}
