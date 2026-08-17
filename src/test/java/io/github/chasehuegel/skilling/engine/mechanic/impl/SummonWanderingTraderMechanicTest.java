package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:summon_wandering_trader}: a right-click summons a
 * {@link WanderingTrader} at the player's location, and a non-right-click or
 * non-interact event is a no-op.
 */
class SummonWanderingTraderMechanicTest {

    @Test
    void rightClickSpawnsWanderingTraderAtPlayer() {
        var world = mock(World.class);
        var location = new Location(world, 1, 2, 3);
        var player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);

        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        assertTrue(new SummonWanderingTraderMechanic().execute(player, Map.of(), event));
        verify(world).spawn(eq(location), eq(WanderingTrader.class),
                eq(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM),
                eq(false), any());
    }

    @Test
    void leftClickIsNoOp() {
        var player = mock(Player.class);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);

        assertFalse(new SummonWanderingTraderMechanic().execute(player, Map.of(), event));
        verify(player, org.mockito.Mockito.never()).getWorld();
    }

    @Test
    void nonInteractEventIsNoOp() {
        var player = mock(Player.class);
        assertFalse(new SummonWanderingTraderMechanic().execute(player, Map.of(),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
        verify(player, org.mockito.Mockito.never()).getWorld();
    }
}
