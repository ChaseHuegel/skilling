package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:summon_guardian} summons a scaled friendly IronGolem on a
 * right-click and never on a left-click.
 */
class SummonGuardianMechanicTest {

    private static final Map<String, Object> PARAMS = Map.of(
            "scale", 0.7,
            "speed", 1.5,
            "damage", 0.7,
            "name", "Arcane Guardian"
    );

    private static PlayerInteractEvent click(Action action) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        return event;
    }

    @Test
    void leftClickIsNoOp() {
        var player = mock(Player.class);
        assertFalse(new SummonGuardianMechanic().execute(player, PARAMS, click(Action.LEFT_CLICK_AIR)));
    }

    @Test
    void rightClickAirSpawnsScaledGuardian() {
        var world = mock(World.class);
        var player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        var guardian = mock(IronGolem.class);
        var inst = mock(AttributeInstance.class);
        when(guardian.getAttribute(any(Attribute.class))).thenReturn(inst);
        when(world.spawn(any(Location.class), eq(IronGolem.class),
                eq(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM),
                eq(false), any()))
                .thenReturn(guardian);

        assertTrue(new SummonGuardianMechanic().execute(player, PARAMS, click(Action.RIGHT_CLICK_AIR)));

        verify(world).spawn(eq(new Location(world, 10, 64, 20)), eq(IronGolem.class),
                eq(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM), eq(false), any());
        verify(guardian).getAttribute(Attribute.SCALE);
        verify(guardian).getAttribute(Attribute.MOVEMENT_SPEED);
        verify(guardian).getAttribute(Attribute.ATTACK_DAMAGE);
        verify(inst, times(3)).addModifier(ArgumentMatchers.any(AttributeModifier.class));
        verify(guardian).customName(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rightClickBlockAlsoSpawns() {
        var world = mock(World.class);
        var player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        var guardian = mock(IronGolem.class);
        when(guardian.getAttribute(any(Attribute.class))).thenReturn(mock(AttributeInstance.class));
        when(world.spawn(any(Location.class), eq(IronGolem.class),
                eq(org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM),
                eq(false), any()))
                .thenReturn(guardian);

        assertTrue(new SummonGuardianMechanic().execute(player, PARAMS, click(Action.RIGHT_CLICK_BLOCK)));
    }
}