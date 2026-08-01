package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the parameter extraction and radius clamping of {@link AllyAuraMechanic}
 * in isolation. A recording lookup stands in for the live Bukkit registry, which
 * is unavailable in the JUnit JVM.
 */
class AllyAuraMechanicTest {

    @Test
    void defaultConfigUsesEightBlockRadiusFiveSecondDuration() {
        var config = AllyAuraMechanic.parseConfig(Map.of());
        assertEquals(8.0, config.radius());
        assertEquals(100, config.durationTicks());
        assertEquals(0, config.amplifier());
    }

    @Test
    void customConfigConvertsDurationToTicks() {
        var config = AllyAuraMechanic.parseConfig(Map.of(
                "radius", 5.0,
                "duration", 3.0,
                "amplifier", 2.0
        ));
        assertEquals(5.0, config.radius());
        assertEquals(60, config.durationTicks());
        assertEquals(2, config.amplifier());
    }

    @Test
    void radiusIsClampedToBukkitBounds() {
        assertEquals(0.0, AllyAuraMechanic.clampRadius(-5.0));
        assertEquals(8.0, AllyAuraMechanic.clampRadius(8.0));
        assertEquals(32.0, AllyAuraMechanic.clampRadius(100.0));
    }

    @Test
    void effectParamIsRoutedThroughNamespacedLookup() {
        List<NamespacedKey> seen = new ArrayList<>();
        assertThrows(IllegalArgumentException.class,
                () -> AllyAuraMechanic.resolveParams(
                        Map.of("effect", "minecraft:regeneration"),
                        key -> {
                            seen.add(key);
                            return null;
                        }));
        assertEquals(NamespacedKey.fromString("minecraft:regeneration"), seen.get(0));
    }

    @Test
    void missingEffectParamFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> AllyAuraMechanic.resolveParams(Map.of(), key -> null));
    }

    @Test
    void unknownEffectKeyFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> AllyAuraMechanic.resolveParams(Map.of("effect", "minecraft:nonexistent"), key -> null));
    }
}
