package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the doubled and tripled drop paths of {@link YieldMultiplierMechanic}
 * and the consume-once semantics of a failed roll.
 */
class YieldMultiplierMechanicTest {

    @AfterEach
    void tearDown() {
        YieldMultiplierMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    private static BlockBreakEvent eventWithCoalDrop(ItemStack toDrop) {
        var block = mock(Block.class);
        var location = mock(Location.class);
        when(block.getLocation()).thenReturn(location);
        when(location.add(anyDouble(), anyDouble(), anyDouble())).thenReturn(location);
        var world = mock(World.class);
        when(block.getWorld()).thenReturn(world);

        var drop = mock(ItemStack.class);
        when(drop.isEmpty()).thenReturn(false);
        when(drop.getAmount()).thenReturn(1);
        when(drop.getMaxStackSize()).thenReturn(64);
        when(drop.clone()).thenReturn(toDrop);
        when(block.getDrops(any(ItemStack.class))).thenReturn(List.of(drop));

        var breakEvent = mock(BlockBreakEvent.class);
        when(breakEvent.getBlock()).thenReturn(block);
        return breakEvent;
    }

    private static Player playerWithMainHand() {
        var player = mock(Player.class);
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(mock(ItemStack.class));
        return player;
    }

    @Test
    void tripleChanceTriplesDropsWhenRolled() {
        YieldMultiplierMechanic.setRandomSource(() -> 1.0);
        var mechanic = new YieldMultiplierMechanic();
        var toDrop = mock(ItemStack.class);
        var event = eventWithCoalDrop(toDrop);

        assertTrue(mechanic.execute(playerWithMainHand(),
                Map.of("yield_chance", 100.0, "triple_chance", 50.0), event));
        verify(toDrop).setAmount(3);
        verify(event.getBlock().getWorld()).dropItemNaturally(any(Location.class), eq(toDrop));
    }

    @Test
    void doubleRollDropsTwiceWhenTripleMisses() {
        YieldMultiplierMechanic.setRandomSource(() -> 99.0);
        var mechanic = new YieldMultiplierMechanic();
        var toDrop = mock(ItemStack.class);
        var event = eventWithCoalDrop(toDrop);

        // 99 < triple 0? no. 99 >= chance 100? no -> double roll succeeds.
        assertTrue(mechanic.execute(playerWithMainHand(),
                Map.of("yield_chance", 100.0, "triple_chance", 0.0), event));
        verify(toDrop).setAmount(2);
        verify(event.getBlock().getWorld()).dropItemNaturally(any(Location.class), eq(toDrop));
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        YieldMultiplierMechanic.setRandomSource(() -> 99.0);
        var mechanic = new YieldMultiplierMechanic();
        var event = eventWithCoalDrop(mock(ItemStack.class));

        // triple_chance 0, double chance 50, roll 99 >= 50 -> no drops, still true.
        assertTrue(mechanic.execute(playerWithMainHand(),
                Map.of("yield_chance", 50.0, "triple_chance", 0.0), event));
        verify(event.getBlock().getWorld(), never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }
}
