package com.browserautomation.agent.sensitive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles sensitive data in LLM messages by replacing credentials with
 * domain-scoped {@code <secret>} tags. This prevents credentials from being
 * sent to the LLM in plain text.
 *
 *
 * Usage:
 * <pre>
 * SensitiveDataHandler handler = new SensitiveDataHandler();
 * handler.registerSecret("login", "password", "myP@ss123");
 * String safe = handler.maskSecrets("Type myP@ss123 into the password field");
 * // Result: "Type <secret>login.password</secret> into the password field"
 * </pre>
 */
public class SensitiveDataHandler {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataHandler.class);

    // Map of domain -> (key -> value)
    private final Map<String, Map<String, String>> secrets;
    // Reverse map: value -> domain.key for efficient lookup
    private final Map<String, String> reverseMap;
    // Pattern cache
    private Pattern combinedPattern;
    private boolean patternDirty;

    public SensitiveDataHandler() {
        this.secrets = new LinkedHashMap<>();
        this.reverseMap = new LinkedHashMap<>();
        this.patternDirty = true;
    }

    /**
     * Register a secret value that should be masked in messages.
     *
     * @param domain the domain/context (e.g., "login", "api", "database")
     * @param key    the key within the domain (e.g., "password", "token")
     * @param value  the actual secret value to mask
     */
    public void registerSecret(String domain, String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        secrets.computeIfAbsent(domain, k -> new LinkedHashMap<>()).put(key, value);
        reverseMap.put(value, domain + "." + key);
        patternDirty = true;
        logger.debug("Registered secret: {}.{}", domain, key);
    }

    /**
     * Remove a registered secret.
     *
     * @param domain the domain
     * @param key    the key
     */
    public void removeSecret(String domain, String key) {
        Map<String, String> domainSecrets = secrets.get(domain);
        if (domainSecrets != null) {
            String value = domainSecrets.remove(key);
            if (value != null) {
                reverseMap.remove(value);
                patternDirty = true;
            }
            if (domainSecrets.isEmpty()) {
                secrets.remove(domain);
            }
        }
    }

    /**
     * Mask all registered secrets in the given text.
     * Replaces occurrences of secret values with {@code <secret>domain.key</secret>} tags.
     *
     * @param text the text that may contain secrets
     * @return the text with secrets replaced by tags
     */
    public String maskSecrets(String text) {
        if (text == null || text.isEmpty() || reverseMap.isEmpty()) {
            return text;
        }

        rebuildPatternIfNeeded();
        if (combinedPattern == null) {
            return text;
        }

        Matcher matcher = combinedPattern.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String matched = matcher.group();
            String tag = reverseMap.get(matched);
            if (tag != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement("<secret>" + tag + "</secret>"));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Unmask secret tags back to their actual values.
     * Replaces {@code <secret>domain.key</secret>} tags with the actual secret values.
     *
     * @param text the text with secret tags
     * @return the text with actual secret values restored
     */
    public String unmaskSecrets(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Pattern tagPattern = Pattern.compile("<secret>([^<]+)</secret>");
        Matcher matcher = tagPattern.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String tag = matcher.group(1);
            String[] parts = tag.split("\\.", 2);
            if (parts.length == 2) {
                Map<String, String> domainSecrets = secrets.get(parts[0]);
                if (domainSecrets != null) {
                    String value = domainSecrets.get(parts[1]);
                    if (value != null) {
                        matcher.appendReplacement(result, Matcher.quoteReplacement(value));
                        continue;
                    }
                }
            }
            // Leave tag as-is if not found
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Check if the text contains any registered secrets.
     *
     * @param text the text to check
     * @return true if secrets are found in the text
     */
    public boolean containsSecrets(String text) {
        if (text == null || text.isEmpty() || reverseMap.isEmpty()) {
            return false;
        }
        for (String secret : reverseMap.keySet()) {
            if (text.contains(secret)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all registered domains.
     */
    public Set<String> getDomains() {
        return Collections.unmodifiableSet(secrets.keySet());
    }

    /**
     * Get all keys for a domain.
     */
    public Set<String> getKeys(String domain) {
        Map<String, String> domainSecrets = secrets.get(domain);
        if (domainSecrets == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(domainSecrets.keySet());
    }

    /**
     * Get the number of registered secrets.
     */
    public int getSecretCount() {
        return reverseMap.size();
    }

    private void rebuildPatternIfNeeded() {
        if (!patternDirty) {
            return;
        }
        if (reverseMap.isEmpty()) {
            combinedPattern = null;
        } else {
            // Sort by length descending to match longest secrets first
            List<String> sortedSecrets = new ArrayList<>(reverseMap.keySet());
            sortedSecrets.sort((a, b) -> Integer.compare(b.length(), a.length()));

            StringBuilder patternBuilder = new StringBuilder();
            for (int i = 0; i < sortedSecrets.size(); i++) {
                if (i > 0) {
                    patternBuilder.append("|");
                }
                patternBuilder.append(Pattern.quote(sortedSecrets.get(i)));
            }
            combinedPattern = Pattern.compile(patternBuilder.toString());
        }
        patternDirty = false;
    }
}
