package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.testutil.TestEnchantments;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtractEnchantMechanicTest {

    @AfterEach
    void tearDown() {
        ExtractEnchantMechanic.reset();
    }

    private static PlayerInteractEvent interact(Player player, Action action) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        return event;
    }

    private static ItemStack held(Material type, Map<Enchantment, Integer> enchants) {
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getEnchantments()).thenReturn(enchants);
        return item;
    }

    private static Enchantment enchant(String id, int level) {
        return TestEnchantments.fake(id, level, true, false);
    }

    @Test
    void returnsFalseForNonInteractEvent() {
        var player = mock(Player.class);
        assertFalse(new ExtractEnchantMechanic().execute(player, Map.of(),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseForNonRightClick() {
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            var held = held(Material.DIAMOND_SWORD, Map.of(enchant("sharpness", 3), 3));
            when(player.getInventory().getItemInMainHand()).thenReturn(held);
            assertFalse(new ExtractEnchantMechanic().execute(player, Map.of(),
                    interact(player, Action.LEFT_CLICK_BLOCK)));
        }
    }

    @Test
    void returnsFalseForEnchantedBookSource() {
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            var held = held(Material.ENCHANTED_BOOK, Map.of(enchant("sharpness", 3), 3));
            when(player.getInventory().getItemInMainHand()).thenReturn(held);
            assertFalse(new ExtractEnchantMechanic().execute(player, Map.of(),
                    interact(player, Action.RIGHT_CLICK_BLOCK)));
        }
    }

    @Test
    void returnsFalseForUnenchantedHeldItem() {
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        var held = held(Material.DIAMOND_SWORD, new HashMap<>());
        when(player.getInventory().getItemInMainHand()).thenReturn(held);
        assertFalse(new ExtractEnchantMechanic().execute(player, Map.of(),
                interact(player, Action.RIGHT_CLICK_BLOCK)));
    }

    @Test
    void extractsHighestEnchantAndCancelsInteract() {
        var player = mock(Player.class);
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getWorld()).thenReturn(mock(org.bukkit.World.class));
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            var low = enchant("unbreaking", 2);
            var high = enchant("sharpness", 5);
            var enchants = new HashMap<Enchantment, Integer>();
            enchants.put(low, 2);
            enchants.put(high, 5);
            var held = held(Material.DIAMOND_SWORD, enchants);
            when(inventory.getItemInMainHand()).thenReturn(held);
            when(inventory.addItem(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(new java.util.HashMap<Integer, org.bukkit.inventory.ItemStack>());

            var book = mock(ItemStack.class);
            var storage = mock(EnchantmentStorageMeta.class);
            when(book.getItemMeta()).thenReturn(storage);
            ExtractEnchantMechanic.setBookFactory(() -> book);

            var event = interact(player, Action.RIGHT_CLICK_BLOCK);
            assertTrue(new ExtractEnchantMechanic().execute(player, Map.of(), event));

            verify(event).setCancelled(true);
            // The highest-level enchant is stripped off the held item and stored in the book.
            verify(held).removeEnchantment(high);
            verify(storage).addStoredEnchant(high, 5, true);
        }
    }
}
