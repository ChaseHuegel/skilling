package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:fireball}: a right-click launches a non-griefing
 * SmallFireball (no yield, non-incendiary) carrying configured damage; a
 * left-click is a no-op.
 */
class FireballMechanicTest {

    private static final Map<String, Object> PARAMS = Map.of("speed", 2.0, "damage", 6.0);

    @Test
    void rightClickAirLaunchesNonGriefingFireball() {
        var player = mock(Player.class);
        when(player.getLocation()).thenReturn(mock(Location.class));
        var loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);
        when(loc.getDirection()).thenReturn(new Vector(1, 0, 0));
        var fireball = mock(SmallFireball.class);
        var pdc = mock(PersistentDataContainer.class);
        when(fireball.getPersistentDataContainer()).thenReturn(pdc);
        when(player.launchProjectile(SmallFireball.class)).thenReturn(fireball);

        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertTrue(new FireballMechanic().execute(player, PARAMS, event));
        verify(fireball).setYield(0.0f);
        verify(fireball).setIsIncendiary(false);
        verify(fireball).setVelocity(any(Vector.class));
        verify(pdc).set(ProjectileMechanic.DAMAGE_KEY, PersistentDataType.DOUBLE, 6.0);
    }

    @Test
    void leftClickIsNoOp() {
        var player = mock(Player.class);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);
        assertFalse(new FireballMechanic().execute(player, PARAMS, event));
        verify(player, never()).launchProjectile(any());
    }
}