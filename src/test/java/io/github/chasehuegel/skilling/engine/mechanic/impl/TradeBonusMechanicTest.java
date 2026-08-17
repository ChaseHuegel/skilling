package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:trade_bonus}: a successful chance roll grants the player
 * a bonus emerald directly to inventory, a failed roll grants nothing but still
 * counts as an activation attempt, and a non-trade event is a no-op.
 */
class TradeBonusMechanicTest {

    @AfterEach
    void tearDown() {
        TradeBonusMechanic.setRandomSource(null);
        TradeBonusMechanic.setEmeraldSource(null);
    }

    private static org.bukkit.inventory.ItemStack emerald() {
        var emerald = mock(org.bukkit.inventory.ItemStack.class);
        when(emerald.getType()).thenReturn(Material.EMERALD);
        return emerald;
    }

    private Player playerWithInventory(PlayerInventory inventory) {
        var player = mock(Player.class);
        when(player.getInventory()).thenReturn(inventory);
        return player;
    }

    @Test
    void returnsFalseForNonTradeEvent() {
        var player = playerWithInventory(mock(PlayerInventory.class));
        assertFalse(new TradeBonusMechanic().execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithNonPositiveChance() {
        var player = playerWithInventory(mock(PlayerInventory.class));
        assertFalse(new TradeBonusMechanic().execute(player, Map.of("chance", 0.0),
                mock(PlayerTradeEvent.class)));
    }

    @Test
    void successfulRollGrantsBonusEmerald() {
        var inventory = mock(PlayerInventory.class);
        var player = playerWithInventory(inventory);
        TradeBonusMechanic.setRandomSource(() -> 0.0); // always succeed
        TradeBonusMechanic.setEmeraldSource(TradeBonusMechanicTest::emerald);

        assertTrue(new TradeBonusMechanic().execute(player, Map.of("chance", 50.0),
                mock(PlayerTradeEvent.class)));

        ArgumentCaptor<ItemStack> captor = ArgumentCaptor.forClass(ItemStack.class);
        verify(inventory).addItem(captor.capture());
        assertTrue(captor.getValue().getType() == Material.EMERALD,
                "the bonus must be an emerald");
    }

    @Test
    void failedRollGrantsNothingButCountsAsActivation() {
        var inventory = mock(PlayerInventory.class);
        var player = playerWithInventory(inventory);
        TradeBonusMechanic.setRandomSource(() -> 0.99); // always fail

        assertTrue(new TradeBonusMechanic().execute(player, Map.of("chance", 50.0),
                mock(PlayerTradeEvent.class)),
                "a failed roll still counts as an activation attempt");
        verify(inventory, never()).addItem(any(ItemStack.class));
    }
}
