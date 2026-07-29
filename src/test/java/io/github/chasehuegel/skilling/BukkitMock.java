package io.github.chasehuegel.skilling;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.Mockito.*;

public final class BukkitMock {

    private BukkitMock() {}

    public static Player mockPlayer() {
        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(player.getFoodLevel()).thenReturn(20);
        return player;
    }

    public static Player mockPlayerWithItems(Map<Material, Integer> items) {
        Player player = mockPlayer();
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = new ItemStack[36];
        int idx = 0;
        for (var entry : items.entrySet()) {
            if (idx < 36) {
                contents[idx] = new ItemStack(entry.getKey(), entry.getValue());
                idx++;
            }
        }
        when(inv.getContents()).thenReturn(contents);
        return player;
    }

    public static EntityDamageByEntityEvent mockDamageEvent(Player damager, double damage) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(damager);
        when(event.getEntity()).thenReturn(damager);
        when(event.getFinalDamage()).thenReturn(damage);
        when(event.getDamage()).thenReturn(damage);
        return event;
    }

    public static BlockBreakEvent mockBlockBreakEvent(Player player) {
        return mock(BlockBreakEvent.class);
    }

    public static PlayerInteractEvent mockInteractEvent(Player player) {
        return mock(PlayerInteractEvent.class);
    }

    public static PlayerItemConsumeEvent mockConsumeEvent(Player player) {
        return mock(PlayerItemConsumeEvent.class);
    }
}
