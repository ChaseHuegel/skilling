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
 * Verifies {@code core:field_aura} never buffs hostile mobs by default and that
 * the {@code targets} parameter selects who receives the effect.
 */
class FieldAuraMechanicTest {

    private static final double RADIUS = 8.0;

    private static final class Scene {
        final Player player = mock(Player.class);
        final Monster hostile = mock(Monster.class);
        final LivingEntity neutral = mock(LivingEntity.class);
        final PotionEffect effect = mock(PotionEffect.class);

        Scene() {
            UUID playerId = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerId);
            when(hostile.getUniqueId()).thenReturn(UUID.randomUUID());
            when(neutral.getUniqueId()).thenReturn(UUID.randomUUID());
            Location loc = mock(Location.class);
            when(player.getLocation()).thenReturn(loc);
            when(loc.getNearbyLivingEntities(RADIUS)).thenReturn(List.of(hostile, neutral));
        }
    }

    @Test
    void defaultTargetsSkipHostileMobs() {
        Scene s = new Scene();
        assertTrue(FieldAuraMechanic.apply(s.player, s.effect, RADIUS, "allies"));
        verify(s.player).addPotionEffect(s.effect);
        verify(s.hostile, never()).addPotionEffect(s.effect);
        verify(s.neutral).addPotionEffect(s.effect);
    }

    @Test
    void hostilesTargetOnlyBuffsEnemies() {
        Scene s = new Scene();
        assertTrue(FieldAuraMechanic.apply(s.player, s.effect, RADIUS, "hostiles"));
        verify(s.player, never()).addPotionEffect(s.effect);
        verify(s.hostile).addPotionEffect(s.effect);
        verify(s.neutral, never()).addPotionEffect(s.effect);
    }

    @Test
    void allTargetsBuffsEveryone() {
        Scene s = new Scene();
        assertTrue(FieldAuraMechanic.apply(s.player, s.effect, RADIUS, "all"));
        verify(s.player).addPotionEffect(s.effect);
        verify(s.hostile).addPotionEffect(s.effect);
        verify(s.neutral).addPotionEffect(s.effect);
    }
}
