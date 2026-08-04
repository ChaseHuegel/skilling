package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.listener.SkillEventListener;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AreaHarvestMechanicTest {

    @AfterEach
    void tearDown() {
        AreaHarvestMechanic.processingSet().clear();
    }

    private int runHarvest(int radius, int maxBlocks, PluginManager pm, boolean cancel) {
        var world = mock(World.class);
        var origin = mock(Block.class);
        when(origin.getType()).thenReturn(Material.WHEAT);
        when(origin.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        AtomicInteger breakCount = new AtomicInteger();
        when(origin.getRelative(anyInt(), eq(0), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);
            var block = mock(Block.class);
            when(block.getType()).thenReturn(Material.WHEAT);
            when(block.getLocation()).thenReturn(new Location(world, dx, 0, dz));
            doAnswer(inv2 -> {
                breakCount.incrementAndGet();
                return null;
            }).when(block).breakNaturally(any());
            return block;
        });

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(origin);

        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(mock(org.bukkit.inventory.ItemStack.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(pm);
            if (cancel) {
                doAnswer(inv -> {
                    if (inv.getArgument(0) instanceof BlockBreakEvent bbe) {
                        bbe.setCancelled(true);
                    }
                    return null;
                }).when(pm).callEvent(any(BlockBreakEvent.class));
            }
            new AreaHarvestMechanic().execute(player,
                    Map.of("radius", radius, "max_blocks", maxBlocks), event);
        }
        return breakCount.get();
    }

    @Test
    void returnsFalseForNonBlockBreakEvent() {
        var player = mock(Player.class);
        assertFalse(new AreaHarvestMechanic().execute(player, Map.of("radius", 1),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithNonPositiveRadius() {
        var player = mock(Player.class);
        assertFalse(new AreaHarvestMechanic().execute(player, Map.of("radius", 0),
                mock(BlockBreakEvent.class)));
    }

    @Test
    void radiusIsClampedToConfiguredMax() {
        // Radius 100 clamps to 32 -> (2*32+1)^2 - 1 = 4224 candidate blocks, but
        // the break budget is also clamped to MAX_BLOCKS, so the cap binds rather
        // than breaking all 4224 blocks on the main thread.
        int broken = runHarvest(100, 100_000, mock(PluginManager.class), false);
        assertEquals(AreaHarvestMechanic.MAX_BLOCKS, broken);
    }

    @Test
    void maxBlocksIsClampedToConfiguredCap() {
        // A radius-8 disk has (2*8+1)^2 - 1 = 288 candidates; an oversized
        // max_blocks must clamp to MAX_BLOCKS, not break every candidate.
        int broken = runHarvest(8, 100_000, mock(PluginManager.class), false);
        assertEquals(AreaHarvestMechanic.MAX_BLOCKS, broken);
    }

    @Test
    void breakingStopsAtMaxBlocks() {
        // A 5x5 radius (120 candidates) capped at 10 harvested blocks.
        int broken = runHarvest(5, 10, mock(PluginManager.class), false);
        assertEquals(10, broken);
    }

    @Test
    void defaultMaxBlocksIsBoundedNotUnlimited() {
        // Without max_blocks the default of 64 caps a 5x5 radius (120 candidates).
        var world = mock(World.class);
        var origin = mock(Block.class);
        when(origin.getType()).thenReturn(Material.WHEAT);
        when(origin.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        AtomicInteger breakCount = new AtomicInteger();
        when(origin.getRelative(anyInt(), eq(0), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);
            var block = mock(Block.class);
            when(block.getType()).thenReturn(Material.WHEAT);
            when(block.getLocation()).thenReturn(new Location(world, dx, 0, dz));
            doAnswer(inv2 -> {
                breakCount.incrementAndGet();
                return null;
            }).when(block).breakNaturally(any());
            return block;
        });

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(origin);
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(mock(org.bukkit.inventory.ItemStack.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(PluginManager.class));
            new AreaHarvestMechanic().execute(player, Map.of("radius", 5), event);
        }
        assertEquals(64, breakCount.get());
    }

    @Test
    void durabilityConsumedPerHarvestedBlock() {
        var world = mock(World.class);
        var origin = mock(Block.class);
        when(origin.getType()).thenReturn(Material.WHEAT);
        when(origin.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        when(origin.getRelative(anyInt(), eq(0), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);
            var block = mock(Block.class);
            when(block.getType()).thenReturn(Material.WHEAT);
            when(block.getLocation()).thenReturn(new Location(world, dx, 0, dz));
            return block;
        });

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(origin);

        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        var tool = mock(ItemStack.class);
        var material = mock(Material.class);
        when(material.getMaxDurability()).thenReturn((short) 100);
        when(tool.getType()).thenReturn(material);
        var meta = mock(Damageable.class);
        when(meta.getDamage()).thenReturn(5);
        when(tool.getItemMeta()).thenReturn(meta);
        when(inventory.getItemInMainHand()).thenReturn(tool);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(PluginManager.class));
            new AreaHarvestMechanic().execute(player, Map.of("radius", 1), event);
        }

        // A 3x3 harvest minus the origin = 8 additional blocks, one durability
        // point each (the vanilla break already covered the origin block).
        verify(meta, times(8)).setDamage(6);
        verify(inventory, times(8)).setItemInMainHand(tool);
    }

    @Test
    void harvestStopsWhenToolBreaks() {
        var world = mock(World.class);
        var origin = mock(Block.class);
        when(origin.getType()).thenReturn(Material.WHEAT);
        when(origin.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        AtomicInteger breakCount = new AtomicInteger();
        when(origin.getRelative(anyInt(), eq(0), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);
            var block = mock(Block.class);
            when(block.getType()).thenReturn(Material.WHEAT);
            when(block.getLocation()).thenReturn(new Location(world, dx, 0, dz));
            doAnswer(inv2 -> {
                breakCount.incrementAndGet();
                return null;
            }).when(block).breakNaturally(any());
            return block;
        });

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(origin);

        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        var tool = mock(ItemStack.class);
        var material = mock(Material.class);
        when(material.getMaxDurability()).thenReturn((short) 100);
        when(tool.getType()).thenReturn(material);
        var meta = mock(Damageable.class);
        // One point away from breaking: the first harvested block consumes the tool.
        when(meta.getDamage()).thenReturn(99);
        when(tool.getItemMeta()).thenReturn(meta);
        when(inventory.getItemInMainHand()).thenReturn(tool);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(PluginManager.class));
            new AreaHarvestMechanic().execute(player, Map.of("radius", 1), event);
        }

        // The first harvested block breaks, then the tool breaks and the
        // harvest stops: no free drops from a broken tool on remaining blocks.
        assertEquals(1, breakCount.get());
        verify(tool).setAmount(0);
    }

    @Test
    void protectionPluginCancellationIsRespected() {
        // Every synthetic BlockBreakEvent is cancelled -> nothing is broken.
        int broken = runHarvest(3, 100, mock(PluginManager.class), true);
        assertEquals(0, broken);
    }

    @Test
    void harvestedBreakSkipsPipelineDispatch() {
        var block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(mock(World.class), 1, 1, 1));
        AreaHarvestMechanic.processingSet().add(block.getLocation());

        var profileManager = mock(ProfileManager.class);
        var listener = new SkillEventListener(
                mock(Skilling.class), mock(SkillManager.class), profileManager,
                mock(TagResolver.class), mock(RequirementEngine.class), mock(MechanicRegistry.class),
                mock(FeedbackDebouncer.class), mock(BossBarPool.class), mock(StateFilterRegistry.class));

        var event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(mock(Player.class));
        when(event.getBlock()).thenReturn(block);

        listener.onBlockBreak(event);

        verify(profileManager, never()).getProfile(any());
    }
}
