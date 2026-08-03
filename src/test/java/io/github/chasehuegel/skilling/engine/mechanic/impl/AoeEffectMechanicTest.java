package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:aoe_effect} never buffs hostile mobs by default and
 * always excludes the caster.
 */
class AoeEffectMechanicTest {

    @Test
    void defaultTargetsSkipHostileMobsAndThePlayer() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);
        var hostile = mock(Monster.class);
        when(hostile.getUniqueId()).thenReturn(UUID.randomUUID());
        var neutral = mock(LivingEntity.class);
        when(neutral.getUniqueId()).thenReturn(UUID.randomUUID());
        when(loc.getNearbyLivingEntities(5.0)).thenReturn(List.of(hostile, neutral));

        var effect = mock(PotionEffect.class);
        assertTrue(AoeEffectMechanic.apply(player, effect, 5.0, "allies"));
        verify(hostile, never()).addPotionEffect(effect);
        verify(neutral).addPotionEffect(effect);
    }
}
