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
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
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

    /**
     * Runs a chain break with the given neighbor map, returning the blocks that
     * were actually broken. Mocks the plugin instance so the mechanic can resolve
     * the optional {@code target} reference through the given tag resolver.
     */
    private java.util.List<Block> runChain(World world, Map<Location, Block> neighbors, Material originMaterial,
            TagResolver tagResolver, Map<String, Object> params) {
        var origin = block(world, 0, 0, 0, originMaterial);
        var air = block(world, 99, 99, 99, Material.AIR);
        when(origin.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0), dy = inv.getArgument(1), dz = inv.getArgument(2);
            Location loc = new Location(world, dx, dy, dz);
            return neighbors.getOrDefault(loc, air);
        });
        java.util.List<Block> broken = new java.util.ArrayList<>();
        for (var b : neighbors.values()) {
            when(b.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);
            doAnswer(inv -> {
                broken.add(b);
                return true;
            }).when(b).breakNaturally(any(ItemStack.class));
        }

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
        when(meta.getDamage()).thenReturn(0);
        when(tool.getItemMeta()).thenReturn(meta);
        when(inv.getItemInMainHand()).thenReturn(tool);

        var plugin = mock(Skilling.class);
        when(plugin.getTagResolver()).thenReturn(tagResolver);
        var pluginManager = mock(org.bukkit.plugin.PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            when(Bukkit.getPluginManager()).thenReturn(pluginManager);
            when(Skilling.getInstance()).thenReturn(plugin);
            new ChainBreakMechanic().execute(player, params, event);
        }
        return broken;
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
    void oversizedChainLimitIsClampedToCap() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.STONE);
        // A long linear chain far beyond the cap; the budget must bind.
        Map<Location, Block> chain = new HashMap<>();
        for (int i = 1; i <= 200; i++) {
            chain.put(new Location(world, i, 0, 0), block(world, i, 0, 0, Material.STONE));
        }
        var air = block(world, 999, 999, 999, Material.AIR);
        java.util.function.BiFunction<Block, int[], Block> neighbor = (b, dir) ->
                chain.getOrDefault(new Location(world,
                        b.getLocation().getBlockX() + dir[0],
                        b.getLocation().getBlockY() + dir[1],
                        b.getLocation().getBlockZ() + dir[2]), air);
        when(origin.getRelative(anyInt(), anyInt(), anyInt()))
                .thenAnswer(inv -> neighbor.apply(origin, new int[]{
                        inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)}));
        for (var b : chain.values()) {
            Block current = b;
            when(b.getRelative(anyInt(), anyInt(), anyInt()))
                    .thenAnswer(inv -> neighbor.apply(current, new int[]{
                            inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)}));
        }

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(origin);

        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        var tool = mock(ItemStack.class);
        var toolMaterial = mock(Material.class);
        when(toolMaterial.getMaxDurability()).thenReturn((short) 10_000);
        when(tool.getType()).thenReturn(toolMaterial);
        var meta = mock(Damageable.class);
        when(meta.getDamage()).thenReturn(0);
        when(tool.getItemMeta()).thenReturn(meta);
        when(inv.getItemInMainHand()).thenReturn(tool);

        AtomicInteger broken = new AtomicInteger();
        for (var b : chain.values()) {
            doAnswer(inv2 -> {
                broken.incrementAndGet();
                return true;
            }).when(b).breakNaturally(any());
        }

        var pluginManager = mock(org.bukkit.plugin.PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(pluginManager);
            new ChainBreakMechanic().execute(player, Map.of("chain_limit", 100_000), event);
        }

        // chain_limit excludes the origin, so the mechanic breaks cap chained blocks.
        assertEquals(ChainBreakMechanic.MAX_CHAIN_LIMIT, broken.get(),
                "an oversized chain_limit must be clamped, not executed raw");
    }

    @Test
    void chainLimitExcludesOriginSoChainsUpToLimit() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.STONE);
        var n1 = block(world, 1, 0, 0, Material.STONE);
        var n2 = block(world, -1, 0, 0, Material.STONE);
        var n3 = block(world, 0, 1, 0, Material.STONE);
        var air = block(world, 99, 99, 99, Material.AIR);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(n1.getLocation(), n1);
        neighbors.put(n2.getLocation(), n2);
        neighbors.put(n3.getLocation(), n3);
        when(origin.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0), dy = inv.getArgument(1), dz = inv.getArgument(2);
            Location loc = new Location(world, dx, dy, dz);
            return neighbors.getOrDefault(loc, air);
        });
        when(n1.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(n2.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(n3.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);

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
        when(n1.breakNaturally(tool)).thenReturn(true);
        when(n2.breakNaturally(tool)).thenReturn(true);
        when(n3.breakNaturally(tool)).thenReturn(true);

        var pluginManager = mock(org.bukkit.plugin.PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(pluginManager);

            // chain_limit excludes the origin, so all 3 chained blocks break.
            new ChainBreakMechanic().execute(player, Map.of("chain_limit", 3), event);
        }

        verify(n1).breakNaturally(tool);
        verify(n2).breakNaturally(tool);
        verify(n3).breakNaturally(tool);
        // Three chained blocks were broken; the tool lost 1 durability each.
        verify(meta, times(3)).setDamage(6);
        verify(inv, times(3)).setItemInMainHand(tool);
    }

    @Test
    void failedChainBreakConsumesNoLimitOrDurability() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.STONE);
        var n1 = block(world, 1, 0, 0, Material.STONE);
        var air = block(world, 99, 99, 99, Material.AIR);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(n1.getLocation(), n1);
        when(origin.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0), dy = inv.getArgument(1), dz = inv.getArgument(2);
            Location loc = new Location(world, dx, dy, dz);
            return neighbors.getOrDefault(loc, air);
        });
        when(n1.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(air);

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
        // The only chained block cannot be broken (e.g. unbreakable type).
        when(n1.breakNaturally(tool)).thenReturn(false);

        var pluginManager = mock(org.bukkit.plugin.PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(pluginManager);

            new ChainBreakMechanic().execute(player, Map.of("chain_limit", 10), event);
        }

        verify(n1).breakNaturally(tool);
        verify(meta, never()).setDamage(anyInt());
        verify(inv, never()).setItemInMainHand(tool);
    }

    @Test
    void noTargetChainsToOriginMaterialOnly() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.STONE);
        var sameMaterial = block(world, 1, 0, 0, Material.STONE);
        var differentMaterial = block(world, -1, 0, 0, Material.DIRT);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(sameMaterial.getLocation(), sameMaterial);
        neighbors.put(differentMaterial.getLocation(), differentMaterial);

        var resolver = mock(TagResolver.class);
        var broken = runChain(world, neighbors, Material.STONE, resolver, Map.of("chain_limit", 10));

        // Without a target the chain is limited to the origin's own material;
        // the DIRT neighbor must not break.
        assertTrue(broken.contains(sameMaterial), "same-material neighbor must chain without a target");
        assertFalse(broken.contains(differentMaterial), "different-material neighbor must not chain without a target");
    }

    @Test
    void targetSingleMaterialChainsOnlyToThatMaterial() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.STONE);
        var targetMaterial = block(world, 1, 0, 0, Material.DIRT);
        var otherMaterial = block(world, -1, 0, 0, Material.STONE);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(targetMaterial.getLocation(), targetMaterial);
        neighbors.put(otherMaterial.getLocation(), otherMaterial);

        var resolver = mock(TagResolver.class);
        when(resolver.resolve("minecraft:dirt")).thenReturn(EnumSet.of(Material.DIRT));
        var broken = runChain(world, neighbors, Material.STONE, resolver,
                Map.of("chain_limit", 10, "target", "minecraft:dirt"));

        assertTrue(broken.contains(targetMaterial), "target material must chain");
        assertFalse(broken.contains(otherMaterial), "non-target material must not chain");
        verify(resolver).resolve("minecraft:dirt");
    }

    @Test
    void targetVanillaTagChainsToEveryMemberMaterial() {
        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.OAK_LOG);
        var memberMaterial = block(world, 1, 0, 0, Material.BIRCH_LOG);
        var nonMember = block(world, -1, 0, 0, Material.STONE);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(memberMaterial.getLocation(), memberMaterial);
        neighbors.put(nonMember.getLocation(), nonMember);

        var resolver = mock(TagResolver.class);
        when(resolver.resolve("#minecraft:logs")).thenReturn(EnumSet.of(Material.OAK_LOG, Material.BIRCH_LOG));
        var broken = runChain(world, neighbors, Material.OAK_LOG, resolver,
                Map.of("chain_limit", 10, "target", "#minecraft:logs"));

        assertTrue(broken.contains(memberMaterial), "member of the target tag must chain");
        assertFalse(broken.contains(nonMember), "non-member must not chain");
        verify(resolver).resolve("#minecraft:logs");
    }

    @Test
    void targetCustomTagChainsToEveryMemberMaterial() throws Exception {
        Path tagsFile = Files.createTempFile("tags", ".yml");
        Files.writeString(tagsFile, "custom_tags:\n  logs:\n    - \"minecraft:oak_log\"\n    - \"minecraft:birch_log\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        var resolver = new TagResolver(loader);

        var world = mock(World.class);
        var origin = block(world, 0, 0, 0, Material.OAK_LOG);
        var memberMaterial = block(world, 1, 0, 0, Material.BIRCH_LOG);
        var nonMember = block(world, -1, 0, 0, Material.STONE);

        Map<Location, Block> neighbors = new HashMap<>();
        neighbors.put(memberMaterial.getLocation(), memberMaterial);
        neighbors.put(nonMember.getLocation(), nonMember);

        var broken = runChain(world, neighbors, Material.OAK_LOG, resolver,
                Map.of("chain_limit", 10, "target", "#c:logs"));

        assertTrue(broken.contains(memberMaterial), "member of the custom tag must chain");
        assertFalse(broken.contains(nonMember), "non-member must not chain");
    }


    @Test
    void chainStopsWhenToolBreaks() {
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
        // One point away from breaking: the first chained block consumes the tool.
        when(meta.getDamage()).thenReturn(99);
        when(tool.getItemMeta()).thenReturn(meta);
        when(inv.getItemInMainHand()).thenReturn(tool);
        when(n1.breakNaturally(tool)).thenReturn(true);
        when(n2.breakNaturally(tool)).thenReturn(true);

        var pluginManager = mock(org.bukkit.plugin.PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(pluginManager);

            new ChainBreakMechanic().execute(player, Map.of("chain_limit", 10), event);
        }

        // The first chained block breaks, then the tool breaks and the chain
        // stops: no free drops from a broken tool on the second neighbor.
        verify(n1).breakNaturally(tool);
        verify(n2, never()).breakNaturally(tool);
        verify(tool).setAmount(0);
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
