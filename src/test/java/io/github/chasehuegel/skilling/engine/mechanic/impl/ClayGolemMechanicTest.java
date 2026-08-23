package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:clay_golem}: placing a carved pumpkin that completes a
 * four-clay-block T scaffold clears the pumpkin + clay and spawns an
 * {@link IronGolem} with scaled attributes, while an incomplete scaffold is a
 * no-op that spends nothing.
 */
class ClayGolemMechanicTest {

    private static final Map<String, Object> PARAMS = Map.of(
            "scale", 0.7,
            "speed_multiplier", 1.5,
            "damage_multiplier", 0.7,
            "name", "Clay Golem"
    );

    @Test
    void completeClayScaffoldClearsBlocksAndSpawnsScaledGolem() {
        var world = mock(World.class);
        var head = block(Material.CARVED_PUMPKIN, world);
        var placed = mock(BlockPlaceEvent.class);
        when(placed.getBlockPlaced()).thenReturn(head);

        // Populate the four scaffold positions (relative to the head) with clay.
        var body = block(Material.CLAY, world);
        var left = block(Material.CLAY, world);
        var right = block(Material.CLAY, world);
        var legs = block(Material.CLAY, world);
        when(head.getRelative(0, -1, 0)).thenReturn(body);
        when(head.getRelative(-1, -1, 0)).thenReturn(left);
        when(head.getRelative(1, -1, 0)).thenReturn(right);
        when(head.getRelative(0, -2, 0)).thenReturn(legs);
        when(head.getWorld()).thenReturn(world);
        when(head.getX()).thenReturn(3);
        when(head.getY()).thenReturn(5);
        when(head.getZ()).thenReturn(4);

        var golem = mock(IronGolem.class);
        var inst = mock(AttributeInstance.class);
        when(golem.getAttribute(any(Attribute.class))).thenReturn(inst);
        when(world.spawn(any(Location.class), eq(IronGolem.class),
                eq(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM),
                eq(false), any()))
                .thenReturn(golem);
        var player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        assertTrue(new ClayGolemMechanic().execute(player, PARAMS, placed));

        // The head and all four clay blocks are cleared, the golem is spawned at
        // head y - 2 with feet on the floor, and its three stats are scaled.
        verify(head).setType(Material.AIR, false);
        verify(body).setType(Material.AIR, false);
        verify(left).setType(Material.AIR, false);
        verify(right).setType(Material.AIR, false);
        verify(legs).setType(Material.AIR, false);
        verify(world).spawn(eq(new Location(world, 3.5, 3.0, 4.5)), eq(IronGolem.class),
                eq(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM), eq(false), any());
        verify(golem).getAttribute(Attribute.SCALE);
        verify(golem).getAttribute(Attribute.MOVEMENT_SPEED);
        verify(golem).getAttribute(Attribute.ATTACK_DAMAGE);
        verify(inst, times(3)).addModifier(ArgumentMatchers.any(AttributeModifier.class));
    }

    @Test
    void nonPumpkinPlacementIsNoOp() {
        var placed = mock(BlockPlaceEvent.class);
        var head = block(Material.STONE, mock(World.class));
        when(placed.getBlockPlaced()).thenReturn(head);
        var player = mock(Player.class);

        assertFalse(new ClayGolemMechanic().execute(player, PARAMS, placed));
        verify(head, never()).getRelative(0, -1, 0);
    }

    @Test
    void incompleteScaffoldDoesNotSpawn() {
        var world = mock(World.class);
        var head = block(Material.CARVED_PUMPKIN, world);
        var placed = mock(BlockPlaceEvent.class);
        when(placed.getBlockPlaced()).thenReturn(head);

        var body = block(Material.CLAY, world);
        var left = block(Material.DIRT, world);
        var right = block(Material.CLAY, world);
        var legs = block(Material.CLAY, world);
        when(head.getRelative(0, -1, 0)).thenReturn(body);
        when(head.getRelative(-1, -1, 0)).thenReturn(left);
        when(head.getRelative(1, -1, 0)).thenReturn(right);
        when(head.getRelative(0, -2, 0)).thenReturn(legs);

        var player = mock(Player.class);
        assertFalse(new ClayGolemMechanic().execute(player, PARAMS, placed));
        verify(world, never()).spawn(any(), eq(IronGolem.class), any(), eq(false), any());
        verify(head, never()).setType(Material.AIR, false);
    }

    private static Block block(Material type, World world) {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        when(block.getWorld()).thenReturn(world);
        return block;
    }
}