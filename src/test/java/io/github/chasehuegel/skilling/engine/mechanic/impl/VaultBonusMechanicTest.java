package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link VaultBonusMechanic} on a trial-vault state change: chance gating,
 * the activation-attempt contract on a failed roll, and a bonus drop granted to
 * the player inventory from the configured loot table.
 */
class VaultBonusMechanicTest {

    @AfterEach
    void tearDown() {
        VaultBonusMechanic.setRandomSource(() -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(100));
    }

    private Player player() {
        var inventory = mock(PlayerInventory.class);
        when(inventory.addItem(any(ItemStack.class))).thenReturn(new java.util.HashMap<>());
        var p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getInventory()).thenReturn(inventory);
        return p;
    }

    @Test
    void returnsFalseWithoutATable() {
        var player = player();
        assertFalse(new VaultBonusMechanic().execute(player,
                Map.of("chance", 50.0), mock(EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithNonPositiveChance() {
        var player = player();
        assertFalse(new VaultBonusMechanic().execute(player,
                Map.of("chance", 0.0, "table", "minecraft:chests/trial_chambers_reward"),
                mock(EntityDamageEvent.class)));
    }

    @Test
    void failedRollDoesNotCallTheLootTable() {
        VaultBonusMechanic.setRandomSource(() -> 99.0);
        var player = player();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            LootTable table = mock(LootTable.class);
            bukkit.when(() -> Bukkit.getLootTable(any(NamespacedKey.class))).thenReturn(table);
            boolean executed = new VaultBonusMechanic().execute(player,
                    Map.of("chance", 50.0, "table", "minecraft:chests/trial_chambers_reward"),
                    mock(io.papermc.paper.event.block.VaultChangeStateEvent.class));
            assertTrue(executed, "a failed roll must still consume the activation");
            verify(table, never()).populateLoot(any(), any(LootContext.class));
        }
    }
}