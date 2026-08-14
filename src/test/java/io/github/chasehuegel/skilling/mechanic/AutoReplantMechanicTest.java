package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.mechanic.impl.AutoReplantMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:auto_replant} only replants when the harvested spot is
 * still empty and the player is still relevant, so it never overwrites a block
 * placed over the crop in the meantime.
 */
class AutoReplantMechanicTest {

    private final AutoReplantMechanic mechanic = new AutoReplantMechanic();

    @Test
    void replantsWhenTheSpotIsStillAir() {
        Material[] type = {Material.WHEAT};
        Block block = mock(Block.class);
        when(block.getType()).thenAnswer(inv -> type[0]);
        Ageable ageable = mock(Ageable.class);
        when(ageable.getAge()).thenReturn(7);
        when(ageable.getMaximumAge()).thenReturn(7);
        when(block.getBlockData()).thenReturn(ageable);

        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);

        List<Runnable> captured = new ArrayList<>();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            when(Skilling.getInstance()).thenReturn(mock(Skilling.class));
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(inv -> {
                captured.add(inv.getArgument(1));
                return mock(BukkitTask.class);
            });
            when(Bukkit.getScheduler()).thenReturn(scheduler);

            mechanic.execute(player, Map.of(), event);
            // The harvest completes: the spot becomes air before the replant tick.
            type[0] = Material.AIR;
            captured.get(0).run();
        }

        verify(block).setType(Material.WHEAT);
        verify(block).setBlockData(any(Ageable.class), eq(false));
    }

    @Test
    void doesNotOverwriteAnOccupiedSpot() {
        Material[] type = {Material.WHEAT};
        Block block = mock(Block.class);
        when(block.getType()).thenAnswer(inv -> type[0]);
        Ageable ageable = mock(Ageable.class);
        when(ageable.getAge()).thenReturn(7);
        when(ageable.getMaximumAge()).thenReturn(7);
        when(block.getBlockData()).thenReturn(ageable);

        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);

        List<Runnable> captured = new ArrayList<>();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            when(Skilling.getInstance()).thenReturn(mock(Skilling.class));
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(inv -> {
                captured.add(inv.getArgument(1));
                return mock(BukkitTask.class);
            });
            when(Bukkit.getScheduler()).thenReturn(scheduler);

            mechanic.execute(player, Map.of(), event);
            // Someone placed a block over the harvested crop before the tick.
            type[0] = Material.STONE;
            captured.get(0).run();
        }

        verify(block, never()).setType(any(Material.class));
        verify(block, never()).setBlockData(any(), eq(false));
    }

    @Test
    void doesNotReplantForAnOfflinePlayer() {
        Material[] type = {Material.WHEAT};
        Block block = mock(Block.class);
        when(block.getType()).thenAnswer(inv -> type[0]);
        Ageable ageable = mock(Ageable.class);
        when(ageable.getAge()).thenReturn(7);
        when(ageable.getMaximumAge()).thenReturn(7);
        when(block.getBlockData()).thenReturn(ageable);

        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(false);

        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);

        List<Runnable> captured = new ArrayList<>();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            when(Skilling.getInstance()).thenReturn(mock(Skilling.class));
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(inv -> {
                captured.add(inv.getArgument(1));
                return mock(BukkitTask.class);
            });
            when(Bukkit.getScheduler()).thenReturn(scheduler);

            mechanic.execute(player, Map.of(), event);
            type[0] = Material.AIR;
            captured.get(0).run();
        }

        verify(block, never()).setType(any(Material.class));
    }
}
