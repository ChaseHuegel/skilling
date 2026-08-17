package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code core:area_fertilize} mechanic: it spreads bonemeal to
 * matching same-type blocks in a radius around the fertilized block, respects
 * the radius clamp, leaves non-matching types and the origin untouched, and
 * no-ops on a non-fertilize event or a playerless fertilize.
 */
class AreaFertilizeMechanicTest {

    @AfterEach
    void tearDown() {
        AreaFertilizeMechanic.processingSet().clear();
    }

    /**
     * Builds a fertilize event whose origin is WHEAT at (0,0,0). Neighbors
     * within {@code sameTypeRadius} of the origin match WHEAT; neighbors farther
     * out (or all when {@code sameTypeRadius == 0}) are a different type.
     *
     * @param radius         the configured mechanic radius
     * @param sameTypeRadius how many Chebyshev rings match the origin type (0 = all)
     * @param fertilized     counter incremented per applyBoneMeal call
     * @return the mocked fertilize event
     */
    private BlockFertilizeEvent fertilizeEvent(int radius, int sameTypeRadius, AtomicInteger fertilized) {
        var world = mock(World.class);
        var origin = mock(Block.class);
        when(origin.getType()).thenReturn(Material.WHEAT);
        when(origin.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        when(origin.getRelative(anyInt(), eq(0), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);
            var block = mock(Block.class);
            Material type = sameTypeRadius > 0
                    && Math.max(Math.abs(dx), Math.abs(dz)) > sameTypeRadius
                    ? Material.DIRT : Material.WHEAT;
            when(block.getType()).thenReturn(type);
            when(block.getLocation()).thenReturn(new Location(world, dx, 0, dz));
            Mockito.doAnswer(inv2 -> {
                fertilized.incrementAndGet();
                return true;
            }).when(block).applyBoneMeal(Mockito.any());
            return block;
        });

        var event = mock(BlockFertilizeEvent.class);
        when(event.getBlock()).thenReturn(origin);
        when(event.getPlayer()).thenReturn(mock(Player.class));
        return event;
    }

    private int runFertilize(int radius) {
        AtomicInteger fertilized = new AtomicInteger();
        var event = fertilizeEvent(radius, 0, fertilized);
        new AreaFertilizeMechanic().execute(mock(Player.class), Map.of("radius", radius), event);
        return fertilized.get();
    }

