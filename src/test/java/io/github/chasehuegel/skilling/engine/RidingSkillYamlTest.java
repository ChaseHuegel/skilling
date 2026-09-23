package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.TestSkillManager;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the bundled Riding skill: it parses with the vehicle-focused triggers
 * ({@code ride_distance}, {@code mount_damage_taken}) and mechanics
 * ({@code core:mounted_ward}, {@code core:mounted_speed}, {@code riding_type}),
 * and the horse companion arc is bound early so recall is useful in early game.
 */
class RidingSkillYamlTest {

    private SkillDefinition.Ability ability(String id) {
        SkillDefinition skill = TestSkillManager.newBuiltIn().parseSkill(
                new java.io.File(Objects.requireNonNull(getClass().getClassLoader().getResource("skills/riding.yml")).getFile()));
        return skill.abilities().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("riding.yml has no ability '" + id + "'"));
    }

    @Test
    void xpSourcesCoverMountCombatAndDistance() {
        SkillDefinition skill = TestSkillManager.newBuiltIn().parseSkill(
                new java.io.File(Objects.requireNonNull(getClass().getClassLoader().getResource("skills/riding.yml")).getFile()));
        var triggers = skill.xpSources().stream().map(SkillDefinition.XpSource::trigger).toList();
        assertTrue(triggers.contains("ride_horse"), "riding must source XP from mounting");
        assertTrue(triggers.contains("ride_distance"), "riding must source XP from riding distance");
        assertTrue(triggers.contains("entity_damage"), "riding must source XP from mounted combat");
        assertTrue(triggers.contains("player_tame"), "riding must source XP from taming");
    }

    @Test
    void horseBondAndRecallLiveEarly() {
        assertEquals(15, ability("horse_bond").unlockLevel());
        assertEquals("right_click_entity", ability("horse_bond").trigger());
        assertTrue(ability("horse_bond").mechanics().stream()
                .anyMatch(m -> "core:mark_companion".equals(m.type())));

        SkillDefinition.Ability recall = ability("horse_recall");
        assertEquals(25, recall.unlockLevel());
        assertEquals("right_click", recall.trigger());
        assertTrue(recall.mechanics().stream()
                .anyMatch(m -> "core:summon_companion".equals(m.type())));
        assertTrue(recall.requirements().state().contains("has_tamed:horse"));
        assertNotNull(recall.requirements().exhaustion(), "recall must be gated by hunger");
    }

    @Test
    void wardMountUsesMountDamageTrigger() {
        SkillDefinition.Ability ward = ability("ward_mount");
        assertEquals("mount_damage_taken", ward.trigger());
        assertTrue(ward.mechanics().stream().anyMatch(m -> "core:mounted_ward".equals(m.type())));
    }

    @Test
    void terrainMasteryGatesMountedSpeedToLivingMounts() {
        SkillDefinition.Ability terrain = ability("terrain_mastery");
        assertEquals("ride_horse", terrain.trigger());
        SkillDefinition.MechanicEntry speed = terrain.mechanics().stream()
                .filter(m -> "core:mounted_speed".equals(m.type())).findFirst().orElseThrow();
        assertTrue(speed.filters().stream().anyMatch(f ->
                        f.state() != null && f.state().equals("riding_type:living_mount")),
                "terrain_mastery must gate mounted_speed to living mounts");
    }

    @Test
    void mountedCombatIsOneDamageline() {
        // The L1 identity scalar is the only flat mounted-damage line; the
        // capstone must not stack raw mounted damage on top of it.
        assertEquals(1, ability("mounted_combat").unlockLevel());
        assertTrue(ability("master_rider").mechanics().stream()
                .noneMatch(m -> "core:modify_damage".equals(m.type())));
    }
}