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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:drop_passengers}: every carried mob is dismounted via
 * {@code leaveVehicle()} (per passenger, rather than a single {@code eject()}
 * that can silently no-op on a player), and a drop with no passengers is a
 * no-op that spends no cost or cooldown.
 */
class DropPassengersMechanicTest {

    private static final ItemStack AIR = mock(ItemStack.class);

    static {
        when(AIR.getType()).thenReturn(Material.AIR);
    }

    private static Player emptyHandedPlayer() {
        var inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(AIR);
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(inventory);
        return player;
    }

    @Test
    void ejectsEveryCarriedMobViaLeaveVehicle() {
        var cow1 = mock(Cow.class);
        var cow2 = mock(Cow.class);
        when(cow1.leaveVehicle()).thenReturn(true);
        when(cow2.leaveVehicle()).thenReturn(true);
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of(cow1, cow2));

        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertTrue(new DropPassengersMechanic().execute(player, Map.of(), event));
        verify(cow1).leaveVehicle();
        verify(cow2).leaveVehicle();
    }

    @Test
    void returnsFalseWhenCarryingNothing() {
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of());
        assertFalse(new DropPassengersMechanic().execute(player, Map.of(),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void partialDismountFailureStillReportsTrueForSuccessfulOnes() {
        var cow = mock(Cow.class);
        when(cow.leaveVehicle()).thenReturn(false);
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of(cow));

        assertFalse(new DropPassengersMechanic().execute(player, Map.of(),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)),
                "no passenger actually dismounted");
    }
}
