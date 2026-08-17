package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:reroll_trades}: right-clicking a villager cycles its
 * profession through {@code NONE} and back to regenerate its offers, while a
 * non-villager target, a non-entity-interact event, or an already-jobless
 * villager is left untouched.
 */
class RerollTradesMechanicTest {

    @Test
    void rightClickVillagerCyclesProfession() {
        var villager = mock(Villager.class);
        when(villager.getProfession()).thenReturn(Villager.Profession.FARMER);
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(villager);

        assertTrue(new RerollTradesMechanic().execute(mock(Player.class), Map.of(), event));
        verify(villager).setProfession(Villager.Profession.NONE);
        verify(villager).setProfession(Villager.Profession.FARMER);
    }

    @Test
    void nonVillagerTargetIsNoOp() {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(mock(org.bukkit.entity.Cow.class));
        assertFalse(new RerollTradesMechanic().execute(mock(Player.class), Map.of(), event));
    }

    @Test
    void joblessVillagerIsLeftUntouched() {
        var villager = mock(Villager.class);
        when(villager.getProfession()).thenReturn(Villager.Profession.NONE);
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(villager);

        assertTrue(new RerollTradesMechanic().execute(mock(Player.class), Map.of(), event));
        verify(villager, never()).setProfession(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonEntityInteractEventIsNoOp() {
        assertFalse(new RerollTradesMechanic().execute(mock(Player.class), Map.of(),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }
}
