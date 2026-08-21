package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link LootBonusMechanic} on generated world loot: chance gating, the
 * activation-attempt contract on a failed roll, and one-copy bonus payout into
 * the player inventory.
 */
class LootBonusMechanicTest {

    @AfterEach
    void tearDown() {
        LootBonusMechanic.setRandomSource(() -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(100));
    }

    private LootGenerateEvent lootEvent() {
        var loot = mock(ItemStack.class);
        when(loot.getType()).thenReturn(Material.EMERALD);
        when(loot.clone()).thenReturn(loot);
        var event = mock(LootGenerateEvent.class);
        when(event.getLoot()).thenReturn(List.of(loot));
        return event;
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
    void returnsFalseOffLootGenerateEvent() {
        var player = player();
        assertFalse(new LootBonusMechanic().execute(player, Map.of("chance", 50.0),
                mock(EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithNonPositiveChance() {
        var player = player();
        assertFalse(new LootBonusMechanic().execute(player, Map.of("chance", 0.0), lootEvent()));
    }

    @Test
    void failedRollIsStillAnActivationAttempt() {
        LootBonusMechanic.setRandomSource(() -> 99.0);
        var player = player();
        boolean executed = new LootBonusMechanic().execute(player, Map.of("chance", 50.0), lootEvent());
        assertTrue(executed, "a failed roll must still consume the activation");
        verify(player.getInventory(), never()).addItem(any());
    }

    @Test
    void successfulRollGrantsOneBonusCopy() {
        LootBonusMechanic.setRandomSource(() -> 10.0);
        var player = player();
        assertTrue(new LootBonusMechanic().execute(player, Map.of("chance", 50.0), lootEvent()));
        verify(player.getInventory()).addItem(any(ItemStack.class));
    }
}