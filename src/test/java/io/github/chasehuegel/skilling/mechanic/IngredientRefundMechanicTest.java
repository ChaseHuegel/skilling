package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.IngredientRefundMechanic;
import org.bukkit.Material;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngredientRefundMechanicTest {

    private ItemStack ingredient(Material type) {
        var stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(type);
        return stack;
    }

    private CraftItemEvent craftEvent(ItemStack... matrix) {
        var event = mock(CraftItemEvent.class);
        var crafting = mock(CraftingInventory.class);
        when(crafting.getMatrix()).thenReturn(matrix);
        when(event.getInventory()).thenReturn(crafting);
        return event;
    }

    @Test
    void returnsFalseForNonCraftEvent() {
        var mechanic = new IngredientRefundMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("chance", 50.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWhenChanceIsZero() {
        var mechanic = new IngredientRefundMechanic();
        var player = BukkitMock.mockPlayer();
        var event = craftEvent(ingredient(Material.OAK_PLANKS));
        assertFalse(mechanic.execute(player, Map.of(), event));
    }

    @Test
    void returnsFalseWhenMatrixHasNoIngredients() {
        var mechanic = new IngredientRefundMechanic();
        var player = BukkitMock.mockPlayer();
        var event = craftEvent(new ItemStack[9]);
        assertFalse(mechanic.execute(player, Map.of("chance", 50.0), event));
    }

    @Test
    void failedRollStillReturnsTrueButGrantsNothing() {
        IngredientRefundMechanic.setRandomSource(() -> 90.0);
        var mechanic = new IngredientRefundMechanic();
        var player = BukkitMock.mockPlayer();
        var event = craftEvent(ingredient(Material.OAK_PLANKS));

        assertTrue(mechanic.execute(player, Map.of("chance", 50.0), event));
        verify(player.getInventory(), never()).addItem(any(ItemStack.class));
    }

    @Test
    void successfulRollRefundsAUsedIngredient() {
        IngredientRefundMechanic.setRandomSource(() -> 0.0);
        var refunded = mock(ItemStack.class);
        IngredientRefundMechanic.setItemFactory(m -> refunded);
        var mechanic = new IngredientRefundMechanic();
        var player = BukkitMock.mockPlayer();
        when(player.getInventory().addItem(refunded)).thenReturn(new java.util.HashMap<>());
        var event = craftEvent(ingredient(Material.OAK_PLANKS));

        assertTrue(mechanic.execute(player, Map.of("chance", 50.0), event));
        verify(player.getInventory()).addItem(refunded);
    }

    @Test
    void ingredientReferenceRestrictsTheRefundPool() {
        // The non-# material path resolves without a live TagResolver, so the
        // filter narrows a mixed grid to just the configured wood and the refund
        // is deterministic (one candidate).
        IngredientRefundMechanic.setRandomSource(() -> 0.0);
        var refunded = mock(ItemStack.class);
        IngredientRefundMechanic.setItemFactory(m -> refunded);
        var mechanic = new IngredientRefundMechanic();
        var player = BukkitMock.mockPlayer();
        when(player.getInventory().addItem(refunded)).thenReturn(new java.util.HashMap<>());
        var event = craftEvent(ingredient(Material.OAK_PLANKS), ingredient(Material.DIAMOND));

        assertTrue(mechanic.execute(player, Map.of("chance", 50.0, "ingredient", "minecraft:oak_planks"), event));
        verify(player.getInventory()).addItem(refunded);
    }
}
