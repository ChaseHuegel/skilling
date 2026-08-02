package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyCraftOutputMechanic;
import org.bukkit.Material;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
    void outputIsCappedAtMaxStackSize() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        // A 40-stack normal craft doubled would be 80; it must cap at 64.
        var current = resultStack(40);
        var event = craftEvent(false, current, 40);

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        verify(current).setAmount(64);
    }
}
