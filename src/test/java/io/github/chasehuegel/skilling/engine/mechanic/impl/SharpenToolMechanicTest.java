package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharpenToolMechanicTest {

    private static final UUID STABLE = UUID.fromString("8f1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d");

    private static PlayerInteractEvent rightClick() {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        return event;
    }

    private static Player holding(ItemStack held, PlayerInventory inv) {
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(held);
        return player;
    }

    @Test
    void returnsFalseForNonInteractEvent() {
        var player = mock(Player.class);
        assertFalse(new SharpenToolMechanic().execute(player, Map.of("amount", 2.0, "uuid", STABLE.toString()),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseForLeftClick() {
        var player = mock(Player.class);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        assertFalse(new SharpenToolMechanic().execute(player, Map.of("amount", 2.0, "uuid", STABLE.toString()), event));
    }

    @Test
    void returnsFalseWhenEmptyHandOrNoAmount() {
        var inv = mock(PlayerInventory.class);
        var player = holding(mock(ItemStack.class), inv);
        when(player.getInventory().getItemInMainHand().isEmpty()).thenReturn(true);
        assertFalse(new SharpenToolMechanic().execute(player, Map.of("amount", 2.0, "uuid", STABLE.toString()), rightClick()));
        assertFalse(new SharpenToolMechanic().execute(player, Map.of("amount", 0.0, "uuid", STABLE.toString()), rightClick()));
    }

    @Test
    void appliesMiningEfficiencyModifierWithStableUuid() {
        var held = mock(ItemStack.class);
        when(held.isEmpty()).thenReturn(false);
        ItemMeta meta = mock(ItemMeta.class);
        when(held.getItemMeta()).thenReturn(meta);
        var inv = mock(PlayerInventory.class);
        var player = holding(held, inv);

        assertTrue(new SharpenToolMechanic().execute(player, Map.of("amount", 4.0, "uuid", STABLE.toString()), rightClick()));

        ArgumentCaptor<AttributeModifier> captor = ArgumentCaptor.forClass(AttributeModifier.class);
        verify(meta).addAttributeModifier(org.mockito.ArgumentMatchers.eq(Attribute.MINING_EFFICIENCY), captor.capture());
        AttributeModifier applied = captor.getValue();
        assertTrue(applied.getUniqueId().equals(STABLE));
        assertTrue(applied.getAmount() == 4.0);
        verify(held).setItemMeta(meta);
        verify(inv).setItemInMainHand(held);
    }

    @Test
    void reSharpenReplacesExistingModifierWithSameUuid() {
        var held = mock(ItemStack.class);
        when(held.isEmpty()).thenReturn(false);
        ItemMeta meta = mock(ItemMeta.class);
        when(held.getItemMeta()).thenReturn(meta);
        var existing = new AttributeModifier(STABLE, "skilling_sharpen", 1.0,
                AttributeModifier.Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.ANY);
        when(meta.getAttributeModifiers(Attribute.MINING_EFFICIENCY)).thenReturn(List.of(existing));
        var inv = mock(PlayerInventory.class);
        var player = holding(held, inv);

        assertTrue(new SharpenToolMechanic().execute(player, Map.of("amount", 4.0, "uuid", STABLE.toString()), rightClick()));
        verify(meta).removeAttributeModifier(Attribute.MINING_EFFICIENCY, existing);
    }

    @Test
    void doesNotTouchNonMiningModifiers() {
        var held = mock(ItemStack.class);
        when(held.isEmpty()).thenReturn(false);
        ItemMeta meta = mock(ItemMeta.class);
        when(held.getItemMeta()).thenReturn(meta);
        var unrelated = new AttributeModifier(UUID.randomUUID(), "other", 1.0,
                AttributeModifier.Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.ANY);
        when(meta.getAttributeModifiers(Attribute.MINING_EFFICIENCY)).thenReturn(List.of(unrelated));
        var inv = mock(PlayerInventory.class);
        var player = holding(held, inv);

        assertTrue(new SharpenToolMechanic().execute(player, Map.of("amount", 4.0, "uuid", STABLE.toString()), rightClick()));
        verify(meta, never()).removeAttributeModifier(Attribute.MINING_EFFICIENCY, unrelated);
    }
}