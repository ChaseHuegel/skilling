package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.mechanic.impl.MechanicParamValidators;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that unknown string-valued mechanic parameters (effect, attribute,
 * material, particle) are rejected when the skill YAML loads, not when the
 * ability fires in an event handler.
 */
class SkillManagerMechanicParamValidationTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MechanicParamValidators.configureLookups(
                key -> key.getKey().equals("poison"),
                key -> key.getKey().equals("movement_speed"),
                key -> key.getKey().equals("entity.player.levelup"),
                key -> key.getKey().equals("happy_villager"),
                key -> key.getKey().equals("netherite_pickaxe"),
                ref -> ref.equals("#minecraft:logs") || ref.startsWith("minecraft:"));
    }

    @AfterEach
    void tearDown() {
        MechanicParamValidators.configureLookups(null, null, null, null, null, null);
    }

    private SkillManager newSkillManager() {
        return io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
    }

    private void writeSkill(String mechanicYaml) throws Exception {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: abil
                    display_name: "Abil"
                    unlock_level: 1
                    trigger: "player_interact"
                    mechanics:
                """ + mechanicYaml + """
                    feedback: { notify: { action_bar: false } }
                """);
    }

    @Test
    void unknownEffectFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:aoe_effect"
                        parameters:
                          effect: { constant: "minecraft:poisn" }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("poisn"), ex.getMessage());
    }

    @Test
    void unknownAttributeFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:modify_attribute"
                        parameters:
                          attribute: { constant: "minecraft:movement_speeed" }
                          amount: { constant: 1.0 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("movement_speeed"), ex.getMessage());
    }

    @Test
    void unknownMaterialFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:set_cooldown"
                        parameters:
                          material: { constant: "minecraft:not_a_material" }
                          ticks: { constant: 20 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("not_a_material"), ex.getMessage());
    }

    @Test
    void unknownParticleFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:block_particles"
                        parameters:
                          particle: { constant: "minecraft:not_a_particle" }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("minecraft:not_a_particle"), ex.getMessage());
    }

    @Test
    void missingEffectFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:aoe_effect"
                        parameters:
                          radius: { constant: 5 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("effect"), ex.getMessage());
    }

    @Test
    void missingAttributeFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:modify_attribute"
                        parameters:
                          amount: { constant: 1.0 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("attribute"), ex.getMessage());
    }

    @Test
    void stringValuedNumericFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:apply_status"
                        parameters:
                          effect: { constant: "minecraft:poison" }
                          duration: { constant: "3" }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("duration"), ex.getMessage());
    }

    @Test
    void unsupportedParameterFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:apply_status"
                        parameters:
                          effect: { constant: "minecraft:poison" }
                          typo: { constant: 1.0 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("typo"), ex.getMessage());
    }

    @Test
    void unknownChainBreakTargetFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:chain_break"
                        parameters:
                          chain_limit: { constant: 10 }
                          target: { constant: "#minecraft:typo" }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("#minecraft:typo"), ex.getMessage());
    }

    @Test
    void validChainBreakTargetLoads() throws Exception {
        writeSkill("""
                      - type: "core:chain_break"
                        parameters:
                          chain_limit: { constant: 10 }
                          target: { constant: "#minecraft:logs" }
                """);
        assertDoesNotThrow(() -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
    }

    private void writeAbilityWithFeedback(String abilityBody) throws Exception {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: abil
                    display_name: "Abil"
                    unlock_level: 1
                    trigger: "player_interact"
                    mechanics: []
                """ + abilityBody);
    }

    @Test
    void invalidFeedbackSoundFailsToLoad() throws Exception {
        writeAbilityWithFeedback("""
                    feedback:
                      notify: { action_bar: true }
                      sounds: [ { type: "minecraft:not_a_sound" } ]
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("minecraft:not_a_sound"), ex.getMessage());
    }

    @Test
    void invalidFeedbackParticleFailsToLoad() throws Exception {
        writeAbilityWithFeedback("""
                    feedback:
                      notify: { action_bar: true }
                      particles: [ { type: "minecraft:not_a_particle" } ]
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("minecraft:not_a_particle"), ex.getMessage());
    }

    @Test
    void missingRecipeFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:unlock_recipe"
                        parameters:
                          duration: { constant: 3 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("recipe"), ex.getMessage());
    }

    @Test
    void malformedRecipeKeyFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:unlock_recipe"
                        parameters:
                          recipe: { constant: "NOT_A_RECIPE" }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("NOT_A_RECIPE"), ex.getMessage());
    }

    @Test
    void unknownUnlockRecipeParamFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:unlock_recipe"
                        parameters:
                          recipe: { constant: "minecraft:netherite_pickaxe" }
                          typo: { constant: 1.0 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("typo"), ex.getMessage());
    }

    @Test
    void validUnlockRecipeLoads() throws Exception {
        writeSkill("""
                      - type: "core:unlock_recipe"
                        parameters:
                          recipe: { constant: "minecraft:netherite_pickaxe" }
                """);
        assertDoesNotThrow(() -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
    }

    @Test
    void persistentAttributeMissingUuidFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:persistent_attribute"
                        parameters:
                          attribute: { constant: "minecraft:movement_speed" }
                          amount: { constant: 5.0 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("uuid"), ex.getMessage());
    }

    @Test
    void persistentAttributeMalformedUuidFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:persistent_attribute"
                        parameters:
                          attribute: { constant: "minecraft:movement_speed" }
                          amount: { constant: 5.0 }
                          uuid: { constant: "not-a-uuid" }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("not-a-uuid"), ex.getMessage());
    }

    @Test
    void persistentAttributeMissingAttributeFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:persistent_attribute"
                        parameters:
                          uuid: { constant: "3f2b9c4a-1e5d-4a6b-8c7d-9e0f1a2b3c4d" }
                          amount: { constant: 5.0 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("attribute"), ex.getMessage());
    }

    @Test
    void persistentAttributeNegativeConstantAmountFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:persistent_attribute"
                        parameters:
                          attribute: { constant: "minecraft:movement_speed" }
                          uuid: { constant: "3f2b9c4a-1e5d-4a6b-8c7d-9e0f1a2b3c4d" }
                          amount: { constant: -1.0 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("amount"), ex.getMessage());
    }

    @Test
    void validPersistentAttributeLoads() throws Exception {
        writeSkill("""
                      - type: "core:persistent_attribute"
                        parameters:
                          attribute: { constant: "minecraft:movement_speed" }
                          amount: { linear: { base: 0.0, step: 0.25, max: 25.0 } }
                          uuid: { constant: "3f2b9c4a-1e5d-4a6b-8c7d-9e0f1a2b3c4d" }
                """);
        assertDoesNotThrow(() -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
    }

    @Test
    void invalidOnFailureSoundFailsToLoad() throws Exception {
        writeAbilityWithFeedback("""
                    on_failure:
                      cooldown:
                        sounds: [ { type: "minecraft:not_a_sound" } ]
                    feedback: { notify: { action_bar: false } }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("minecraft:not_a_sound"), ex.getMessage());
    }

    @Test
    void validNamespacedFeedbackLoads() throws Exception {
        writeAbilityWithFeedback("""
                    on_failure:
                      cooldown:
                        sounds: [ { type: "minecraft:entity.player.levelup" } ]
                    feedback:
                      notify: { action_bar: true }
                      sounds: [ { type: "minecraft:entity.player.levelup" } ]
                      particles: [ { type: "minecraft:happy_villager" } ]
                """);
        assertDoesNotThrow(() -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
    }

    @Test
    void negativeRadiusFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:aoe_effect"
                        parameters:
                          effect: { constant: "minecraft:poison" }
                          radius: { constant: -5 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("radius"), ex.getMessage());
    }

    @Test
    void chanceOverOneHundredFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:dodge"
                        parameters:
                          chance: { constant: 150 }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
        assertTrue(ex.getMessage().contains("chance"), ex.getMessage());
    }

    @Test
    void negativeCooldownTicksFailToLoad() throws Exception {
        writeSkill("""
                      - type: "core:set_cooldown"
                        parameters:
                          material: { constant: "minecraft:shield" }
                          ticks: { constant: -10 }
                """);
        assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().parseSkill(tempDir.resolve("skills/test.yml").toFile()));
    }

    @Test
    void validParamsLoadAndExecuteUnchanged() throws Exception {
        writeSkill("""
                      - type: "core:ally_aura"
                        parameters:
                          effect: { constant: "minecraft:poison" }
                          radius: { constant: 5 }
                          duration: { constant: 3 }
                      - type: "core:block_particles"
                        parameters:
                          particle: { constant: "minecraft:happy_villager" }
                """);
        SkillManager manager = newSkillManager();
        assertDoesNotThrow(() -> manager.loadSkills(tempDir.resolve("skills").toFile()));
        assertTrue(manager.getSkills().containsKey("test"));
    }

    @Test
    void customAddonMechanicCanOptIntoValidation() throws Exception {
        var registry = new io.github.chasehuegel.skilling.engine.registry.MechanicRegistry();
        registry.register("test:custom", TestMechanic.class,
                java.util.List.of("effect"),
                (ctx, params) -> {
                    if (!"minecraft:poison".equals(params.get("effect"))) {
                        throw new IllegalArgumentException(ctx + ": unknown effect '" + params.get("effect") + "'");
                    }
                });
        var evalReg = new io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry();
        io.github.chasehuegel.skilling.Skilling.registerBuiltinEvaluators(evalReg);
        var trigReg = new io.github.chasehuegel.skilling.engine.registry.TriggerRegistry();
        io.github.chasehuegel.skilling.Skilling.registerBuiltinTriggers(trigReg);
        var manager = new SkillManager(evalReg, registry, trigReg,
                new io.github.chasehuegel.skilling.engine.tag.TagResolver(
                        new io.github.chasehuegel.skilling.engine.tag.CustomTagLoader()),
                new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry());

        writeSkill("""
                      - type: "test:custom"
                        parameters:
                          effect: { constant: "minecraft:poisn" }
                """);
        assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(tempDir.resolve("skills/test.yml").toFile()));
    }

    /** No-op mechanic used only to register a validator under a test key. */
    public static class TestMechanic implements io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic {
        @Override
        public boolean execute(org.bukkit.entity.Player player, java.util.Map<String, Object> params,
                               org.bukkit.event.Event event) {
            return true;
        }
    }
}
