package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Exploration skill's foundational passive: Pathfinder binds the
 * {@code chunk_load} trigger and unlocks at level 0 (the starting level), so
 * exploring grants its speed boost even though exploring grants no exploration
 * XP (chunk_load XP stays with Survival).
 */
class ExplorationAbilityBindingTest {

    private SkillDefinition.Ability ability(String id) {
        SkillDefinition skill = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn().parseSkill(
                new java.io.File(Objects.requireNonNull(
                        getClass().getClassLoader().getResource("skills/exploration.yml")).getFile()));
        return skill.abilities().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("exploration.yml has no ability '" + id + "'"));
    }

    @Test
    void pathfinderBindsChunkLoadAndUnlocksAtLevelZero() {
        SkillDefinition.Ability pathfinder = ability("pathfinder");
        assertEquals("chunk_load", pathfinder.trigger());
        assertEquals(0, pathfinder.unlockLevel(),
                "exploring grants no exploration XP, so a level-1 gate would lock Pathfinder forever");
        assertTrue(pathfinder.mechanics().stream().anyMatch(m -> "core:speed_bonus".equals(m.type())),
                "pathfinder must use core:speed_bonus");
    }

    @Test
    void pathfinderSpeedScalesWithLevel() {
        SkillDefinition.Ability pathfinder = ability("pathfinder");
        SkillDefinition.MechanicEntry speed = pathfinder.mechanics().stream()
                .filter(m -> "core:speed_bonus".equals(m.type())).findFirst().orElseThrow();
        assertTrue(speed.parameters().containsKey("multiplier"), "speed_bonus must carry a multiplier");
        assertTrue(speed.parameters().containsKey("duration"), "speed_bonus must carry a duration");
    }
}
