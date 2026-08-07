package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies recursive skill loading: subfolder skills load, a duplicate id
 * keeps the first definition, and a malformed file is skipped with a warning
 * instead of failing the load.
 */
class SkillManagerRecursiveLoadTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = TestSkillManager.newBuiltIn();
    }

    private void writeSkill(String relative, String yaml) throws Exception {
        Path file = tempDir.resolve("skills").resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, yaml);
    }

    private void writeValidSkill(String relative, String id) throws Exception {
        writeSkill(relative, """
                id: %s
                max_level: 100
                progression: { curve: constant, base_xp: 100 }
                """.formatted(id));
    }

    @Test
    void skillInSubfolderLoads() throws Exception {
        writeValidSkill("mining.yml", "mining");
        writeValidSkill("combat/archery.yml", "archery");

        skillManager.loadSkills(tempDir.resolve("skills").toFile());

        assertTrue(skillManager.getSkills().containsKey("mining"));
        assertTrue(skillManager.getSkills().containsKey("archery"),
                "a skill in a subfolder must load");
    }

    @Test
    void duplicateSkillIdKeepsTheFirstDefinition() throws Exception {
        writeValidSkill("a.yml", "same");
        writeSkill("sub/dup.yml", """
                id: same
                max_level: 200
                progression: { curve: constant, base_xp: 100 }
                """);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
            skillManager.loadSkills(tempDir.resolve("skills").toFile());
        }

        assertEquals(100, skillManager.getSkill("same").maxLevel(),
                "the first loaded skill must win on a duplicate id");
    }

    @Test
    void malformedSkillIsSkippedAndTheRestLoad() throws Exception {
        writeValidSkill("good.yml", "good");
        writeSkill("bad.yml", """
                id: bad
                max_level: 100
                progression: { curve: constant, base_xp: 100 }
                xp_sources:
                  - { trigger: block_break }
                """);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
            skillManager.loadSkills(tempDir.resolve("skills").toFile());
        }

        assertTrue(skillManager.getSkills().containsKey("good"),
                "valid skills must survive a malformed sibling");
        assertFalse(skillManager.getSkills().containsKey("bad"),
                "the malformed skill must be skipped, not fail the load");
    }
}
