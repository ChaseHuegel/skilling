package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the namespaced-key resolution path and fail-fast behavior of
 * {@link ModifyAttributeMechanic#resolveAttribute(Object)}. A recording lookup
 * stands in for the live Bukkit registry, which is unavailable in the JUnit JVM.
 */
class ModifyAttributeMechanicTest {

    private static NamespacedKey keyLookedUp(Object rawAttr) {
        List<NamespacedKey> seen = new ArrayList<>();
        assertThrows(IllegalArgumentException.class,
                () -> ModifyAttributeMechanic.resolveAttribute(rawAttr, key -> {
                    seen.add(key);
                    return null;
                }));
        return seen.get(0);
    }

    @Test
    void namespacedMovementSpeedResolvesThroughRegistryLookup() {
        assertEquals(NamespacedKey.fromString("minecraft:movement_speed"),
                keyLookedUp("minecraft:movement_speed"));
    }

    @Test
    void namespacedArmorResolvesThroughRegistryLookup() {
        assertEquals(NamespacedKey.fromString("minecraft:armor"), keyLookedUp("minecraft:armor"));
    }

    @Test
    void legacyNumericIdMapsToNamespacedKey() {
        assertEquals(NamespacedKey.fromString("minecraft:movement_speed"), keyLookedUp(4));
    }

    @Test
    void unknownNamespacedKeyFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> ModifyAttributeMechanic.resolveAttribute("minecraft:nonexistent", key -> null));
    }

    @Test
    void nullInputFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> ModifyAttributeMechanic.resolveAttribute(null, key -> null));
    }
}
