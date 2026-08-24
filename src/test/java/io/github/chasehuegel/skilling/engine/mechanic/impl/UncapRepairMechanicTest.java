package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.view.AnvilView;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UncapRepairMechanicTest {

    @Test
    void returnsFalseForNonAnvilEvent() {
        var player = mock(Player.class);
        assertFalse(new UncapRepairMechanic().execute(player, Map.of("cap", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void raisesMaximumRepairCostAndLiftsEnchantRestriction() {
        var view = mock(AnvilView.class);
        var event = mock(PrepareAnvilEvent.class);
        when(event.getView()).thenReturn(view);
        var player = mock(Player.class);

        assertTrue(new UncapRepairMechanic().execute(player, Map.of("cap", 300.0), event));
        verify(view).setMaximumRepairCost(300);
        verify(view).bypassEnchantmentLevelRestriction(true);
    }

    @Test
    void clampsCapAtTheVanillaFloor() {
        var view = mock(AnvilView.class);
        var event = mock(PrepareAnvilEvent.class);
        when(event.getView()).thenReturn(view);
        var player = mock(Player.class);

        assertTrue(new UncapRepairMechanic().execute(player, Map.of("cap", 5.0), event));
        verify(view).setMaximumRepairCost(40);
        verify(view).bypassEnchantmentLevelRestriction(true);
        verify(view, never()).setMaximumRepairCost(5);
    }

    @Test
    void defaultsToVanillaFloorWhenCapAbsent() {
        var view = mock(AnvilView.class);
        var event = mock(PrepareAnvilEvent.class);
        when(event.getView()).thenReturn(view);
        var player = mock(Player.class);

        assertTrue(new UncapRepairMechanic().execute(player, Map.of(), event));
        verify(view).setMaximumRepairCost(40);
    }
}