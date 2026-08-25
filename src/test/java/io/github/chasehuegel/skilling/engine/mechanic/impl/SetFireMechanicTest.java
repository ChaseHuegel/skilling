package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:set_fire}: an aimed hostile is ignited for the configured
 * ticks; no target or a non-positive value is a no-op.
 */
class SetFireMechanicTest {

    private static final Map<String, Object> PARAMS = Map.of("ticks", 100.0);

    @Test
    void aimedHostileIsSetOnFire() {
        var player = mock(Player.class);
        var target = mock(Zombie.class);
        when(target.isDead()).thenReturn(false);
        when(player.getTargetEntity(anyInt())).thenReturn(target);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertTrue(new SetFireMechanic().execute(player, PARAMS, event));
        verify(target).setFireTicks(100);
    }

    @Test
    void noAimedTargetIsNoOp() {
        var player = mock(Player.class);
        when(player.getTargetEntity(anyInt())).thenReturn(null);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        assertFalse(new SetFireMechanic().execute(player, PARAMS, event));
    }

    @Test
    void nonPositiveTicksIsNoOp() {
        var player = mock(Player.class);
        var target = mock(Zombie.class);
        when(target.isDead()).thenReturn(false);
        when(player.getTargetEntity(anyInt())).thenReturn(target);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertFalse(new SetFireMechanic().execute(player, Map.of("ticks", 0.0), event));
        verify(target, never()).setFireTicks(anyInt());
    }
}