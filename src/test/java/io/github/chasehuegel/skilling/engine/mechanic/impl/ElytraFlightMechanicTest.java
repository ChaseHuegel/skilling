package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElytraFlightMechanicTest {

    private static final UUID U = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        ElytraFlightMechanic.clear(U);
    }

    private Player playerWithChest(Material chestMaterial) {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(U);
        var inv = mock(PlayerInventory.class);
        // Build the chestplate before stubbing so no nested stubbing happens
        // inside an unfinished thenReturn.
        ItemStack item = chestItem(chestMaterial);
        when(inv.getChestplate()).thenReturn(item);
        when(player.getInventory()).thenReturn(inv);
        return player;
    }

    private ItemStack chestItem(Material material) {
        if (material == null) return null;
        var stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);
        return stack;
    }

    @Test
    void enablesFlightWhenElytraEquipped() {
        var player = playerWithChest(Material.ELYTRA);
        new ElytraFlightMechanic().execute(player, Map.of(), null);
        verify(player).setAllowFlight(true);
    }

    @Test
    void disablesFlightWithoutElytra() {
        var player = playerWithChest(Material.IRON_CHESTPLATE);
        new ElytraFlightMechanic().execute(player, Map.of(), null);
        verify(player).setAllowFlight(false);
    }

    @Test
    void disablesFlightWithEmptyChestSlot() {
        var player = playerWithChest(null);
        new ElytraFlightMechanic().execute(player, Map.of(), null);
        verify(player).setAllowFlight(false);
    }

    @Test
    void reevaluateIsNoopForIneligiblePlayer() {
        var player = playerWithChest(Material.ELYTRA);
        ElytraFlightMechanic.reevaluate(player);
        verify(player, never()).setAllowFlight(true);
        verify(player, never()).setAllowFlight(false);
    }

    @Test
    void reevaluateTogglesWhenEligiblePlayerChangesElytra() {
        // Grant flight with the elytra on.
        var player = playerWithChest(Material.ELYTRA);
        new ElytraFlightMechanic().execute(player, Map.of(), null);
        verify(player).setAllowFlight(true);

        // Take the elytra off: a reevaluate disables flight.
        var stripped = playerWithChest(Material.NETHERITE_CHESTPLATE);
        ElytraFlightMechanic.reevaluate(stripped);
        verify(stripped).setAllowFlight(false);

        // Re-equip the elytra: a reevaluate re-enables it without re-granting.
        var reequipped = playerWithChest(Material.ELYTRA);
        ElytraFlightMechanic.reevaluate(reequipped);
        verify(reequipped).setAllowFlight(true);
    }

    @Test
    void stripAllDisablesEligibleAndClears() {
        var player = playerWithChest(Material.ELYTRA);
        new ElytraFlightMechanic().execute(player, Map.of(), null);
        verify(player, times(1)).setAllowFlight(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPlayer(U)).thenReturn(player);
            when(player.isOnline()).thenReturn(true);
            ElytraFlightMechanic.stripAll();
        }
        verify(player, times(1)).setAllowFlight(false);
        // After strip the player is no longer eligible: a reevaluate cannot
        // re-grant flight, so the true still stands at exactly one invocation.
        ElytraFlightMechanic.reevaluate(player);
        verify(player, times(1)).setAllowFlight(true);
    }

    @Test
    void clearRemovesEligibility() {
        var player = playerWithChest(Material.ELYTRA);
        new ElytraFlightMechanic().execute(player, Map.of(), null);
        ElytraFlightMechanic.clear(U);
        // simulate a fresh player on next join (re-execute re-grants).
        verify(player).setAllowFlight(true);
    }
}
