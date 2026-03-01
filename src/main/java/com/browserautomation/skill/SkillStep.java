package com.browserautomation.skill;

import java.util.Map;

/**
 * A single step within a skill.
 * Represents an action to execute with its parameters.
 */
public class SkillStep {

    private final String actionName;
    private final Map<String, Object> parameters;
    private final String description;

    /**
     * Create a skill step.
     *
     * @param actionName  the name of the action to execute
     * @param parameters  the parameters for this action
     * @param description optional human-readable description
     */
    public SkillStep(String actionName, Map<String, Object> parameters, String description) {
        this.actionName = actionName;
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        this.description = description;
    }

    public String getActionName() { return actionName; }
    public Map<String, Object> getParameters() { return parameters; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return actionName + "(" + parameters + ")"
                + (description != null ? " // " + description : "");
    }
}
