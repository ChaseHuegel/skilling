package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the load-time parameter validators. The potion-effect and attribute
 * key checks are injected so no live Bukkit registry is needed; material and
 * particle validation needs no registry at all.
 */
class MechanicParamValidatorsTest {

    @AfterEach
    void tearDown() {
        MechanicParamValidators.configureLookups(null, null, null, null, null, null);
    }

    private static void configureLookups() {
        MechanicParamValidators.configureLookups(
                key -> key.getKey().equals("poison"),
                key -> key.getKey().equals("movement_speed"),
                key -> key.getKey().equals("entity.player.levelup"),
                key -> key.getKey().equals("happy_villager"),
                key -> key.getKey().equals("netherite_pickaxe"),
                ref -> ref.equals("#minecraft:logs") || ref.equals("#c:ores")
                        || ref.equals("minecraft:stone"));
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
    void potionEffectRejectsMissingAndSkipsWhenLookupUnconfigured() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.potionEffect("ctx", Map.of("effect", "minecraft:poisn"), "effect"));
        assertThrows(IllegalArgumentException.class, () ->
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
    void attributeRejectsMissingAndSkipsWhenLookupUnconfigured() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.attribute("ctx", Map.of("attribute", "minecraft:whatever"), "attribute"));
        assertThrows(IllegalArgumentException.class, () ->
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
    void materialOrTagAcceptsMaterialTagAndCustomTag() {
        configureLookups();
        assertDoesNotThrow(() ->
                MechanicParamValidators.materialOrTag("ctx", Map.of("target", "#minecraft:logs"), "target"));
        assertDoesNotThrow(() ->
                MechanicParamValidators.materialOrTag("ctx", Map.of("target", "#c:ores"), "target"));
        assertDoesNotThrow(() ->
                MechanicParamValidators.materialOrTag("ctx", Map.of("target", "minecraft:stone"), "target"));
    }

    @Test
    void materialOrTagRejectsUnknownReference() {
        configureLookups();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.materialOrTag("ctx", Map.of("target", "#minecraft:typo"), "target"));
        assertTrue(ex.getMessage().contains("unknown tag or material '#minecraft:typo'"));
    }

    @Test
    void materialOrTagSkipsWhenAbsentBlankOrLookupUnconfigured() {
        assertDoesNotThrow(() -> MechanicParamValidators.materialOrTag("ctx", Map.of(), "target"));
        assertDoesNotThrow(() -> MechanicParamValidators.materialOrTag("ctx", Map.of("target", ""), "target"));
        assertDoesNotThrow(() ->
                MechanicParamValidators.materialOrTag("ctx", Map.of("target", "#minecraft:whatever"), "target"));
    }

