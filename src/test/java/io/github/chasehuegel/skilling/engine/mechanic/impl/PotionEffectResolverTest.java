package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the namespaced-key resolution path and fail-fast behavior of
 * {@link PotionEffectResolver}. A recording lookup stands in for the live
 * Bukkit registry, which is unavailable in the JUnit JVM.
 */
class PotionEffectResolverTest {

    private static NamespacedKey keyLookedUp(Object rawEffect) {
        List<NamespacedKey> seen = new ArrayList<>();
        assertThrows(IllegalArgumentException.class,
                () -> PotionEffectResolver.resolve(rawEffect, key -> {
                    seen.add(key);
                    return null;
                }));
        return seen.get(0);
    }

    @Test
    void namespacedSpeedResolvesThroughRegistryLookup() {
        assertEquals(NamespacedKey.fromString("minecraft:speed"), keyLookedUp("minecraft:speed"));
    }

    @Test
    void namespacedPoisonResolvesThroughRegistryLookup() {
        assertEquals(NamespacedKey.fromString("minecraft:poison"), keyLookedUp("minecraft:poison"));
    }

    @Test
    void namespacedSlownessResolvesThroughRegistryLookup() {
        assertEquals(NamespacedKey.fromString("minecraft:slowness"), keyLookedUp("minecraft:slowness"));
    }

    @Test
    void unknownNamespacedKeyFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> PotionEffectResolver.resolve("minecraft:nonexistent", key -> null));
    }

    @Test
    void malformedNamespacedKeyFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> PotionEffectResolver.resolve("minecraft:Not A Valid Key!", key -> null));
    }

    @Test
    void nullInputFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> PotionEffectResolver.resolve(null, key -> null));
    }

    @Test
    void legacyNumericIdMapsToNamespacedKey() {
        assertEquals(NamespacedKey.fromString("minecraft:poison"), keyLookedUp(19));
    }

    @Test
    void stringConstantFromEvaluatorPassesKeyThroughRegistryLookup() {
        var evaluator = new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator("minecraft:slowness");
        List<NamespacedKey> seen = new ArrayList<>();
        assertThrows(IllegalArgumentException.class,
                () -> PotionEffectResolver.resolve((String) evaluator.rawValue(), key -> {
                    seen.add(key);
                    return null;
                }));
        assertEquals(NamespacedKey.fromString("minecraft:slowness"), seen.get(0));
    }
}
