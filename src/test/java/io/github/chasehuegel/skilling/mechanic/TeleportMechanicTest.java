package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.TeleportMechanic;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeleportMechanicTest {

    @Test
    void returnsFalseForNonInteractEvent() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("range", 10.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithRangeZero() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("range", 0.0), BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void returnsTrueWithValidRange() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var block = mock(Block.class);
        when(block.isEmpty()).thenReturn(true);
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        var loc = spy(new Location(world, 0, 0, 0));
        when(player.getLocation()).thenReturn(loc);
        when(player.getTargetBlockExact(10)).thenReturn(null);
        when(player.teleport(any(Location.class))).thenReturn(true);
        assertTrue(mechanic.execute(player, Map.of("range", 10.0), BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void returnsFalseWhenNoSafeLocation() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var block = mock(Block.class);
        // Nothing is empty, so findSafeLocation finds no spot.
        when(block.isEmpty()).thenReturn(false);
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        var loc = spy(new Location(world, 0, 0, 0));
        when(player.getLocation()).thenReturn(loc);
        when(player.getTargetBlockExact(10)).thenReturn(null);

        assertFalse(mechanic.execute(player, Map.of("range", 10.0), BukkitMock.mockInteractEvent(player)));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void returnsFalseWhenTeleportIsCancelled() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var block = mock(Block.class);
        when(block.isEmpty()).thenReturn(true);
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        var loc = spy(new Location(world, 0, 0, 0));
        when(player.getLocation()).thenReturn(loc);
        when(player.getTargetBlockExact(10)).thenReturn(null);
        // Another plugin cancels the teleport; the mechanic reports a no-op.
        when(player.teleport(any(Location.class))).thenReturn(false);

        assertFalse(mechanic.execute(player, Map.of("range", 10.0), BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void leftClickIsANoOpThatDoesNotTeleport() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        var event = mock(org.bukkit.event.player.PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(org.bukkit.event.block.Action.LEFT_CLICK_AIR);

        assertFalse(mechanic.execute(player, Map.of("range", 10.0), event));
        verify(player, never()).teleport(any(Location.class));
    }
}
