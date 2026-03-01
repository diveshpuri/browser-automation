package com.browserautomation.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SkillRegistry.
 */
class SkillRegistryTest {

    private SkillRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SkillRegistry();
    }

    @Test
    void testRegisterAndGet() {
        Skill skill = new Skill.Builder()
                .name("test-skill")
                .description("A test skill")
                .build();

        registry.register(skill);

        Optional<Skill> found = registry.getSkill("test-skill");
        assertTrue(found.isPresent());
        assertEquals("test-skill", found.get().getName());
    }

    @Test
    void testGetNonExistent() {
        assertTrue(registry.getSkill("nonexistent").isEmpty());
    }

    @Test
    void testUnregister() {
        Skill skill = new Skill.Builder()
                .name("temp")
                .description("Temporary skill")
                .build();

        registry.register(skill);
        assertTrue(registry.unregister("temp"));
        assertFalse(registry.unregister("temp"));
        assertTrue(registry.getSkill("temp").isEmpty());
    }

    @Test
    void testGetAllSkills() {
        registry.register(new Skill.Builder().name("a").description("A").build());
        registry.register(new Skill.Builder().name("b").description("B").build());

        assertEquals(2, registry.getAllSkills().size());
        assertEquals(2, registry.size());
    }

    @Test
    void testGetByCategory() {
        registry.register(new Skill.Builder().name("a").description("A").category("auth").build());
        registry.register(new Skill.Builder().name("b").description("B").category("nav").build());
        registry.register(new Skill.Builder().name("c").description("C").category("auth").build());

        assertEquals(2, registry.getSkillsByCategory("auth").size());
        assertEquals(1, registry.getSkillsByCategory("nav").size());
        assertEquals(0, registry.getSkillsByCategory("other").size());
    }

    @Test
    void testGetCategories() {
        registry.register(new Skill.Builder().name("a").description("A").category("auth").build());
        registry.register(new Skill.Builder().name("b").description("B").category("nav").build());

        assertEquals(2, registry.getCategories().size());
        assertTrue(registry.getCategories().contains("auth"));
        assertTrue(registry.getCategories().contains("nav"));
    }

    @Test
    void testGetSkillsDescription() {
        registry.register(new Skill.Builder().name("login").description("Log in").build());
        String desc = registry.getSkillsDescription();
        assertTrue(desc.contains("login"));
        assertTrue(desc.contains("Log in"));
    }

    @Test
    void testEmptySkillsDescription() {
        assertEquals("", registry.getSkillsDescription());
    }

    @Test
    void testExecuteSkillNotFound() {
        assertThrows(IllegalArgumentException.class, () ->
                registry.executeSkill("nonexistent", null, null, null));
    }
}
