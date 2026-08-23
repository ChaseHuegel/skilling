package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:lunge} only fires on a right-click, clamps its impulse
 * bounds, and never acts on a left-click or a non-interact event.
 */
class LungeMechanicTest {

    private final LungeMechanic mechanic = new LungeMechanic();

    private Player playerFacingPositiveX() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Location loc = mock(Location.class);
        when(loc.getDirection()).thenReturn(new Vector(1, 0, 0));
        when(player.getLocation()).thenReturn(loc);
        return player;
    }

    private PlayerInteractEvent interact(Action action) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        return event;
    }

    @Test
    void rightClickLungesForward() {
        Player player = playerFacingPositiveX();

        assertTrue(mechanic.execute(player, Map.of("force", 1.0, "vertical", 0.0), interact(Action.RIGHT_CLICK_AIR)));
        verify(player).setVelocity(argThat(v -> v.getX() == 1.0 && v.getY() == 0.0));
    }

    @Test
    void rightClickOnBlockAlsoLunges() {
        Player player = playerFacingPositiveX();

        assertTrue(mechanic.execute(player, Map.of("force", 1.0, "vertical", 0.0), interact(Action.RIGHT_CLICK_BLOCK)));
        verify(player).setVelocity(argThat(v -> v.getX() == 1.0));
    }

    @Test
    void forceIsClampedToSaneMaximum() {
        Player player = playerFacingPositiveX();

        assertTrue(mechanic.execute(player, Map.of("force", 100.0, "vertical", 0.0), interact(Action.RIGHT_CLICK_AIR)));
        verify(player).setVelocity(argThat(v -> v.getX() == LungeMechanic.MAX_FORCE));
    }

    @Test
    void verticalIsClampedToSaneMaximum() {
        Player player = playerFacingPositiveX();

        assertTrue(mechanic.execute(player, Map.of("force", 1.0, "vertical", 100.0), interact(Action.RIGHT_CLICK_AIR)));
        verify(player).setVelocity(argThat(v -> v.getY() == LungeMechanic.MAX_VERTICAL));
    }

    @Test
    void leftClickIsANoOpThatDoesNotLunge() {
        Player player = playerFacingPositiveX();

        assertFalse(mechanic.execute(player, Map.of("force", 2.0), interact(Action.LEFT_CLICK_AIR)));
        verify(player, never()).setVelocity(argThat(v -> v.getX() > 0));
    }

    @Test
    void zeroForceIsANoOp() {
        Player player = playerFacingPositiveX();

        assertFalse(mechanic.execute(player, Map.of("force", 0.0), interact(Action.RIGHT_CLICK_AIR)));
        verify(player, never()).setVelocity(argThat(v -> v.getX() > 0));
    }

    @Test
    void nonInteractEventIsANoOp() {
        Player player = playerFacingPositiveX();

        assertFalse(mechanic.execute(player, Map.of("force", 2.0), mock(Event.class)));
        verify(player, never()).setVelocity(argThat(v -> v.getX() > 0));
    }
}