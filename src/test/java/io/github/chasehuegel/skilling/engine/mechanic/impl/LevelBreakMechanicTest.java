package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:level_break} chains only on the XZ plane: horizontal
 * neighbors break while blocks directly above/below the origin stay intact,
 * and {@code chain_limit} still caps the total.
 */
class LevelBreakMechanicTest {

    @AfterEach
    void tearDown() {
        ChainBreakMechanic.clearChainProcessingForTest();
    }

    private Block block(World world, int x, int y, int z, Material material) {
        var block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(world, x, y, z));
        when(block.getType()).thenReturn(material);
        return block;
    }

    private Player player() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        var tool = mock(ItemStack.class);
        var material = mock(Material.class);
        when(material.getMaxDurability()).thenReturn((short) 100);
        when(tool.getType()).thenReturn(material);
        var meta = mock(Damageable.class);
        when(meta.getDamage()).thenReturn(0);
        when(tool.getItemMeta()).thenReturn(meta);
        when(inv.getItemInMainHand()).thenReturn(tool);
        return player;
    }

    private BlockBreakEvent breakAt(Block origin) {
        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(origin);
        return event;
    }

    @Test
    void returnsFalseForNonBlockBreakEvent() {
        assertFalse(new LevelBreakMechanic().execute(player(), Map.of("chain_limit", 10),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void horizontalNeighborsChainButVerticalColumnIsUntouched() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.STONE);
        var east = block(world, 1, 0, 0, Material.STONE);
        var above = block(world, 0, 1, 0, Material.STONE); // must never break
        var below = block(world, 0, -1, 0, Material.STONE); // must never break
        var air = block(world, 99, 99, 99, Material.AIR);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(east.getLocation(), east);
        neighbors.put(above.getLocation(), above);
        neighbors.put(below.getLocation(), below);
        when(origin.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0), dy = inv.getArgument(1), dz = inv.getArgument(2);
            Location loc = new Location(world, dx, dy, dz);
            return neighbors.getOrDefault(loc, air);
        });
        when(east.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            new LevelBreakMechanic().execute(player(), Map.of("chain_limit", 10), breakAt(origin));
        }

        verify(east).breakNaturally(any(ItemStack.class));
        verify(above, never()).breakNaturally(any(ItemStack.class));
        verify(below, never()).breakNaturally(any(ItemStack.class));
    }

    @Test
    void chainLimitCapsTotalBrokenBlocks() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.STONE);
        var east = block(world, 1, 0, 0, Material.STONE);
        var north = block(world, 0, 0, 1, Material.STONE);
        var farEast = block(world, 2, 0, 0, Material.STONE); // reachable only via east
        var air = block(world, 99, 99, 99, Material.AIR);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(east.getLocation(), east);
        neighbors.put(north.getLocation(), north);
        when(origin.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0), dy = inv.getArgument(1), dz = inv.getArgument(2);
            Location loc = new Location(world, dx, dy, dz);
            return neighbors.getOrDefault(loc, air);
        });
        when(east.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0), dy = inv.getArgument(1), dz = inv.getArgument(2);
            Location loc = new Location(world, dx, dy, dz);
            return loc.equals(farEast.getLocation()) ? farEast : air;
        });
        when(north.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(farEast.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            // limit 2: origin is free, so only one of the two immediate neighbors breaks.
            new LevelBreakMechanic().execute(player(), Map.of("chain_limit", 2), breakAt(origin));
        }

        verify(east, org.mockito.Mockito.times(1)).breakNaturally(any(ItemStack.class));
        verify(farEast, never()).breakNaturally(any(ItemStack.class));
    }
}
