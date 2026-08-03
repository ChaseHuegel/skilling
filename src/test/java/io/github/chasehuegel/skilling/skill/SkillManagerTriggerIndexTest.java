package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the trigger-key index built by {@link SkillManager}: dispatch looks
 * up only the sources/abilities bound to a trigger (no full scan), and reloads
 * rebuild the index without stale entries.
 */
class SkillManagerTriggerIndexTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = TestSkillManager.newBuiltIn();
    }

    private void writeSkill(String fileName, String trigger, String abilityTrigger) throws Exception {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve(fileName), """
                id: %s
                max_level: 100
                progression: { curve: constant, base_xp: 100 }
                xp_sources:
                  - { trigger: %s, reward: { constant: 1.0 } }
                abilities:
                  - id: abil
                    display_name: "Abil"
                    unlock_level: 1
                    trigger: %s
                    mechanics:
                      - { type: "core:block_particles", parameters: { particle: { constant: "HAPPY_VILLAGER" } } }
                    feedback: { notify: { action_bar: false } }
                """.formatted(fileName.replace(".yml", ""), trigger, abilityTrigger));
    }

    @Test
    void indexMatchesManualScanOfAllSkills() throws Exception {
        writeSkill("a.yml", "block_break", "block_break");
        writeSkill("b.yml", "entity_damage", "block_break");
        skillManager.loadSkills(tempDir.resolve("skills").toFile());

        List<SkillManager.XpSourceRef> expectedXp = new ArrayList<>();
        List<SkillManager.AbilityRef> expectedAbilities = new ArrayList<>();
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            for (SkillDefinition.XpSource source : skill.xpSources()) {
                if (source.trigger().equals("block_break")) expectedXp.add(new SkillManager.XpSourceRef(skill, source));
            }
            for (SkillDefinition.Ability ability : skill.abilities()) {
                if (ability.trigger().equals("block_break")) expectedAbilities.add(new SkillManager.AbilityRef(skill, ability));
            }
        }

        assertEquals(expectedXp, skillManager.xpSourcesFor("block_break"),
                "the index must yield exactly the scan's block_break sources");
        assertEquals(expectedAbilities, skillManager.abilitiesFor("block_break"),
                "the index must yield exactly the scan's block_break abilities");
        assertEquals(1, skillManager.xpSourcesFor("entity_damage").size());
        assertTrue(skillManager.abilitiesFor("entity_damage").isEmpty(),
                "no ability is bound to entity_damage in this fixture");
    }

    @Test
    void reloadRebuildsTheIndexWithoutStaleEntries() throws Exception {
        writeSkill("a.yml", "block_break", "block_break");
        skillManager.loadSkills(tempDir.resolve("skills").toFile());
        assertEquals(1, skillManager.xpSourcesFor("block_break").size());
        assertEquals(1, skillManager.abilitiesFor("block_break").size());

        // Replace the skill with one bound to a different trigger: the old
        // trigger's index entries must vanish.
        Files.writeString(tempDir.resolve("skills/a.yml"), """
                id: a
                max_level: 100
                progression: { curve: constant, base_xp: 100 }
                xp_sources:
                  - { trigger: entity_damage, reward: { constant: 1.0 } }
                abilities: []
                """);
        skillManager.loadSkills(tempDir.resolve("skills").toFile());

        assertTrue(skillManager.xpSourcesFor("block_break").isEmpty(),
                "the reloaded index must not keep stale block_break sources");
        assertEquals(1, skillManager.xpSourcesFor("entity_damage").size());
        assertTrue(skillManager.abilitiesFor("block_break").isEmpty());
    }
}
