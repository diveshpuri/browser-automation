package com.browserautomation.skill;

import com.browserautomation.action.ActionParameters;
import com.browserautomation.action.ActionRegistry;
import com.browserautomation.action.ActionResult;
import com.browserautomation.action.BrowserAction;
import com.browserautomation.browser.BrowserSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Registry for managing automation skills.
 *
 * <p>Supports registering skills programmatically or loading from JSON files.
 * Skills can be executed directly or referenced in LLM conversations.</p>
 */
public class SkillRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, Skill> skills = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Register a skill.
     */
    public void register(Skill skill) {
        skills.put(skill.getName(), skill);
        logger.debug("Registered skill: {}", skill.getName());
    }

    /**
     * Unregister a skill by name.
     */
    public boolean unregister(String name) {
        Skill removed = skills.remove(name);
        if (removed != null) {
            logger.debug("Unregistered skill: {}", name);
            return true;
        }
        return false;
    }

    /**
     * Get a skill by name.
     */
    public Optional<Skill> getSkill(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    /**
     * Get all registered skills.
     */
    public Collection<Skill> getAllSkills() {
        return Collections.unmodifiableCollection(skills.values());
    }

    /**
     * Get skills by category.
     */
    public List<Skill> getSkillsByCategory(String category) {
        return skills.values().stream()
                .filter(s -> s.getCategory().equals(category))
                .toList();
    }

    /**
     * Get all skill categories.
     */
    public Set<String> getCategories() {
        Set<String> categories = new LinkedHashSet<>();
        skills.values().forEach(s -> categories.add(s.getCategory()));
        return categories;
    }

    /**
     * Execute a skill using the provided browser session and action registry.
     *
     * @param skillName      the name of the skill to execute
     * @param actionRegistry the action registry for resolving actions
     * @param session        the browser session to execute against
     * @param variables      variable substitutions for parameterized steps
     * @return list of action results from each step
     */
    public List<ActionResult> executeSkill(String skillName, ActionRegistry actionRegistry,
                                           BrowserSession session, Map<String, String> variables) {
        Skill skill = skills.get(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("Skill not found: " + skillName);
        }

        logger.info("Executing skill '{}' ({} steps)", skillName, skill.getSteps().size());
        List<ActionResult> results = new ArrayList<>();

        for (int i = 0; i < skill.getSteps().size(); i++) {
            SkillStep step = skill.getSteps().get(i);
            logger.debug("Skill '{}' step {}/{}: {}", skillName, i + 1, skill.getSteps().size(), step);

            BrowserAction action = actionRegistry.getAction(step.getActionName());
            if (action == null) {
                ActionResult error = ActionResult.error("Unknown action in skill: " + step.getActionName());
                results.add(error);
                logger.warn("Skill '{}' step {} failed: unknown action '{}'",
                        skillName, i + 1, step.getActionName());
                break;
            }

            // Substitute variables in parameters
            Map<String, Object> resolvedParams = resolveVariables(step.getParameters(), variables);
            ActionResult result = action.execute(session, new ActionParameters(resolvedParams));
            results.add(result);

            if (!result.isSuccess()) {
                logger.warn("Skill '{}' step {} failed: {}", skillName, i + 1, result.getError());
                break;
            }
        }

        logger.info("Skill '{}' completed: {}/{} steps succeeded",
                skillName, results.stream().filter(ActionResult::isSuccess).count(), skill.getSteps().size());
        return results;
    }

    /**
     * Load a skill from a JSON file.
     *
     * <p>Expected JSON format:</p>
     * <pre>
     * {
     *   "name": "login",
     *   "description": "Log into a website",
     *   "category": "authentication",
     *   "parameters": {"url": "The login page URL", "username": "The username"},
     *   "steps": [
     *     {"action": "navigate", "params": {"url": "${url}"}},
     *     {"action": "input_text", "params": {"index": 0, "text": "${username}"}}
     *   ]
     * }
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public Skill loadFromJson(Path path) throws IOException {
        String json = Files.readString(path);
        return parseSkillJson(json);
    }

    /**
     * Load a skill from a JSON input stream.
     */
    public Skill loadFromJson(InputStream inputStream) throws IOException {
        String json = new String(inputStream.readAllBytes());
        return parseSkillJson(json);
    }

    @SuppressWarnings("unchecked")
    private Skill parseSkillJson(String json) throws IOException {
        Map<String, Object> data = objectMapper.readValue(json, Map.class);

        String name = (String) data.get("name");
        String description = (String) data.get("description");
        String category = (String) data.getOrDefault("category", "general");
        Map<String, String> parameters = (Map<String, String>) data.getOrDefault("parameters", Map.of());

        List<SkillStep> steps = new ArrayList<>();
        List<Map<String, Object>> stepsData = (List<Map<String, Object>>) data.getOrDefault("steps", List.of());
        for (Map<String, Object> stepData : stepsData) {
            String actionName = (String) stepData.get("action");
            Map<String, Object> params = (Map<String, Object>) stepData.getOrDefault("params", Map.of());
            String stepDesc = (String) stepData.get("description");
            steps.add(new SkillStep(actionName, params, stepDesc));
        }

        Skill skill = new Skill(name, description, steps, parameters, category);
        register(skill);
        logger.info("Loaded skill '{}' from JSON ({} steps)", name, steps.size());
        return skill;
    }

    /**
     * Load all skills from a directory.
     */
    public List<Skill> loadFromDirectory(Path directory) throws IOException {
        List<Skill> loaded = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            try (var stream = Files.list(directory)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                loaded.add(loadFromJson(p));
                            } catch (IOException e) {
                                logger.warn("Failed to load skill from {}: {}", p, e.getMessage());
                            }
                        });
            }
        }
        logger.info("Loaded {} skills from directory {}", loaded.size(), directory);
        return loaded;
    }

    /**
     * Get the skills description for the LLM system prompt.
     */
    public String getSkillsDescription() {
        if (skills.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("## Available Skills\n\n");
        for (Skill skill : skills.values()) {
            sb.append(skill.toLlmDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Get the number of registered skills.
     */
    public int size() {
        return skills.size();
    }

    private Map<String, Object> resolveVariables(Map<String, Object> params, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) return params;

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String strValue) {
                for (Map.Entry<String, String> var : variables.entrySet()) {
                    strValue = strValue.replace("${" + var.getKey() + "}", var.getValue());
                }
                resolved.put(entry.getKey(), strValue);
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }
}
