package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the load-time parameter validators. The potion-effect and attribute
 * key checks are injected so no live Bukkit registry is needed; material and
 * particle validation needs no registry at all.
 */
class MechanicParamValidatorsTest {

    @AfterEach
    void tearDown() {
        MechanicParamValidators.configureLookups(null, null);
    }

    private static void configureLookups() {
        MechanicParamValidators.configureLookups(
                key -> key.getKey().equals("poison"),
                key -> key.getKey().equals("movement_speed"));
    }

    @Test
    void potionEffectAcceptsKnownKeyAndRejectsUnknown() {
        configureLookups();
        assertDoesNotThrow(() ->
                MechanicParamValidators.potionEffect("ctx", Map.of("effect", "minecraft:poison"), "effect"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.potionEffect("ctx", Map.of("effect", "minecraft:poisn"), "effect"));
    }

    @Test
    void potionEffectSkipsWhenAbsentOrLookupUnconfigured() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.potionEffect("ctx", Map.of("effect", "minecraft:poisn"), "effect"));
        assertDoesNotThrow(() ->
                MechanicParamValidators.potionEffect("ctx", Map.of(), "effect"));
    }

    @Test
    void attributeAcceptsKnownKeyAndRejectsUnknown() {
        configureLookups();
        assertDoesNotThrow(() ->
                MechanicParamValidators.attribute("ctx", Map.of("attribute", "minecraft:movement_speed"), "attribute"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.attribute("ctx", Map.of("attribute", "minecraft:movement_speeed"), "attribute"));
    }

    @Test
    void attributeSkipsWhenAbsentOrLookupUnconfigured() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.attribute("ctx", Map.of("attribute", "minecraft:whatever"), "attribute"));
        assertDoesNotThrow(() ->
                MechanicParamValidators.attribute("ctx", Map.of(), "attribute"));
    }

    @Test
    void materialAcceptsKnownAndRejectsUnknown() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.material("ctx", Map.of("material", "minecraft:shield"), "material"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.material("ctx", Map.of("material", "minecraft:not_a_material"), "material"));
    }

    @Test
    void materialSkipsWhenAbsentOrBlank() {
        assertDoesNotThrow(() -> MechanicParamValidators.material("ctx", Map.of(), "material"));
        assertDoesNotThrow(() -> MechanicParamValidators.material("ctx", Map.of("material", ""), "material"));
    }

    @Test
    void particleAcceptsKnownAndRejectsUnknown() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.particle("ctx", Map.of("particle", "happy_villager"), "particle"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.particle("ctx", Map.of("particle", "NOT_A_PARTICLE"), "particle"));
        assertEquals("ctx: unknown particle 'NOT_A_PARTICLE'", ex.getMessage());
    }

    @Test
    void particleSkipsWhenAbsentOrBlank() {
        assertDoesNotThrow(() -> MechanicParamValidators.particle("ctx", Map.of(), "particle"));
        assertDoesNotThrow(() -> MechanicParamValidators.particle("ctx", Map.of("particle", ""), "particle"));
    }
}
