package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:drop_passengers}: an empty-hand right-click of air ejects
 * every carried mob, while a held item, a non-air interaction, or an empty
 * passenger load is a no-op.
 */
class DropPassengersMechanicTest {

    private static final ItemStack AIR = mock(ItemStack.class);

    static {
        when(AIR.getType()).thenReturn(Material.AIR);
    }

    private static PlayerInteractEvent airClick(Player player) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        return event;
    }

    private static Player emptyHandedPlayer() {
        var inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(AIR);
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(inventory);
        return player;
    }

    @Test
    void returnsFalseForNonInteractEvent() {
        assertFalse(new DropPassengersMechanic().execute(emptyHandedPlayer(), Map.of(),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseForNonAirRightClick() {
        var player = emptyHandedPlayer();
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        assertFalse(new DropPassengersMechanic().execute(player, Map.of(), event));
    }

    @Test
    void returnsFalseWithHeldItem() {
        var inventory = mock(PlayerInventory.class);
        var held = mock(ItemStack.class);
        when(held.getType()).thenReturn(Material.WHEAT);
        when(inventory.getItemInMainHand()).thenReturn(held);
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(inventory);

        assertFalse(new DropPassengersMechanic().execute(player, Map.of(), airClick(player)));
        verify(player, never()).eject();
    }

    @Test
    void returnsFalseWhenCarryingNothing() {
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of());
        assertFalse(new DropPassengersMechanic().execute(player, Map.of(), airClick(player)));
        verify(player, never()).eject();
    }

    @Test
    void ejectsAllCarriedMobsOnEmptyHandAirClick() {
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of(mock(Cow.class), mock(Cow.class)));
        when(player.eject()).thenReturn(true);
        assertTrue(new DropPassengersMechanic().execute(player, Map.of(), airClick(player)));
        verify(player).eject();
    }
}
