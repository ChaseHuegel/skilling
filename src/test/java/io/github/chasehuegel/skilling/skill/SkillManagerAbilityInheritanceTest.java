package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.AbilityManager;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the polymorphic ability base-merge: an id-referenced ability
 * inherits the registered definition, the skill's own fields overwrite it at
 * the top level (with nested structures replaced wholesale), and each skill
 * parses its own independent {@code Ability} instance.
 */
class SkillManagerAbilityInheritanceTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;
    private Path abilitiesDir;

    private static final String BASE = """
            id: vein_miner
            display_name: "Vein Miner"
            unlock_level: 25
            trigger: "block_break"
            display:
              lore: [ "&7Break up to &a{chain_limit}&7 blocks." ]
            mechanics:
              - type: "core:chain_break"
                filters: [ { target: "#c:veinminer" } ]
                parameters:
                  chain_limit: { milestones: { 25: 3, 50: 8 } }
            feedback: { notify: { action_bar: false } }
            """;

    @BeforeEach
    void setUp() throws Exception {
        abilitiesDir = tempDir.resolve("abilities");
        Files.createDirectories(abilitiesDir);
        Files.writeString(abilitiesDir.resolve("vein_miner.yml"), BASE);

        var abilityManager = new AbilityManager();
        abilityManager.loadAbilities(abilitiesDir.toFile());
        skillManager = TestSkillManager.newBuiltIn();
        skillManager.setAbilityManager(abilityManager);
    }

    private void writeSkill(String fileName, String abilitiesYaml) throws Exception {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve(fileName), """
                id: %s
                max_level: 100
                progression: { curve: constant, base_xp: 100 }
                xp_sources: []
                abilities:
                %s""".formatted(fileName.replace(".yml", ""), abilitiesYaml));
    }

    private SkillDefinition.Ability abilityOf(String skillId, String abilityId) {
        return skillManager.getSkill(skillId).abilities().stream()
                .filter(a -> a.id().equals(abilityId))
                .findFirst().orElseThrow();
    }

    @Test
    void pureIdReferenceInheritsFullBase() throws Exception {
        writeSkill("s.yml", "  - id: vein_miner\n");

        skillManager.loadSkills(tempDir.resolve("skills").toFile());

        var ability = abilityOf("s", "vein_miner");
        assertEquals("Vein Miner", ability.displayName(), "inherited display_name must survive");
        assertEquals(25, ability.unlockLevel(), "inherited unlock_level must survive");
        assertEquals("block_break", ability.trigger(), "inherited trigger must survive");
        assertEquals(1, ability.mechanics().size(), "inherited mechanics must survive");
        assertEquals(1, ability.display().lore().size(), "inherited lore must survive");
    }

    @Test
    void explicitFieldOverridesTheBase() throws Exception {
        writeSkill("s.yml", "  - id: vein_miner\n    unlock_level: 40\n");

        skillManager.loadSkills(tempDir.resolve("skills").toFile());

        var ability = abilityOf("s", "vein_miner");
        assertEquals(40, ability.unlockLevel(), "the skill's unlock_level must override the base");
        assertEquals("block_break", ability.trigger(), "unrelated inherited fields stay intact");
    }

    @Test
    void suppliedMechanicsReplaceTheBaseWholesale() throws Exception {
        writeSkill("s.yml", """
                  - id: vein_miner
                    unlock_level: 30
                    display:
                      lore: [ "&7Bonus ore XP &a{multiplier}x." ]
                    mechanics:
                      - type: "core:xp_bonus"
                        filters: [ { target: "#c:ores" } ]
                        parameters:
                          multiplier: { linear: { base: 1.05, step: 0.01, max: 1.6 } }
                """);

        skillManager.loadSkills(tempDir.resolve("skills").toFile());

        var ability = abilityOf("s", "vein_miner");
        assertEquals(1, ability.mechanics().size(),
                "the skill's mechanics replace the base list, no deep merge");
        assertEquals("core:xp_bonus", ability.mechanics().get(0).type(),
                "the inherited chain_break must not survive the override");
    }

    @Test
    void unknownIdWithIncompleteInlineDefinitionFailsAtParseTime() throws Exception {
        writeSkill("s.yml", "  - id: not_registered\n");

        // The fail-fast happens in parseSkill (before loadSkills warn-and-skips);
        // an unknown id with no inline content cannot build an ability.
        assertThrows(IllegalArgumentException.class,
                () -> skillManager.parseSkill(tempDir.resolve("skills/s.yml").toFile()));
    }

    @Test
    void twoSkillsInheritingTheSameAbilityGetIndependentInstances() throws Exception {
        writeSkill("a.yml", "  - id: vein_miner\n    unlock_level: 10\n");
        writeSkill("b.yml", "  - id: vein_miner\n    unlock_level: 60\n");

        skillManager.loadSkills(tempDir.resolve("skills").toFile());

        var a = abilityOf("a", "vein_miner");
        var b = abilityOf("b", "vein_miner");
        assertEquals(10, a.unlockLevel());
        assertEquals(60, b.unlockLevel());
        assertNotSame(a, b,
                "each skill must hold its own Ability instance with its own unlock_level");
    }
}