    @Test
    void particleAcceptsKnownAndRejectsUnknown() {
        configureLookups();
        assertDoesNotThrow(() ->
                MechanicParamValidators.particle("ctx", Map.of("particle", "minecraft:happy_villager"), "particle"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.particle("ctx", Map.of("particle", "minecraft:not_a_particle"), "particle"));
        assertEquals("ctx: unknown particle 'minecraft:not_a_particle'", ex.getMessage());
    }

    @Test
    void particleSkipsWhenAbsentOrLookupUnconfigured() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.particle("ctx", Map.of("particle", "minecraft:whatever"), "particle"));
        assertDoesNotThrow(() -> MechanicParamValidators.particle("ctx", Map.of(), "particle"));
        assertDoesNotThrow(() -> MechanicParamValidators.particle("ctx", Map.of("particle", ""), "particle"));
    }

    @Test
    void soundAcceptsKnownAndRejectsUnknown() {
        configureLookups();
        assertDoesNotThrow(() ->
                MechanicParamValidators.sound("ctx", Map.of("type", "minecraft:entity.player.levelup"), "type"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.sound("ctx", Map.of("type", "minecraft:not_a_sound"), "type"));
        assertEquals("ctx: unknown sound 'minecraft:not_a_sound'", ex.getMessage());
    }

    @Test
    void soundSkipsWhenAbsentOrLookupUnconfigured() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.sound("ctx", Map.of("type", "minecraft:whatever"), "type"));
        assertDoesNotThrow(() -> MechanicParamValidators.sound("ctx", Map.of(), "type"));
    }

    @Test
    void soundRejectsMalformedNamespacedValue() {
        configureLookups();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.sound("ctx", Map.of("type", "NOT_A_SOUND"), "type"));
        assertTrue(ex.getMessage().contains("invalid namespaced identifier"));
    }

    @Test
    void recipeAcceptsRegisteredKeyAndSkipsUnknown() {
        configureLookups();
        assertDoesNotThrow(() ->
                MechanicParamValidators.recipe("ctx", Map.of("recipe", "minecraft:netherite_pickaxe"), "recipe"));
        assertDoesNotThrow(() ->
                MechanicParamValidators.recipe("ctx", Map.of("recipe", "minecraft:netherite_pickaxe"), "recipe"));
        // An unregistered but syntactically valid recipe warns instead of failing,
        // because a data pack or third-party plugin may register it after load.
        assertDoesNotThrow(() ->
                MechanicParamValidators.recipe("ctx", Map.of("recipe", "minecraft:later_recipe"), "recipe"));
    }

    @Test
    void recipeRejectsMissingAndMalformedValues() {
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.recipe("ctx", Map.of(), "recipe"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.recipe("ctx", Map.of("recipe", "NOT_A_RECIPE"), "recipe"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.recipe("ctx", Map.of("recipe", ""), "recipe"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.recipe("ctx", Map.of("recipe", 5.0), "recipe"));
    }

    @Test
    void recipeSkipsExistenceCheckWhenLookupUnconfigured() {
        assertDoesNotThrow(() ->
                MechanicParamValidators.recipe("ctx", Map.of("recipe", "minecraft:anything"), "recipe"));
    }

    @Test
    void radiusRejectsNegativeAndAcceptsNonNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.radius("ctx", Map.of("radius", -5.0), "radius"));
        assertDoesNotThrow(() -> MechanicParamValidators.radius("ctx", Map.of("radius", 0.0), "radius"));
        assertDoesNotThrow(() -> MechanicParamValidators.radius("ctx", Map.of("radius", 32.0), "radius"));
        assertDoesNotThrow(() -> MechanicParamValidators.radius("ctx", Map.of(), "radius"));
    }

    @Test
    void chanceRejectsOutOfBoundsAndAcceptsInRange() {
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.chance("ctx", Map.of("chance", -1.0), "chance", 100));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.chance("ctx", Map.of("chance", 101.0), "chance", 100));
        assertDoesNotThrow(() -> MechanicParamValidators.chance("ctx", Map.of("chance", 100.0), "chance", 100));
        assertDoesNotThrow(() -> MechanicParamValidators.chance("ctx", Map.of("chance", 0.0), "chance", 100));
        assertDoesNotThrow(() -> MechanicParamValidators.chance("ctx", Map.of(), "chance", 100));
    }

    @Test
    void nonNegativeRejectsNegativeAndAcceptsZero() {
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.nonNegative("ctx", Map.of("duration", -1.0), "duration"));
        assertDoesNotThrow(() -> MechanicParamValidators.nonNegative("ctx", Map.of("duration", 0.0), "duration"));
        assertDoesNotThrow(() -> MechanicParamValidators.nonNegative("ctx", Map.of(), "duration"));
    }

    @Test
    void positiveRejectsNonPositiveAndAcceptsPositive() {
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.positive("ctx", Map.of("multiplier", 0.0), "multiplier"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.positive("ctx", Map.of("multiplier", -1.0), "multiplier"));
        assertDoesNotThrow(() -> MechanicParamValidators.positive("ctx", Map.of("multiplier", 0.5), "multiplier"));
    }

    @Test
    void stringValuedNumericIsRejectedInsteadOfSkipped() {
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.nonNegative("ctx", Map.of("duration", "3"), "duration"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.radius("ctx", Map.of("radius", "5"), "radius"));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.chance("ctx", Map.of("chance", "100"), "chance", 100));
        assertThrows(IllegalArgumentException.class, () ->
                MechanicParamValidators.positive("ctx", Map.of("multiplier", "1.5"), "multiplier"));
    }
}
