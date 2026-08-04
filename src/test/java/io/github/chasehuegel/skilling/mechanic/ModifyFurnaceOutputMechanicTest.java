package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyFurnaceOutputMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModifyFurnaceOutputMechanicTest {

    private Player mockPlayer() {
        var player = BukkitMock.mockPlayer();
        when(player.getInventory().addItem(any(ItemStack.class))).thenReturn(new java.util.HashMap<>());
        return player;
    }

    private FurnaceExtractEvent extractEvent(int amount, Material itemType) {
        var event = mock(FurnaceExtractEvent.class);
        when(event.getItemAmount()).thenReturn(amount);
        when(event.getItemType()).thenReturn(itemType);
        return event;
    }

    @Test
    void returnsFalseForNonFurnaceExtractEvent() {
        var mechanic = new ModifyFurnaceOutputMechanic();
        var player = mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void returnsFalseWithMultiplierOne() {
        var mechanic = new ModifyFurnaceOutputMechanic();
        var player = mockPlayer();
        var event = extractEvent(10, Material.IRON_INGOT);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), event));
        verify(player.getInventory(), never()).addItem(any(ItemStack.class));
    }

    @Test
    void computeBonusDerivesTheExtraAmount() {
        assertEquals(0, ModifyFurnaceOutputMechanic.computeBonus(10, 1.0));
        assertEquals(4, ModifyFurnaceOutputMechanic.computeBonus(8, 1.5));
        assertEquals(10, ModifyFurnaceOutputMechanic.computeBonus(10, 2.0));
        // Below 1.0 the bonus is negative; execute()'s bonus > 0 guard skips it.
        assertEquals(-5, ModifyFurnaceOutputMechanic.computeBonus(10, 0.5));
    }

    @Test
    void splitAmountsCapsEveryStackAtMaxStackSize() {
        // 32 extracted * 5x = 128 bonus -> 64 + 64, never an amount > 64.
        assertEquals(List.of(64, 64), ModifyFurnaceOutputMechanic.splitAmounts(64, 128));
        // A bonus within the cap stays a single stack.
        assertEquals(List.of(50), ModifyFurnaceOutputMechanic.splitAmounts(64, 50));
    }

    @Test
    void splitAmountsHandlesNonStackableItems() {
        // maxStackSize 1 -> every bonus item is its own stack.
        assertEquals(List.of(1, 1, 1), ModifyFurnaceOutputMechanic.splitAmounts(1, 3));
    }

    @Test
    void executeNeverAddsAnOversizedStack() {
        var mechanic = new ModifyFurnaceOutputMechanic();
        var player = mockPlayer();
        var event = extractEvent(32, Material.IRON_INGOT);

        // Intercept every ItemStack built by the mechanic so the test can
        // inspect the amounts without the Bukkit ITEM registry (which returns
        // null entries in a plain-JUnit JVM).
        List<Integer> amounts = new ArrayList<>();
        try (var mocked = mockConstruction(ItemStack.class, (stack, context) ->
                amounts.add((int) context.arguments().get(1)))) {
            assertTrue(mechanic.execute(player, Map.of("multiplier", 5.0), event));
            // 32 * 4 = 128 bonus -> two 64-stacks, never one stack > 64.
            assertEquals(2, mocked.constructed().size());
            assertEquals(List.of(64, 64), amounts);
        }
    }
}
