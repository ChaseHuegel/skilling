package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the shared tool-durability cost used by chain/harvest mechanics:
 * one point per additional block, Unbreaking interaction, plugin veto via
 * {@link PlayerItemDamageEvent}, and breaking at max durability.
 */
class ToolDurabilityTest {

    @AfterEach
    void tearDown() {
        ToolDurability.reset();
    }

    private ItemStack mockTool(int damage, int maxDurability) {
        var tool = mock(ItemStack.class);
        var material = mock(Material.class);
        when(material.getMaxDurability()).thenReturn((short) maxDurability);
        when(tool.getType()).thenReturn(material);
        var meta = mock(Damageable.class);
        when(meta.getDamage()).thenReturn(damage);
        when(tool.getItemMeta()).thenReturn(meta);
        return tool;
    }

    private Player playerWithTool(ItemStack tool) {
        var player = mock(Player.class);
        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(tool);
        return player;
    }

    private void withPluginManager(Runnable body) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(PluginManager.class));
            body.run();
        }
    }

    @Test
    void appliesOneDurabilityPoint() {
        var tool = mockTool(5, 100);
        var player = playerWithTool(tool);

        withPluginManager(() -> ToolDurability.damageOnce(player, tool));

        verify((Damageable) tool.getItemMeta()).setDamage(6);
        verify(player.getInventory()).setItemInMainHand(tool);
    }

    @Test
    void unbreakingSkipsDamageWhenRollFails() {
        ToolDurability.setUnbreakingReader(tool -> 3);
        ToolDurability.setRandomSource(() -> 0.99);
        var tool = mockTool(5, 100);
        var player = playerWithTool(tool);

        withPluginManager(() -> ToolDurability.damageOnce(player, tool));

        verify((Damageable) tool.getItemMeta(), never()).setDamage(anyInt());
        verify(player.getInventory(), never()).setItemInMainHand(tool);
    }

    @Test
    void unbreakingConsumesWhenRollSucceeds() {
        ToolDurability.setUnbreakingReader(tool -> 3);
        ToolDurability.setRandomSource(() -> 0.1);
        var tool = mockTool(5, 100);
        var player = playerWithTool(tool);

        withPluginManager(() -> ToolDurability.damageOnce(player, tool));

        verify((Damageable) tool.getItemMeta()).setDamage(6);
    }

    @Test
    void noUnbreakingAlwaysConsumesRegardlessOfRoll() {
        ToolDurability.setRandomSource(() -> 0.99);
        var tool = mockTool(5, 100);
        var player = playerWithTool(tool);

        withPluginManager(() -> ToolDurability.damageOnce(player, tool));

        verify((Damageable) tool.getItemMeta()).setDamage(6);
    }

    @Test
    void cancelledDamageEventVetoesDurability() {
        var tool = mockTool(5, 100);
        var player = playerWithTool(tool);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            var pm = mock(PluginManager.class);
            when(Bukkit.getPluginManager()).thenReturn(pm);
            doAnswer(inv -> {
                ((PlayerItemDamageEvent) inv.getArgument(0)).setCancelled(true);
                return null;
            }).when(pm).callEvent(any(PlayerItemDamageEvent.class));

            ToolDurability.damageOnce(player, tool);
        }

        verify((Damageable) tool.getItemMeta(), never()).setDamage(anyInt());
        verify(player.getInventory(), never()).setItemInMainHand(tool);
    }

    @Test
    void toolBreaksAtMaxDurabilityInsteadOfExceeding() {
        var tool = mockTool(99, 100);
        var player = playerWithTool(tool);

        withPluginManager(() -> ToolDurability.damageOnce(player, tool));

        verify(tool).setAmount(0);
        verify((Damageable) tool.getItemMeta(), never()).setDamage(anyInt());
        verify(player.getInventory()).setItemInMainHand(tool);
    }

    @Test
    void damageOnceReportsBreakAndSurvival() {
        var breaking = mockTool(99, 100);
        withPluginManager(() -> assertTrue(ToolDurability.damageOnce(playerWithTool(breaking), breaking)));

        var surviving = mockTool(5, 100);
        withPluginManager(() -> assertFalse(ToolDurability.damageOnce(playerWithTool(surviving), surviving)));
    }

    @Test
    void offhandSlotDamageWritesBackToOffHandSlot() {
        var tool = mockTool(5, 100);
        var player = playerWithTool(tool);

        withPluginManager(() -> ToolDurability.damageOnce(player, tool, org.bukkit.inventory.EquipmentSlot.OFF_HAND));

        verify((Damageable) tool.getItemMeta()).setDamage(6);
        verify(player.getInventory()).setItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND, tool);
    }

    @Test
    void airToolIsIgnored() {
        var tool = mock(ItemStack.class);
        when(tool.getType()).thenReturn(Material.AIR);
        var player = playerWithTool(tool);

        withPluginManager(() -> ToolDurability.damageOnce(player, tool));

        verify(player.getInventory(), never()).setItemInMainHand(tool);
    }
}
