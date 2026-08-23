package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link TransmuteMechanic} consumes a fixed source batch from the
 * held main-hand stack and grants the product, cancelling the vanilla click.
 */
class TransmuteMechanicTest {

    private static final Map<String, Object> IRON_TO_GOLD = Map.of(
            "source", "minecraft:iron_ingot",
            "product", "minecraft:gold_ingot",
            "source_count", 4.0,
            "product_count", 1.0
    );

    @AfterEach
    void restoreStackFactory() {
        TransmuteMechanic.setStackFactory(ItemStack::new);
    }

    @Test
    void returnsFalseForNonRightClickBlock() {
        var mechanic = new TransmuteMechanic();
        var player = mock(Player.class);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        assertFalse(mechanic.execute(player, IRON_TO_GOLD, event));
    }

    @Test
    void returnsFalseWhenHeldItemDoesNotMatchSource() {
        var mechanic = new TransmuteMechanic();
        var player = mock(Player.class);
        var inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        var held = mockItem(Material.GOLD_INGOT, 16);
        when(inv.getItemInMainHand()).thenReturn(held);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        assertFalse(mechanic.execute(player, IRON_TO_GOLD, event));
        verify(inv, never()).setItemInMainHand(any());
    }

    @Test
    void consumesSourceBatchGrantsProductAndCancels() {
        var mechanic = new TransmuteMechanic();
        var player = mock(Player.class);
        var inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        var held = mockItem(Material.IRON_INGOT, 8);
        when(inv.getItemInMainHand()).thenReturn(held);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        var product = mock(ItemStack.class);
        when(product.getType()).thenReturn(Material.GOLD_INGOT);
        when(product.getAmount()).thenReturn(1);
        TransmuteMechanic.setStackFactory((m, n) -> product);

        assertTrue(mechanic.execute(player, IRON_TO_GOLD, event));

        verify(held).setAmount(4);
        verify(inv).setItemInMainHand(held);
        ArgumentCaptor<ItemStack> captor = ArgumentCaptor.forClass(ItemStack.class);
        verify(inv).addItem(captor.capture());
        assertEquals(Material.GOLD_INGOT, captor.getValue().getType());
        assertEquals(1, captor.getValue().getAmount());
        verify(event).setCancelled(true);
    }

    private static Map<String, Object> onBlock(Map<String, Object> base, String block) {
        Map<String, Object> params = new java.util.HashMap<>(base);
        params.put("block", block);
        return params;
    }

    @Test
    void matchesStationMaterialReferenceTransmutes() {
        var mechanic = new TransmuteMechanic();
        var player = mock(Player.class);
        var inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        var held = mockItem(Material.IRON_INGOT, 8);
        when(inv.getItemInMainHand()).thenReturn(held);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        var station = mock(Block.class);
        when(station.getType()).thenReturn(Material.FURNACE);
        when(event.getClickedBlock()).thenReturn(station);

        var product = mock(ItemStack.class);
        when(product.getType()).thenReturn(Material.GOLD_INGOT);
        when(product.getAmount()).thenReturn(1);
        TransmuteMechanic.setStackFactory((m, n) -> product);

        assertTrue(mechanic.execute(player, onBlock(IRON_TO_GOLD, "minecraft:furnace"), event));
        verify(held).setAmount(4);
        verify(event).setCancelled(true);
    }

    @Test
    void rejectsMismatchedStationWithoutConsuming() {
        var mechanic = new TransmuteMechanic();
        var player = mock(Player.class);
        var inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        var held = mockItem(Material.IRON_INGOT, 8);
        when(inv.getItemInMainHand()).thenReturn(held);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        var station = mock(Block.class);
        when(station.getType()).thenReturn(Material.GRASS_BLOCK);
        when(event.getClickedBlock()).thenReturn(station);

        assertFalse(mechanic.execute(player, onBlock(IRON_TO_GOLD, "minecraft:furnace"), event));
        verify(inv, never()).setItemInMainHand(any());
        verify(event, never()).setCancelled(true);
    }

    @Test
    void rejectsNullStationWhenBlockParamPresent() {
        var mechanic = new TransmuteMechanic();
        var player = mock(Player.class);
        var inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        var held = mockItem(Material.IRON_INGOT, 8);
        when(inv.getItemInMainHand()).thenReturn(held);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        assertFalse(mechanic.execute(player, onBlock(IRON_TO_GOLD, "minecraft:furnace"), event));
        verify(inv, never()).setItemInMainHand(any());
        verify(event, never()).setCancelled(true);
    }

    private static ItemStack mockItem(Material type, int amount) {
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }
}
