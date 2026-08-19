package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link ModifyBrewOutputMechanic} adds a bonus bottle to a free stand
 * slot on a successful chance roll and never loses it when the batch is full.
 */
class ModifyBrewOutputMechanicTest {

    @AfterEach
    void restoreRandom() {
        ModifyBrewOutputMechanic.setRandomSource(() -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(100));
    }

    @Test
    void returnsFalseForNonBrewEvent() {
        var mechanic = new ModifyBrewOutputMechanic();
        assertFalse(mechanic.execute(mock(Player.class), Map.of("chance", 100.0), mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void placesBonusBottleInFreeSlot() {
        ModifyBrewOutputMechanic.setRandomSource(() -> 0.0); // roll succeeds
        var mechanic = new ModifyBrewOutputMechanic();
        var player = mock(Player.class);

        var brewed = mock(ItemStack.class);
        when(brewed.getType()).thenReturn(Material.POTION);
        var bonusClone = mock(ItemStack.class);
        when(brewed.clone()).thenReturn(bonusClone);

        var inv = mock(BrewerInventory.class);
        when(inv.getItem(0)).thenReturn(brewed);
        when(inv.getItem(1)).thenReturn(null);
        when(inv.getItem(2)).thenReturn(null);

        var event = mock(BrewEvent.class);
        when(event.getContents()).thenReturn(inv);

        assertTrue(mechanic.execute(player, Map.of("chance", 100.0), event));
        verify(inv).setItem(eq(1), eq(bonusClone));
    }

    @Test
    void grantsToPlayerWhenBatchFull() {
        ModifyBrewOutputMechanic.setRandomSource(() -> 0.0);
        var mechanic = new ModifyBrewOutputMechanic();
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(mock(org.bukkit.inventory.PlayerInventory.class));

        var brewed = mock(ItemStack.class);
        when(brewed.getType()).thenReturn(Material.POTION);
        var bonusClone = mock(ItemStack.class);
        when(brewed.clone()).thenReturn(bonusClone);

        var inv = mock(BrewerInventory.class);
        when(inv.getItem(0)).thenReturn(brewed);
        when(inv.getItem(1)).thenReturn(brewed);
        when(inv.getItem(2)).thenReturn(brewed);

        var event = mock(BrewEvent.class);
        when(event.getContents()).thenReturn(inv);

        assertTrue(mechanic.execute(player, Map.of("chance", 100.0), event));
        verify(inv, never()).setItem(eq(0), any(ItemStack.class));
        verify(inv, never()).setItem(eq(1), any(ItemStack.class));
        verify(inv, never()).setItem(eq(2), any(ItemStack.class));
        verify(player.getInventory()).addItem(eq(bonusClone));
    }
}
