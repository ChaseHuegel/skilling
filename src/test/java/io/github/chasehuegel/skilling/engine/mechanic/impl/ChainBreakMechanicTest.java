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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChainBreakMechanicTest {

    @AfterEach
    void tearDown() {
        ChainBreakMechanic.processingSet().clear();
    }

    private Block block(World world, int x, int y, int z, Material material) {
        var block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(world, x, y, z));
        when(block.getType()).thenReturn(material);
        return block;
    }

    @Test
    void returnsFalseForNonBlockBreakEvent() {
        var player = mock(Player.class);
        assertFalse(new ChainBreakMechanic().execute(player, Map.of("chain_limit", 10),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithChainLimitZero() {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        assertFalse(new ChainBreakMechanic().execute(player, Map.of(),
                mock(BlockBreakEvent.class)));
    }

    @Test
    void durabilityConsumedOncePerChainedBlock() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.STONE);
        var n1 = block(world, 1, 0, 0, Material.STONE);
        var n2 = block(world, -1, 0, 0, Material.STONE);
        var air = block(world, 99, 99, 99, Material.AIR);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(n1.getLocation(), n1);
        neighbors.put(n2.getLocation(), n2);
        when(origin.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0), dy = inv.getArgument(1), dz = inv.getArgument(2);
            Location loc = new Location(world, dx, dy, dz);
            return neighbors.getOrDefault(loc, air);
        });
        when(n1.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(n2.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(origin);

        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        var tool = mock(ItemStack.class);
        var toolMaterial = mock(Material.class);
        when(toolMaterial.getMaxDurability()).thenReturn((short) 100);
        when(tool.getType()).thenReturn(toolMaterial);
        var meta = mock(Damageable.class);
        when(meta.getDamage()).thenReturn(5);
        when(tool.getItemMeta()).thenReturn(meta);
        when(inv.getItemInMainHand()).thenReturn(tool);

        var pluginManager = mock(org.bukkit.plugin.PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(pluginManager);

            new ChainBreakMechanic().execute(player, Map.of("chain_limit", 2), event);
        }

        // Two chained blocks were broken; the tool lost 1 durability each.
        verify(n1).breakNaturally(tool);
        verify(n2).breakNaturally(tool);
        verify(meta, times(2)).setDamage(6);
        verify(inv, times(2)).setItemInMainHand(tool);
    }

    @Test
    void isChainProcessingFalseOutsideChain() {
        var player = mock(Player.class);
        var world = mock(World.class);
        var block = block(world, 0, 0, 0, Material.STONE);
        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);
        assertFalse(ChainBreakMechanic.isChainProcessing(block));
    }

    @Test
    void chainedBreakSkipsPipelineDispatch() {
        var block = block(mock(World.class), 5, 5, 5, Material.STONE);

        var profileManager = mock(ProfileManager.class);
        var listener = new SkillEventListener(
                mock(Skilling.class), mock(SkillManager.class), profileManager,
                mock(TagResolver.class), mock(RequirementEngine.class), mock(MechanicRegistry.class),
                mock(FeedbackDebouncer.class), mock(BossBarPool.class), mock(StateFilterRegistry.class));

        var event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(mock(Player.class));
        when(event.getBlock()).thenReturn(block);

        // Mark the block as mid-chain-break immediately before the listener call
        // so no intervening allocation can disturb the shared processing set.
        ChainBreakMechanic.processingSet().add(block.getLocation());
        listener.onBlockBreak(event);

        // dispatch() would look up the profile; a chained break must not reach it.
        verify(profileManager, never()).getProfile(any());
    }
}
