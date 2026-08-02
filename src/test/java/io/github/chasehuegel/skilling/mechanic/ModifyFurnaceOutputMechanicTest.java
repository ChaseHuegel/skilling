package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyFurnaceOutputMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}
