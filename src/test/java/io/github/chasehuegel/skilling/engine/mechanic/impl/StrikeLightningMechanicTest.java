package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:strike_lightning}: a right-click aimed at a hostile
 * strikes a visual lightning effect and deals the configured damage; no target
 * is a no-op.
 */
class StrikeLightningMechanicTest {

    private static final Map<String, Object> PARAMS = Map.of("damage", 8.0);

    @Test
    void aimedHostileReceivesLightningVisualAndDamage() {
        var player = mock(Player.class);
        var target = mock(Zombie.class);
        when(target.isDead()).thenReturn(false);
        when(player.getTargetEntity(anyInt())).thenReturn(target);
        var world = mock(World.class);
        var loc = mock(Location.class);
        when(target.getLocation()).thenReturn(loc);
        when(target.getWorld()).thenReturn(world);

        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertTrue(new StrikeLightningMechanic().execute(player, PARAMS, event));
        verify(world).strikeLightningEffect(loc);
        verify(target).damage(8.0, player);
    }

    @Test
    void noAimedTargetIsNoOp() {
        var player = mock(Player.class);
        when(player.getTargetEntity(anyInt())).thenReturn(null);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertFalse(new StrikeLightningMechanic().execute(player, PARAMS, event));
    }

    @Test
    void playerIsNeverStruck() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        when(player.getTargetEntity(anyInt())).thenReturn(other);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(other.getWorld()).thenReturn(mock(World.class));

        assertFalse(new StrikeLightningMechanic().execute(player, PARAMS, event));
    }
}