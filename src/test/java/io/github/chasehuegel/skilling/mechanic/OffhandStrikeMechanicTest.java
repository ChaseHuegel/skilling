package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.OffhandStrikeMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OffhandStrikeMechanicTest {

    private final OffhandStrikeMechanic mechanic = new OffhandStrikeMechanic();

    private PlayerInteractEvent interact(Action action) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        when(event.useItemInHand()).thenReturn(Event.Result.ALLOW);
        return event;
    }

    private PlayerInteractEvent rightClick() {
        return interact(Action.RIGHT_CLICK_AIR);
    }

    @Test
    void baseDamageLookupForWeapons() {
        assertEquals(4.0, OffhandStrikeMechanic.baseDamage(Material.WOODEN_SWORD), 1e-9);
        assertEquals(6.0, OffhandStrikeMechanic.baseDamage(Material.IRON_SWORD), 1e-9);
        assertEquals(8.0, OffhandStrikeMechanic.baseDamage(Material.NETHERITE_SWORD), 1e-9);
        assertEquals(5.0, OffhandStrikeMechanic.baseDamage(Material.IRON_AXE), 1e-9);
        assertEquals(8.0, OffhandStrikeMechanic.baseDamage(Material.TRIDENT), 1e-9);
    }

    @Test
    void baseDamageDefaultsToUnarmedForNonWeapon() {
        assertEquals(1.0, OffhandStrikeMechanic.baseDamage(Material.DIRT), 1e-9);
    }

    @Test
    void returnsFalseForNonInteractEvent() {
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithNoTargetInRange() {
        var player = BukkitMock.mockPlayer();
        when(player.getTargetEntity(4)).thenReturn(null);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), rightClick()));
    }

    @Test
    void leftClickDoesNotTrigger() {
        var player = BukkitMock.mockPlayer();
        var target = mock(LivingEntity.class);
        when(player.getTargetEntity(4)).thenReturn(target);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), interact(Action.LEFT_CLICK_AIR)));
        verify(target, never()).damage(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(org.bukkit.entity.Entity.class));
    }

    @Test
    void blockClickDoesNotTrigger() {
        var player = BukkitMock.mockPlayer();
        var target = mock(LivingEntity.class);
        when(player.getTargetEntity(4)).thenReturn(target);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), interact(Action.LEFT_CLICK_BLOCK)));
        verify(target, never()).damage(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(org.bukkit.entity.Entity.class));
    }

    @Test
    void emptyHandInteractDoesNotTrigger() {
        var player = BukkitMock.mockPlayer();
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.useItemInHand()).thenReturn(Event.Result.DENY);
        var target = mock(LivingEntity.class);
        when(player.getTargetEntity(4)).thenReturn(target);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), event));
        verify(target, never()).damage(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(org.bukkit.entity.Entity.class));
    }

    @Test
    void airOffHandReturnsFalseAndDealsNoDamage() {
        var player = BukkitMock.mockPlayer();
        var offhand = mock(ItemStack.class);
        when(offhand.getType()).thenReturn(Material.AIR);
        when(player.getInventory().getItemInOffHand()).thenReturn(offhand);
        var target = mock(LivingEntity.class);
        when(player.getTargetEntity(4)).thenReturn(target);

        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), rightClick()));
        verify(target, never()).damage(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(org.bukkit.entity.Entity.class));
    }

    @Test
    void unbreakableOffHandReturnsFalseAndDealsNoDamage() {
        var player = BukkitMock.mockPlayer();
        var offhand = mock(ItemStack.class);
        when(offhand.getType()).thenReturn(Material.IRON_SWORD);
        var meta = mock(Damageable.class);
        when(meta.isUnbreakable()).thenReturn(true);
        when(offhand.getItemMeta()).thenReturn(meta);
        when(player.getInventory().getItemInOffHand()).thenReturn(offhand);
        var target = mock(LivingEntity.class);
        when(player.getTargetEntity(4)).thenReturn(target);

        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), rightClick()));
        verify(target, never()).damage(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(org.bukkit.entity.Entity.class));
    }

    @Test
    void dealsDamageAndPersistsDurabilityToOffHandSlot() {
        var player = BukkitMock.mockPlayer();
        var inv = player.getInventory();
        var offhand = mock(ItemStack.class);
        when(offhand.getType()).thenReturn(Material.IRON_SWORD);
        var meta = mock(Damageable.class);
        when(meta.getDamage()).thenReturn(10);
        when(meta.isUnbreakable()).thenReturn(false);
        when(offhand.getItemMeta()).thenReturn((org.bukkit.inventory.meta.ItemMeta) meta);
        when(offhand.getItemMeta()).thenReturn(meta);
        when(inv.getItemInOffHand()).thenReturn(offhand);

        var target = mock(LivingEntity.class);
        when(player.getTargetEntity(4)).thenReturn(target);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), rightClick()));
        }

        verify(target).damage(12.0, player);
        // Durability is consumed through ToolDurability's cancellable
        // PlayerItemDamageEvent path and written back to the off-hand slot.
        verify(meta).setDamage(11);
        verify(offhand).setItemMeta(meta);
        verify(inv).setItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND, offhand);
    }
}
