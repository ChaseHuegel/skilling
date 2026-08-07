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

class SkillManagerReloadTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = TestSkillManager.newBuiltIn();
    }

    @Test
    void malformedSkillAmongManyIsSkippedWithWarning() throws Exception {
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

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
            skillManager.loadSkills(skillsDir.toFile());
        }
        assertTrue(skillManager.getSkills().containsKey("good"),
                "the valid skill must survive a malformed sibling");
        assertFalse(skillManager.getSkills().containsKey("bad"),
                "the malformed skill must be skipped, not fail the load");
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
