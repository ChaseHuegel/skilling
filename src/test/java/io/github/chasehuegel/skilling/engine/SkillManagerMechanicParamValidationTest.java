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
                key -> key.getKey().equals("movement_speed"));
    }

    @AfterEach
    void tearDown() {
        MechanicParamValidators.configureLookups(null, null);
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
                () -> newSkillManager().loadSkills(tempDir.resolve("skills").toFile()));
        assertTrue(ex.getMessage().contains("poisn"), ex.getMessage());
    }

    @Test
    void unknownAttributeFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:modify_attribute"
                        parameters:
                          attribute: { constant: "minecraft:movement_speeed" }
                          amount: 1.0
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().loadSkills(tempDir.resolve("skills").toFile()));
        assertTrue(ex.getMessage().contains("movement_speeed"), ex.getMessage());
    }

    @Test
    void unknownMaterialFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:set_cooldown"
                        parameters:
                          material: { constant: "minecraft:not_a_material" }
                          ticks: 20
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().loadSkills(tempDir.resolve("skills").toFile()));
        assertTrue(ex.getMessage().contains("not_a_material"), ex.getMessage());
    }

    @Test
    void unknownParticleFailsToLoad() throws Exception {
        writeSkill("""
                      - type: "core:block_particles"
                        parameters:
                          particle: { constant: "NOT_A_PARTICLE" }
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newSkillManager().loadSkills(tempDir.resolve("skills").toFile()));
        assertTrue(ex.getMessage().contains("NOT_A_PARTICLE"), ex.getMessage());
    }

    @Test
    void validParamsLoadAndExecuteUnchanged() throws Exception {
        writeSkill("""
                      - type: "core:ally_aura"
                        parameters:
                          effect: { constant: "minecraft:poison" }
                          radius: 5
                          duration: 3
                      - type: "core:block_particles"
                        parameters:
                          particle: { constant: "HAPPY_VILLAGER" }
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
                        new io.github.chasehuegel.skilling.engine.tag.CustomTagLoader()));

        writeSkill("""
                      - type: "test:custom"
                        parameters:
                          effect: { constant: "minecraft:poisn" }
                """);
        assertThrows(IllegalArgumentException.class,
                () -> manager.loadSkills(tempDir.resolve("skills").toFile()));
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
