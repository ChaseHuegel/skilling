package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link BlockTransformMechanic} consumes a matching held catalyst and
 * converts the clicked block to the configured result, cancelling the vanilla
 * click. A non-matching catalyst is a no-op that spends nothing.
 */
class BlockTransformMechanicTest {

    private static final Map<String, Object> SEEDS_TO_GRASS = Map.of(
            "catalyst", "minecraft:wheat_seeds",
            "result", "minecraft:grass_block"
    );

    private static PlayerInteractEvent rightClick(Block block) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);
        return event;
    }

    private static Player holding(Material type, int amount) {
        var player = mock(Player.class);
        var inv = mock(PlayerInventory.class);
        var held = mock(ItemStack.class);
        when(held.getType()).thenReturn(type);
        when(held.getAmount()).thenReturn(amount);
        when(inv.getItemInMainHand()).thenReturn(held);
        when(player.getInventory()).thenReturn(inv);
        return player;
    }

    @Test
    void returnsFalseForNonRightClickBlock() {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        assertFalse(new BlockTransformMechanic().execute(mock(Player.class), SEEDS_TO_GRASS, event));
    }

    @Test
    void returnsFalseWhenHeldItemDoesNotMatchCatalyst() {
        var player = holding(Material.GOLD_INGOT, 16);
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIRT);
        var event = rightClick(block);
        assertFalse(new BlockTransformMechanic().execute(player, SEEDS_TO_GRASS, event));
        verify(block, never()).setType(any());
        verify(event, never()).setCancelled(true);
    }

    @Test
    void returnsFalseWhenClickingAir() {
        var player = holding(Material.WHEAT_SEEDS, 8);
        var air = mock(Block.class);
        when(air.getType()).thenReturn(Material.AIR);
        var event = rightClick(air);
        assertFalse(new BlockTransformMechanic().execute(player, SEEDS_TO_GRASS, event));
    }

    @Test
    void consumesCatalystConvertsBlockAndCancels() {
        var player = holding(Material.WHEAT_SEEDS, 8);
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIRT);
        var event = rightClick(block);

        assertTrue(new BlockTransformMechanic().execute(player, SEEDS_TO_GRASS, event));

        var inv = player.getInventory();
        verify(inv.getItemInMainHand()).setAmount(7);
        verify(inv).setItemInMainHand(any(ItemStack.class));
        verify(block).setType(Material.GRASS_BLOCK, false);
        verify(event).setCancelled(true);
    }
}