package com.browserautomation.integration;

import com.browserautomation.browser.BrowserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gmail integration for browser automation.
 *
 * Provides pre-built automation patterns for common Gmail operations
 * using the browser session.
 */
public class GmailIntegration {

    private static final Logger logger = LoggerFactory.getLogger(GmailIntegration.class);

    private static final String GMAIL_URL = "https://mail.google.com";
    private static final String GMAIL_COMPOSE_URL = "https://mail.google.com/mail/?view=cm";

    private final BrowserSession session;
    private boolean loggedIn;

    public GmailIntegration(BrowserSession session) {
        this.session = session;
        this.loggedIn = false;
    }

    /**
     * Navigate to Gmail.
     */
    public void open() {
        logger.info("Opening Gmail");
        session.navigateTo(GMAIL_URL);
    }

    /**
     * Compose a new email with the given parameters.
     *
     * @param to      recipient email address
     * @param subject email subject
     * @param body    email body text
     */
    public void composeEmail(String to, String subject, String body) {
        logger.info("Composing email to: {}, subject: {}", to, subject);

        // Use Gmail compose URL with parameters
        String composeUrl = String.format(
                "%s&to=%s&su=%s&body=%s",
                GMAIL_COMPOSE_URL,
                urlEncode(to),
                urlEncode(subject),
                urlEncode(body)
        );
        session.navigateTo(composeUrl);
    }

    /**
     * Search for emails matching the query.
     *
     * @param query the search query (Gmail search syntax)
     */
    public void searchEmails(String query) {
        logger.info("Searching emails: {}", query);
        String searchUrl = GMAIL_URL + "/mail/#search/" + urlEncode(query);
        session.navigateTo(searchUrl);
    }

    /**
     * Navigate to the inbox.
     */
    public void goToInbox() {
        session.navigateTo(GMAIL_URL + "/mail/#inbox");
    }

    /**
     * Navigate to sent mail.
     */
    public void goToSent() {
        session.navigateTo(GMAIL_URL + "/mail/#sent");
    }

    /**
     * Navigate to drafts.
     */
    public void goToDrafts() {
        session.navigateTo(GMAIL_URL + "/mail/#drafts");
    }

    /**
     * Navigate to a specific label.
     */
    public void goToLabel(String label) {
        session.navigateTo(GMAIL_URL + "/mail/#label/" + urlEncode(label));
    }

    /**
     * Get the current page content as text (for reading emails).
     */
    public String getPageContent() {
        return session.extractContent();
    }

    /**
     * Check if we appear to be on Gmail.
     */
    public boolean isOnGmail() {
        try {
            String url = session.getCurrentPage().url();
            return url.contains("mail.google.com");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if we appear to be logged in.
     */
    public boolean isLoggedIn() {
        if (loggedIn) return true;
        try {
            String url = session.getCurrentPage().url();
            loggedIn = url.contains("mail.google.com/mail");
            return loggedIn;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get actions available for the Gmail integration.
     */
    public List<Map<String, String>> getAvailableActions() {
        List<Map<String, String>> actions = new ArrayList<>();
        actions.add(Map.of("name", "open_gmail", "description", "Open Gmail in the browser"));
        actions.add(Map.of("name", "compose_email", "description", "Compose a new email"));
        actions.add(Map.of("name", "search_emails", "description", "Search for emails"));
        actions.add(Map.of("name", "go_to_inbox", "description", "Navigate to inbox"));
        actions.add(Map.of("name", "go_to_sent", "description", "Navigate to sent mail"));
        actions.add(Map.of("name", "go_to_drafts", "description", "Navigate to drafts"));
        return actions;
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
