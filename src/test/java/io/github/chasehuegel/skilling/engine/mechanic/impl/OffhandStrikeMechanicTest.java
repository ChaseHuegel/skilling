package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:offhand_strike} bounds its reach and multiplier and never
 * strikes another player.
 */
class OffhandStrikeMechanicTest {

    private final OffhandStrikeMechanic mechanic = new OffhandStrikeMechanic();

    private Player playerWithIronSwordOffhand() {
        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack offhand = mock(ItemStack.class);
        when(offhand.getType()).thenReturn(Material.IRON_SWORD);
        Damageable meta = mock(Damageable.class);
        when(meta.isUnbreakable()).thenReturn(false);
        when(offhand.getItemMeta()).thenReturn(meta);
        when(inv.getItemInOffHand()).thenReturn(offhand);
        when(player.getInventory()).thenReturn(inv);
        return player;
    }

    private PlayerInteractEvent rightClick() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.useItemInHand()).thenReturn(Event.Result.ALLOW);
        return event;
    }

    private boolean strike(Player player, Map<String, Object> params) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            return mechanic.execute(player, params, rightClick());
        }
    }

    @Test
    void reachIsClampedToVanillaAttackReach() {
        Player player = playerWithIronSwordOffhand();
        Monster monster = mock(Monster.class);
        // reach 100 clamps to MAX_REACH (4.5), truncated to 4 for the int raycast.
        when(player.getTargetEntity(4)).thenReturn(monster);

        assertTrue(strike(player, Map.of("multiplier", 1.0, "reach", 100.0)));
        verify(player).getTargetEntity(4);
        verify(monster).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }

    @Test
    void multiplierIsClampedToSaneMaximum() {
        Player player = playerWithIronSwordOffhand();
        Monster monster = mock(Monster.class);
        when(player.getTargetEntity(4)).thenReturn(monster);

        assertTrue(strike(player, Map.of("multiplier", 100.0)));
        // Iron sword base 6.0 * clamped MAX_MULTIPLIER (4.0) = 24.0.
        verify(monster).damage(6.0 * OffhandStrikeMechanic.MAX_MULTIPLIER, player);
    }

    @Test
    void hostileTargetIsStruckUnderDefaultFilter() {
        Player player = playerWithIronSwordOffhand();
        Monster monster = mock(Monster.class);
        when(player.getTargetEntity(4)).thenReturn(monster);

        assertTrue(strike(player, Map.of("multiplier", 2.0)));
        verify(monster).damage(12.0, player);
    }

    @Test
    void otherPlayersAreNeverStruck() {
        Player player = playerWithIronSwordOffhand();
        Player other = mock(Player.class);
        when(player.getTargetEntity(4)).thenReturn(other);

        assertFalse(strike(player, Map.of("multiplier", 2.0)));
        verify(other, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }
}
