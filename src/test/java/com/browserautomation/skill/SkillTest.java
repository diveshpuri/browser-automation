package com.browserautomation.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Skill and SkillStep classes.
 */
class SkillTest {

    @Test
    void testSkillBuilder() {
        Skill skill = new Skill.Builder()
                .name("login")
                .description("Log into a website")
                .category("authentication")
                .parameter("url", "The login page URL")
                .parameter("username", "The username")
                .addStep("navigate", Map.of("url", "${url}"), "Go to login page")
                .addStep("input_text", Map.of("index", 0, "text", "${username}"))
                .build();

        assertEquals("login", skill.getName());
        assertEquals("Log into a website", skill.getDescription());
        assertEquals("authentication", skill.getCategory());
        assertEquals(2, skill.getParameters().size());
        assertEquals(2, skill.getSteps().size());
    }

    @Test
    void testSkillBuilderDefaults() {
        Skill skill = new Skill.Builder()
                .name("test")
                .description("Test skill")
                .build();

        assertEquals("general", skill.getCategory());
        assertTrue(skill.getParameters().isEmpty());
        assertTrue(skill.getSteps().isEmpty());
    }

    @Test
    void testSkillBuilderValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new Skill.Builder().description("desc").build());

        assertThrows(IllegalArgumentException.class, () ->
                new Skill.Builder().name("name").build());
    }

    @Test
    void testSkillToLlmDescription() {
        Skill skill = new Skill.Builder()
                .name("search")
                .description("Search the web")
                .parameter("query", "Search query")
                .addStep("navigate", Map.of("url", "https://google.com"))
                .build();

        String desc = skill.toLlmDescription();
        assertTrue(desc.contains("search"));
        assertTrue(desc.contains("Search the web"));
        assertTrue(desc.contains("query"));
    }

    @Test
    void testSkillStep() {
        SkillStep step = new SkillStep("navigate", Map.of("url", "https://example.com"), "Go to example");
        assertEquals("navigate", step.getActionName());
        assertEquals("https://example.com", step.getParameters().get("url"));
        assertEquals("Go to example", step.getDescription());
        assertTrue(step.toString().contains("navigate"));
    }

    @Test
    void testSkillStepNullParams() {
        SkillStep step = new SkillStep("done", null, null);
        assertEquals("done", step.getActionName());
        assertTrue(step.getParameters().isEmpty());
        assertNull(step.getDescription());
    }

    @Test
    void testSkillImmutability() {
        Skill skill = new Skill.Builder()
                .name("test")
                .description("Test")
                .addStep("navigate", Map.of("url", "http://test.com"))
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                skill.getSteps().add(new SkillStep("done", Map.of(), null)));
    }
}
