package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
 * Verifies {@code core:pick_up_mob}: an empty-hand right-click mounts the
 * clicked living mob onto the player, capped by {@code max_passengers}, while
 * a non-living or player target, a held item, or a full passenger load is a
 * no-op that spends no cost or cooldown.
 */
class PickUpMobMechanicTest {

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

    private static PlayerInteractEntityEvent clickOn(Player player, org.bukkit.entity.Entity target) {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(target);
        return event;
    }

    @Test
    void returnsFalseForNonEntityInteractEvent() {
        assertFalse(new PickUpMobMechanic().execute(emptyHandedPlayer(), Map.of(),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithHeldItem() {
        var inventory = mock(PlayerInventory.class);
        var held = mock(ItemStack.class);
        when(held.getType()).thenReturn(Material.WHEAT);
        when(inventory.getItemInMainHand()).thenReturn(held);
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(inventory);
        var event = clickOn(player, mock(Cow.class));

        assertFalse(new PickUpMobMechanic().execute(player, Map.of(), event));
        verify(player, never()).addPassenger(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsFalseForNonLivingTarget() {
        var player = emptyHandedPlayer();
        var event = clickOn(player, mock(org.bukkit.entity.ItemFrame.class));

        assertFalse(new PickUpMobMechanic().execute(player, Map.of(), event));
        verify(player, never()).addPassenger(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsFalseForPlayerTarget() {
        var player = emptyHandedPlayer();
        var event = clickOn(player, mock(Player.class));

        assertFalse(new PickUpMobMechanic().execute(player, Map.of(), event));
        verify(player, never()).addPassenger(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsFalseWithNonPositiveMaxPassengers() {
        var player = emptyHandedPlayer();
        var event = clickOn(player, mock(Cow.class));

        assertFalse(new PickUpMobMechanic().execute(player, Map.of("max_passengers", 0.0), event));
    }

    @Test
    void returnsFalseWhenAtPassengerCapacity() {
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of(mock(Cow.class), mock(Cow.class)));
        var event = clickOn(player, mock(Cow.class));

        assertFalse(new PickUpMobMechanic().execute(player, Map.of("max_passengers", 2.0), event));
        verify(player, never()).addPassenger(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void carriesLivingMobOnRightClick() {
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of());
        var target = mock(Villager.class);
        when(player.addPassenger(target)).thenReturn(true);
        var event = clickOn(player, target);

        assertTrue(new PickUpMobMechanic().execute(player, Map.of("max_passengers", 3.0), event));
        verify(player).addPassenger(target);
    }

    @Test
    void defaultsToSinglePassenger() {
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of());
        var target = mock(Cow.class);
        when(player.addPassenger(target)).thenReturn(true);
        var event = clickOn(player, target);

        assertTrue(new PickUpMobMechanic().execute(player, Map.of(), event));
        verify(player).addPassenger(target);
    }

    @Test
    void failedMountIsNoOp() {
        var player = emptyHandedPlayer();
        when(player.getPassengers()).thenReturn(List.of());
        var target = mock(Cow.class);
        when(player.addPassenger(target)).thenReturn(false);
        var event = clickOn(player, target);

        assertFalse(new PickUpMobMechanic().execute(player, Map.of(), event),
                "an already-mounted target must not spend cost or cooldown");
    }
}
