package com.browserautomation.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages multiple browser sessions for concurrent automation tasks.
 *
 * <p>Supports creating, retrieving, and cleaning up named browser sessions.
 * Thread-safe for use in multi-threaded environments.</p>
 */
public class SessionManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private final Map<String, BrowserSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger sessionCounter = new AtomicInteger(0);
    private final BrowserProfile defaultProfile;
    private final int maxSessions;

    /**
     * Create a SessionManager with default settings.
     */
    public SessionManager() {
        this(new BrowserProfile(), 10);
    }

    /**
     * Create a SessionManager with a custom default profile and max sessions.
     *
     * @param defaultProfile the default browser profile for new sessions
     * @param maxSessions    the maximum number of concurrent sessions
     */
    public SessionManager(BrowserProfile defaultProfile, int maxSessions) {
        this.defaultProfile = defaultProfile;
        this.maxSessions = maxSessions;
    }

    /**
     * Create a new session with an auto-generated name.
     *
     * @return the new browser session
     */
    public BrowserSession createSession() {
        return createSession("session-" + sessionCounter.incrementAndGet());
    }

    /**
     * Create a new session with a specific name and the default profile.
     *
     * @param name the session name
     * @return the new browser session
     */
    public BrowserSession createSession(String name) {
        return createSession(name, defaultProfile);
    }

    /**
     * Create a new session with a specific name and profile.
     *
     * @param name    the session name
     * @param profile the browser profile for this session
     * @return the new browser session
     */
    public BrowserSession createSession(String name, BrowserProfile profile) {
        if (sessions.size() >= maxSessions) {
            throw new IllegalStateException(
                    "Maximum number of sessions (" + maxSessions + ") reached. Close existing sessions first.");
        }
        if (sessions.containsKey(name)) {
            throw new IllegalArgumentException("Session with name '" + name + "' already exists");
        }

        BrowserSession session = new BrowserSession(profile);
        session.start();
        sessions.put(name, session);
        logger.info("Created session '{}' ({}/{})", name, sessions.size(), maxSessions);
        return session;
    }

    /**
     * Get an existing session by name.
     *
     * @param name the session name
     * @return the session, or empty if not found
     */
    public Optional<BrowserSession> getSession(String name) {
        return Optional.ofNullable(sessions.get(name));
    }

    /**
     * Get an existing session, or create a new one if it doesn't exist.
     *
     * @param name the session name
     * @return the existing or newly created session
     */
    public BrowserSession getOrCreateSession(String name) {
        return sessions.computeIfAbsent(name, n -> {
            if (sessions.size() >= maxSessions) {
                throw new IllegalStateException("Maximum sessions reached");
            }
            BrowserSession session = new BrowserSession(defaultProfile);
            session.start();
            logger.info("Created session '{}' on demand ({}/{})", n, sessions.size() + 1, maxSessions);
            return session;
        });
    }

    /**
     * Close and remove a specific session.
     *
     * @param name the session name
     * @return true if the session existed and was closed
     */
    public boolean closeSession(String name) {
        BrowserSession session = sessions.remove(name);
        if (session != null) {
            try {
                session.close();
                logger.info("Closed session '{}' ({}/{})", name, sessions.size(), maxSessions);
            } catch (Exception e) {
                logger.warn("Error closing session '{}': {}", name, e.getMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * Get the names of all active sessions.
     */
    public Set<String> getSessionNames() {
        return Set.copyOf(sessions.keySet());
    }

    /**
     * Get the number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Get the maximum number of sessions allowed.
     */
    public int getMaxSessions() {
        return maxSessions;
    }

    /**
     * Check if a session exists.
     */
    public boolean hasSession(String name) {
        return sessions.containsKey(name);
    }

    /**
     * Close all sessions and clean up resources.
     */
    @Override
    public void close() {
        logger.info("Closing all {} sessions", sessions.size());
        for (Map.Entry<String, BrowserSession> entry : sessions.entrySet()) {
            try {
                entry.getValue().close();
                logger.debug("Closed session '{}'", entry.getKey());
            } catch (Exception e) {
                logger.warn("Error closing session '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        sessions.clear();
    }
}
