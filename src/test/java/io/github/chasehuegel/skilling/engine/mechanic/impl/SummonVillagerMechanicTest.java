package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:summon_villager}: a right-click summons a jobless
 * {@link Villager} at the player's location, and a left-click or non-interact
 * event is a no-op.
 */
class SummonVillagerMechanicTest {

    @Test
    void rightClickSpawnsJoblessVillagerAtPlayer() {
        var world = mock(World.class);
        var location = new Location(world, 1, 2, 3);
        var player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);

        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertTrue(new SummonVillagerMechanic().execute(player, Map.of(), event));

        ArgumentCaptor<Consumer<Villager>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(world).spawn(eq(location), eq(Villager.class),
                eq(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM),
                eq(false), captor.capture());
        var villager = mock(Villager.class);
        captor.getValue().accept(villager);
        verify(villager).setProfession(Villager.Profession.NONE);
    }

    @Test
    void leftClickIsNoOp() {
        var player = mock(Player.class);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);

        assertFalse(new SummonVillagerMechanic().execute(player, Map.of(), event));
        verify(player, never()).getWorld();
    }

    @Test
    void nonInteractEventIsNoOp() {
        var player = mock(Player.class);
        assertFalse(new SummonVillagerMechanic().execute(player, Map.of(),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
        verify(player, never()).getWorld();
    }
}
