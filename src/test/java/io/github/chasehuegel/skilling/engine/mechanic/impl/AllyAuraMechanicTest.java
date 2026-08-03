package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the parameter extraction, radius clamping, and radius-0 behavior of
 * {@link AllyAuraMechanic}. A recording lookup stands in for the live Bukkit
 * registry, which is unavailable in the JUnit JVM.
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

    @Test
    void radiusZeroDoesNotSelfBuff() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);
        when(loc.getNearbyPlayers(0.0)).thenReturn(List.of());

        AllyAuraMechanic.apply(player, mock(PotionEffect.class), 0.0);

        verify(player, never()).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void positiveRadiusBuffsCasterAndNearbyAllies() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var ally = mock(Player.class);
        when(ally.getUniqueId()).thenReturn(UUID.randomUUID());
        var loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);
        when(loc.getNearbyPlayers(8.0)).thenReturn(List.of(player, ally));

        var effect = mock(PotionEffect.class);
        AllyAuraMechanic.apply(player, effect, 8.0);

        verify(player).addPotionEffect(effect);
        verify(ally).addPotionEffect(effect);
    }
}
