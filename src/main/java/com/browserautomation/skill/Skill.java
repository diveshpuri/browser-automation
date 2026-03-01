package com.browserautomation.skill;

import java.util.List;
import java.util.Map;

/**
 * Represents a reusable automation skill.
 * Equivalent to browser-use's skills module.
 *
 * <p>A skill is a named, reusable sequence of actions or instructions
 * that can be registered and invoked by the agent to perform common tasks.</p>
 */
public class Skill {

    private final String name;
    private final String description;
    private final List<SkillStep> steps;
    private final Map<String, String> parameters;
    private final String category;

    /**
     * Create a new skill.
     *
     * @param name        unique skill name
     * @param description human-readable description of what this skill does
     * @param steps       the ordered list of steps in this skill
     * @param parameters  parameter definitions (name -> description)
     * @param category    skill category for organization
     */
    public Skill(String name, String description, List<SkillStep> steps,
                 Map<String, String> parameters, String category) {
        this.name = name;
        this.description = description;
        this.steps = List.copyOf(steps);
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        this.category = category != null ? category : "general";
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<SkillStep> getSteps() { return steps; }
    public Map<String, String> getParameters() { return parameters; }
    public String getCategory() { return category; }

    /**
     * Get a description suitable for the LLM system prompt.
     */
    public String toLlmDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(name).append("**: ").append(description).append("\n");
        if (!parameters.isEmpty()) {
            sb.append("  Parameters: ");
            parameters.forEach((k, v) -> sb.append(k).append(" (").append(v).append("), "));
            sb.append("\n");
        }
        sb.append("  Steps: ").append(steps.size()).append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Skill{name='" + name + "', steps=" + steps.size() + ", category='" + category + "'}";
    }

    /**
     * Builder for creating skills.
     */
    public static class Builder {
        private String name;
        private String description;
        private final List<SkillStep> steps = new java.util.ArrayList<>();
        private final Map<String, String> parameters = new java.util.LinkedHashMap<>();
        private String category;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addStep(SkillStep step) {
            this.steps.add(step);
            return this;
        }

        public Builder addStep(String action, Map<String, Object> params) {
            this.steps.add(new SkillStep(action, params, null));
            return this;
        }

        public Builder addStep(String action, Map<String, Object> params, String description) {
            this.steps.add(new SkillStep(action, params, description));
            return this;
        }

        public Builder parameter(String name, String description) {
            this.parameters.put(name, description);
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Skill build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Skill name is required");
            }
            if (description == null || description.isEmpty()) {
                throw new IllegalArgumentException("Skill description is required");
            }
            return new Skill(name, description, steps, parameters, category);
        }
    }
}
