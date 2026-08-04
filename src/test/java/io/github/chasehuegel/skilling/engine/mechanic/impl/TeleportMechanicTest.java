package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.BukkitMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:teleport} never drops the player into the void or an
 * unsurvivable fall: the destination is clamped to a spot with a solid floor
 * within a fall-safe distance, and the teleport is refused when none exists.
 */
class TeleportMechanicTest {

    private Block airBlock() {
        var block = mock(Block.class);
        when(block.isEmpty()).thenReturn(true);
        when(block.isSolid()).thenReturn(false);
        return block;
    }

    private Block solidFloor() {
        var block = mock(Block.class);
        when(block.isEmpty()).thenReturn(false);
        when(block.isSolid()).thenReturn(true);
        return block;
    }

    /** A world with a solid floor at y=0 and air above it. */
    private World floorWorld() {
        var world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getBlockAt(any(Location.class))).thenAnswer(inv -> {
            Location l = inv.getArgument(0);
            return l.getBlockY() <= 0 ? solidFloor() : airBlock();
        });
        return world;
    }

    private Location skyLocation(World world) {
        return new Location(world, 0, 100, 0);
    }

    @Test
    void findsLandingAboveSolidFloor() {
        var world = floorWorld();
        Location safe = TeleportMechanic.findSafeLocation(new Location(world, 0, 0, 0));
        assertNotNull(safe);
        assertEquals(1.0, safe.getY(), 1e-9);
    }

    @Test
    void refusesWhenNoFloorWithinFallSafeDistance() {
        var world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        var air = airBlock();
        when(world.getBlockAt(any(Location.class))).thenReturn(air);
        assertNull(TeleportMechanic.findSafeLocation(new Location(world, 0, 10, 0)));
    }

    @Test
    void refusesBelowTheVoidFloor() {
        var world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        var air = airBlock();
        when(world.getBlockAt(any(Location.class))).thenReturn(air);
        assertNull(TeleportMechanic.findSafeLocation(new Location(world, 0, -60, 0)));
    }

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
        var world = floorWorld();
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
        when(world.getMinHeight()).thenReturn(-64);
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
        var world = floorWorld();
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

    @Test
    void neverTeleportsIntoTheVoidWhenNoFloorExists() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        var air = airBlock();
        when(world.getBlockAt(any(Location.class))).thenReturn(air);
        var loc = spy(skyLocation(world));
        when(player.getLocation()).thenReturn(loc);
        when(player.getTargetBlockExact(10)).thenReturn(null);

        assertFalse(mechanic.execute(player, Map.of("range", 10.0), BukkitMock.mockInteractEvent(player)));
        verify(player, never()).teleport(any(Location.class));
    }
}
