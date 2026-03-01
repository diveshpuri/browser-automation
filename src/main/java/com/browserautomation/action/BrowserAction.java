package com.browserautomation.action;

import com.browserautomation.browser.BrowserSession;

/**
 * Interface for all browser actions that can be executed by the agent.
 * Each action represents a single browser operation (click, type, navigate, etc.).
 */
public interface BrowserAction {

    /**
     * Get the name of this action (used in LLM tool calling).
     */
    String getName();

    /**
     * Get the description of this action for the LLM.
     */
    String getDescription();

    /**
     * Get the JSON schema of the parameters this action accepts.
     */
    String getParameterSchema();

    /**
     * Execute this action with the given parameters.
     *
     * @param session    the browser session to execute against
     * @param parameters the action parameters as parsed from JSON
     * @return the result of the action execution
     */
    ActionResult execute(BrowserSession session, ActionParameters parameters);
}
