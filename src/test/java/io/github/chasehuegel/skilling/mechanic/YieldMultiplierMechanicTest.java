package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.YieldMultiplierMechanic;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YieldMultiplierMechanicTest {

    @Test
    void returnsFalseForNonBlockBreakEvent() {
        var mechanic = new YieldMultiplierMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var mechanic = new YieldMultiplierMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void successSuppressesVanillaDropsAndSpawnsDoubledSet() {
        var mechanic = new YieldMultiplierMechanic();
        var player = BukkitMock.mockPlayer();
        var event = BukkitMock.mockBlockBreakEvent(player);
        var block = mock(Block.class);
        when(event.getBlock()).thenReturn(block);

        var drop = mock(ItemStack.class);
        when(drop.isEmpty()).thenReturn(false);
        when(drop.getAmount()).thenReturn(1);
        when(block.getDrops(any())).thenReturn(List.of(drop));

        var world = mock(World.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 1, 2, 3));

        assertTrue(mechanic.execute(player, Map.of("yield_chance", 100.0), event));

        // Vanilla drops are suppressed so a break yields only the doubled set (no 3x).
        verify(event).setDropItems(false);
        verify(drop).setAmount(2);
        verify(world).dropItemNaturally(any(Location.class), eq(drop));
    }

    @Test
    void chanceZeroLeavesVanillaDropsUntouched() {
        var mechanic = new YieldMultiplierMechanic();
        var player = BukkitMock.mockPlayer();
        var event = BukkitMock.mockBlockBreakEvent(player);

        assertFalse(mechanic.execute(player, Map.of("yield_chance", 0.0), event));
        verify(event, never()).setDropItems(false);
    }
}
