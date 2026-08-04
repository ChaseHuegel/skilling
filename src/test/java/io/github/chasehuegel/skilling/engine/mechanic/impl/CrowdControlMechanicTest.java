package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:crowd_control} clamps its AoE radius to Bukkit's
 * [0, 32] entity-search bounds and never affects the casting player.
 */
class CrowdControlMechanicTest {

    @Test
    void radiusIsClampedToBukkitBounds() {
        assertEquals(0.0, CrowdControlMechanic.clampRadius(-5.0));
        assertEquals(5.0, CrowdControlMechanic.clampRadius(5.0));
        assertEquals(32.0, CrowdControlMechanic.clampRadius(1000.0));
    }

    @Test
    void oversizedRadiusUsesClampedScanAndExcludesCaster() {
        var caster = mock(Player.class);
        when(caster.getUniqueId()).thenReturn(UUID.randomUUID());
        var origin = mock(LivingEntity.class);
        when(origin.getUniqueId()).thenReturn(UUID.randomUUID());
        var hostile = mock(Monster.class);
        when(hostile.getUniqueId()).thenReturn(UUID.randomUUID());
        when(origin.getNearbyEntities(32.0, 32.0, 32.0)).thenReturn(List.of(caster, hostile));

        var effect = mock(PotionEffect.class);
        assertTrue(CrowdControlMechanic.apply(origin, caster, effect, 1000.0, "hostiles"));
        // The unbounded scan must never run: the radius is clamped to [0, 32].
        verify(origin).getNearbyEntities(32.0, 32.0, 32.0);
        verify(hostile).addPotionEffect(effect);
        verify(caster, never()).addPotionEffect(effect);
    }
}