    @Test
    void returnsFalseForNonFertilizeEvent() {
        var player = mock(Player.class);
        assertFalse(new AreaFertilizeMechanic().execute(player, Map.of("radius", 1),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWhenPlayerIsNull() {
        var world = mock(World.class);
        var origin = mock(Block.class);
        when(origin.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        var event = mock(BlockFertilizeEvent.class);
        when(event.getBlock()).thenReturn(origin);
        when(event.getPlayer()).thenReturn(null);
        assertFalse(new AreaFertilizeMechanic().execute(mock(Player.class),
                Map.of("radius", 1), event));
    }

    @Test
    void returnsFalseWithNonPositiveRadius() {
        var event = mock(BlockFertilizeEvent.class);
        when(event.getPlayer()).thenReturn(mock(Player.class));
        assertFalse(new AreaFertilizeMechanic().execute(mock(Player.class),
                Map.of("radius", 0), event));
    }

    @Test
    void fertilizesMatchingCropsInRadius() {
        // A 3x3 disk minus the origin = 8 same-type neighbors.
        assertEquals(8, runFertilize(1));
    }

    @Test
    void noBlocksOutsideRadiusAreTouched() {
        // A 5x5 disk minus the origin = 24 neighbors; a radius of 2 must not
        // reach blocks at Manhattan distance beyond 2.
        assertEquals(24, runFertilize(2));
    }

    @Test
    void skipsDifferentTypeBlocksWithinRadius() {
        AtomicInteger fertilized = new AtomicInteger();
        // Only the 8 blocks at Chebyshev distance 1 match the origin type;
        // everything farther out is dirt and must be skipped.
        var event = fertilizeEvent(3, 1, fertilized);
        new AreaFertilizeMechanic().execute(mock(Player.class), Map.of("radius", 3), event);
        assertEquals(8, fertilized.get());
    }

    @Test
    void radiusIsClampedToConfiguredMax() {
        // A configured radius of 100 clamps to MAX_RADIUS: (2*8+1)^2 - 1 = 288
        // candidate blocks.
        int max = AreaFertilizeMechanic.MAX_RADIUS;
        assertEquals((2 * max + 1) * (2 * max + 1) - 1, runFertilize(100));
    }

    @Test
    void defaultRadiusIsOne() {
        AtomicInteger fertilized = new AtomicInteger();
        var event = fertilizeEvent(1, 0, fertilized);
        new AreaFertilizeMechanic().execute(mock(Player.class), Map.of(), event);
        assertEquals(8, fertilized.get());
    }

    @Test
    void originBlockIsNotReFertilized() {
        var world = mock(World.class);
        var origin = mock(Block.class);
        when(origin.getType()).thenReturn(Material.WHEAT);
        when(origin.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        when(origin.getRelative(anyInt(), eq(0), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);
            var block = mock(Block.class);
            when(block.getType()).thenReturn(Material.WHEAT);
            when(block.getLocation()).thenReturn(new Location(world, dx, 0, dz));
            return block;
        });

        var event = mock(BlockFertilizeEvent.class);
        when(event.getBlock()).thenReturn(origin);
        when(event.getPlayer()).thenReturn(mock(Player.class));

        new AreaFertilizeMechanic().execute(mock(Player.class), Map.of("radius", 1), event);
        verify(origin, never()).applyBoneMeal(Mockito.any());
    }

    @Test
    void applyBoneMealUsesUpFace() {
        var world = mock(World.class);
        var origin = mock(Block.class);
        when(origin.getType()).thenReturn(Material.WHEAT);
        when(origin.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        var neighbor = mock(Block.class);
        when(neighbor.getType()).thenReturn(Material.WHEAT);
        when(neighbor.getLocation()).thenReturn(new Location(world, 1, 0, 0));
        when(origin.getRelative(anyInt(), eq(0), anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);
            if (dx == 1 && dz == 0) return neighbor;
            var other = mock(Block.class);
            when(other.getType()).thenReturn(Material.DIRT);
            when(other.getLocation()).thenReturn(new Location(world, dx, 0, dz));
            return other;
        });

        var event = mock(BlockFertilizeEvent.class);
        when(event.getBlock()).thenReturn(origin);
        when(event.getPlayer()).thenReturn(mock(Player.class));

        new AreaFertilizeMechanic().execute(mock(Player.class), Map.of("radius", 1), event);
        verify(neighbor).applyBoneMeal(BlockFace.UP);
    }

    @Test
    void fertilizeProcessingGuardSkipsNestedDispatch() {
        // Paper's Location stores its world in a WeakReference; keep a strong
        // reference so the location's hashCode cannot change mid-test when the
        // world mock is collected.
        var world = mock(World.class);
        var block = mock(Block.class);
        Location loc = new Location(world, 1, 1, 1);
        when(block.getLocation()).thenReturn(loc);
        AreaFertilizeMechanic.processingSet().add(loc);
        var event = mock(BlockFertilizeEvent.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(mock(Player.class));

        var profileManager = mock(io.github.chasehuegel.skilling.engine.profile.ProfileManager.class);
        var listener = new io.github.chasehuegel.skilling.engine.listener.SkillEventListener(
                mock(io.github.chasehuegel.skilling.Skilling.class),
                mock(io.github.chasehuegel.skilling.engine.SkillManager.class), profileManager,
                mock(io.github.chasehuegel.skilling.engine.tag.TagResolver.class),
                mock(io.github.chasehuegel.skilling.engine.requirements.RequirementEngine.class),
                mock(io.github.chasehuegel.skilling.engine.registry.MechanicRegistry.class),
                mock(io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer.class),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                mock(io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry.class));

        listener.onFertilize(event);
        Mockito.verify(profileManager, never()).getProfile(Mockito.any());
    }
}
