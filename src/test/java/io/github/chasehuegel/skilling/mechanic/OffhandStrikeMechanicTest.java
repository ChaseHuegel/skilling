package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.OffhandStrikeMechanic;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OffhandStrikeMechanicTest {

    private final OffhandStrikeMechanic mechanic = new OffhandStrikeMechanic();

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
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithNoTargetInRange() {
        var player = BukkitMock.mockPlayer();
        when(player.getTargetEntity(4)).thenReturn(null);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void dealsDamageAndConsumesDurability() {
        var player = BukkitMock.mockPlayer();
        var inv = player.getInventory();
        var offhand = mock(ItemStack.class);
        when(offhand.getType()).thenReturn(Material.IRON_SWORD);
        var meta = mock(Damageable.class);
        when(meta.getDamage()).thenReturn(10);
        when(offhand.getItemMeta()).thenReturn((org.bukkit.inventory.meta.ItemMeta) meta);
        when(offhand.getItemMeta()).thenReturn(meta);
        when(inv.getItemInOffHand()).thenReturn(offhand);

        var target = mock(LivingEntity.class);
        when(player.getTargetEntity(4)).thenReturn(target);

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), BukkitMock.mockInteractEvent(player)));
        verify(target).damage(12.0, player);
        verify(meta).setDamage(11);
        verify(offhand).setItemMeta(meta);
    }
}
