package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyCraftOutputMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModifyCraftOutputMechanicTest {

    private ItemStack resultStack(int amount) {
        var stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(Material.IRON_INGOT);
        when(stack.getAmount()).thenReturn(amount);
        return stack;
    }

    private CraftItemEvent craftEvent(boolean shiftClick, ItemStack current, int perRecipe) {
        var event = mock(CraftItemEvent.class);
        when(event.isShiftClick()).thenReturn(shiftClick);
        when(event.getCurrentItem()).thenReturn(current);
        var recipe = mock(Recipe.class);
        var recipeResult = mock(ItemStack.class);
        when(recipeResult.getAmount()).thenReturn(perRecipe);
        when(recipe.getResult()).thenReturn(recipeResult);
        when(event.getRecipe()).thenReturn(recipe);
        return event;
    }

    @Test
    void returnsFalseForNonCraftEvent() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithMultiplierOne() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), mock(CraftItemEvent.class)));
    }

    @Test
    void normalCraftDoublesTheSingleResult() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        var current = resultStack(1);
        var event = craftEvent(false, current, 1);

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        verify(current).setAmount(2);
    }

    @Test
    void shiftClickBonusIsBasedOnPerRecipeCountNotTheBatchTotal() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        // Batch of 8 (per-recipe result is 1); a multiplier of 2 must add the
        // per-recipe bonus (1), NOT double the whole batch (8 -> would be 16).
        var current = resultStack(8);
        var event = craftEvent(true, current, 1);

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        verify(current).setAmount(9);
    }

    @Test
    void cappedResultStillGrantsTheOverflowToInventory() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        // A 40-stack normal craft doubled would be 80: the result slot caps at
        // 64 and the remaining 16 are granted as an extra stack.
        var current = resultStack(40);
        var event = craftEvent(false, current, 40);
        var extra = mock(ItemStack.class);
        when(current.clone()).thenReturn(extra);
        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.addItem(extra)).thenReturn(new java.util.HashMap<>());

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        verify(current).setAmount(64);
        verify(extra).setAmount(16);
        verify(inv).addItem(extra);
    }

    @Test
    void shiftClickOverflowBeyondMaxStackIsGrantedNotCappedAway() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        // A shift-click batch already at 64 (per-recipe result 1) plus the
        // per-recipe bonus of 1 exceeds one stack: the 1-item overflow is granted.
        var current = resultStack(64);
        var event = craftEvent(true, current, 1);
        var extra = mock(ItemStack.class);
        when(current.clone()).thenReturn(extra);
        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.addItem(extra)).thenReturn(new java.util.HashMap<>());

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        verify(current, never()).setAmount(any(Integer.class));
        verify(extra).setAmount(1);
        verify(inv).addItem(extra);
    }

    @Test
    void overflowIsDroppedWhenInventoryIsFull() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        var current = resultStack(50);
        var event = craftEvent(false, current, 50);
        var extra = mock(ItemStack.class);
        when(current.clone()).thenReturn(extra);
        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.addItem(extra)).thenReturn(new java.util.HashMap<>(Map.of(0, extra)));
        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(mock(Location.class));

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        verify(current).setAmount(64);
        verify(extra).setAmount(36);
        verify(world).dropItemNaturally(any(Location.class), eq(extra));
    }
}
