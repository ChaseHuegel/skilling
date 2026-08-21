package io.github.chasehuegel.skilling.engine;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Exploration skill's milestone bindings: each ability unlocks at
 * its tier, binds the expected trigger, and wires the intended mechanic, so a
 * drift in the skill YAML (a wrong trigger, level, or mechanic) is caught at
 * test time.
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

    private static boolean hasMechanic(SkillDefinition.Ability ability, String type) {
        return ability.mechanics().stream().anyMatch(m -> type.equals(m.type()));
    }

    private static boolean hasFilter(SkillDefinition.Ability ability, String state) {
        return ability.mechanics().stream()
                .flatMap(m -> m.filters().stream())
                .anyMatch(f -> state.equals(f.state()));
    }

    @Test
    void scavengerIsTheL1CoreScalarOnLoot() {
        SkillDefinition.Ability scavenger = ability("scavenger");
        assertEquals(1, scavenger.unlockLevel());
        assertEquals("loot", scavenger.trigger());
        assertTrue(hasMechanic(scavenger, "core:loot_bonus"), "scavenger must use core:loot_bonus");
    }

    @Test
    void diverseHorizonIsTheL15QolOnMapExplore() {
        SkillDefinition.Ability diverseHorizon = ability("diverse_horizon");
        assertEquals(15, diverseHorizon.unlockLevel());
        assertEquals("map_explore", diverseHorizon.trigger());
        assertTrue(hasMechanic(diverseHorizon, "core:biome_discovery"),
                "diverse_horizon must use core:biome_discovery");
        assertTrue(diverseHorizon.mechanics().stream().flatMap(m -> m.parameters().keySet().stream())
                        .anyMatch("max_biomes"::equals),
                "diverse_horizon must carry the max_biomes cap");
    }

    @Test
    void wayfindersCompassIsTheL25PrimaryOnCompass() {
        SkillDefinition.Ability wayfinder = ability("wayfinders_compass");
        assertEquals(25, wayfinder.unlockLevel());
        assertEquals("right_click_air", wayfinder.trigger());
        assertTrue(hasMechanic(wayfinder, "core:locate"), "wayfinders_compass must use core:locate");
    }

    @Test
    void vaultRaiderIsTheL50MajorOnVaults() {
        SkillDefinition.Ability vaultRaider = ability("vault_raider");
        assertEquals(50, vaultRaider.unlockLevel());
        assertEquals("vault_change", vaultRaider.trigger());
        assertTrue(hasMechanic(vaultRaider, "core:vault_bonus"),
                "vault_raider must use core:vault_bonus");
    }

    @Test
    void skywardIsTheL75SynergyOnElytraCollision() {
        SkillDefinition.Ability skyward = ability("skyward");
        assertEquals(75, skyward.unlockLevel());
        assertEquals("entity_damage_taken", skyward.trigger());
        assertTrue(hasMechanic(skyward, "core:cancel_damage"),
                "skyward must use core:cancel_damage");
        assertTrue(hasFilter(skyward, "cause:fly_into_wall"),
                "skyward must gate on elytra wall-collision damage");
    }

    @Test
    void lodestoneRecallIsTheL100Capstone() {
        SkillDefinition.Ability recall = ability("lodestone_recall");
        assertEquals(100, recall.unlockLevel());
        assertEquals("right_click_air", recall.trigger());
        assertTrue(hasMechanic(recall, "core:teleport_lodestone"),
                "lodestone_recall must use core:teleport_lodestone");
    }
}