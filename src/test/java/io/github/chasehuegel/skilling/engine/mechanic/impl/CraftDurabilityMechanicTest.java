package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CraftDurabilityMechanicTest {

    @Test
    void returnsFalseForNonCraftEvent() {
        var player = mock(Player.class);
        assertFalse(new CraftDurabilityMechanic().execute(player, Map.of("amount", 25.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWithNoOrNonPositiveAmount() {
        var player = mock(Player.class);
        var result = mock(ItemStack.class);
        when(result.isEmpty()).thenReturn(false);
        var event = mock(CraftItemEvent.class);
        when(event.getCurrentItem()).thenReturn(result);
        assertFalse(new CraftDurabilityMechanic().execute(player, Map.of(), event));
        assertFalse(new CraftDurabilityMechanic().execute(player, Map.of("amount", 0.0), event));
    }

    @Test
    void boostsMaxDurabilityOnCraftedResult() {
        var player = mock(Player.class);
        var result = mock(ItemStack.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.getData(DataComponentTypes.MAX_DAMAGE)).thenReturn(250);
        var event = mock(CraftItemEvent.class);
        when(event.getCurrentItem()).thenReturn(result);

        assertTrue(new CraftDurabilityMechanic().execute(player, Map.of("amount", 25.0), event));
        verify(result).setData(DataComponentTypes.MAX_DAMAGE, 275);
        verify(event).setCurrentItem(result);
    }

    @Test
    void skipsNonDamageableResult() {
        var player = mock(Player.class);
        var result = mock(ItemStack.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.getData(DataComponentTypes.MAX_DAMAGE)).thenReturn(null);
        var mat = mock(org.bukkit.Material.class);
        when(mat.getMaxDurability()).thenReturn((short) 0);
        when(result.getType()).thenReturn(mat);
        var event = mock(CraftItemEvent.class);
        when(event.getCurrentItem()).thenReturn(result);

        assertFalse(new CraftDurabilityMechanic().execute(player, Map.of("amount", 25.0), event));
    }
}