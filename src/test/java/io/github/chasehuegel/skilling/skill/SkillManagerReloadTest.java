package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillManagerReloadTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = TestSkillManager.newBuiltIn();
    }

    @Test
    void malformedSkillAmongManyFailsLoadAndPreservesPreviousSet() throws Exception {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("good.yml"), """
            id: good
            max_level: 100
            progression: { curve: constant, base_xp: 100 }
            """);
        skillManager.loadSkills(skillsDir.toFile());
        assertTrue(skillManager.getSkills().containsKey("good"));

        // Missing reward -> malformed XP source.
        Files.writeString(skillsDir.resolve("bad.yml"), """
            id: bad
            max_level: 100
            progression: { curve: constant, base_xp: 100 }
            xp_sources:
              - { trigger: block_break }
            """);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> skillManager.loadSkills(skillsDir.toFile()));
        assertTrue(ex.getMessage().contains("bad.yml"),
                "the failing file must be named: " + ex.getMessage());
        assertTrue(skillManager.getSkills().containsKey("good"),
                "the previous skill set must be preserved after a failed load");
    }

    @Test
    void loadSkillsSwapsOnlyOnSuccess() throws Exception {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("a.yml"), """
            id: a
            max_level: 100
            progression: { curve: constant, base_xp: 100 }
            """);
        skillManager.loadSkills(skillsDir.toFile());
        assertEquals(1, skillManager.getSkills().size());

        Files.writeString(skillsDir.resolve("b.yml"), """
            id: b
            max_level: 100
            progression: { curve: constant, base_xp: 100 }
            """);
        skillManager.loadSkills(skillsDir.toFile());
        assertEquals(2, skillManager.getSkills().size());
    }
}
