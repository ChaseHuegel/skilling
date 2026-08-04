package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:knockback} bounds its radius, force, and vertical params
 * and never knocks other players or the caster.
 */
class KnockbackMechanicTest {

    private final KnockbackMechanic mechanic = new KnockbackMechanic();

    private Player playerFacingPositiveX() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Location loc = mock(Location.class);
        when(loc.getDirection()).thenReturn(new Vector(1, 0, 0));
        when(player.getLocation()).thenReturn(loc);
        return player;
    }

    @Test
    void oversizedRadiusIsClampedToBukkitCap() {
        Player player = playerFacingPositiveX();
        var monster = mock(Monster.class);
        when(player.getLocation().getNearbyLivingEntities(32.0)).thenReturn(List.of(monster));

        assertTrue(mechanic.execute(player, Map.of("force", 1.0, "radius", 1000.0), mock(Event.class)));
        // The unbounded scan must never run: the radius is clamped to [0, 32].
        verify(player.getLocation()).getNearbyLivingEntities(32.0);
        verify(monster).setVelocity(any(Vector.class));
    }

    @Test
    void forceIsClampedToSaneMaximum() {
        Player player = playerFacingPositiveX();
        var monster = mock(Monster.class);
        when(player.getLocation().getNearbyLivingEntities(5.0)).thenReturn(List.of(monster));

        assertTrue(mechanic.execute(player,
                Map.of("force", 100.0, "radius", 5.0, "vertical", 0.0), mock(Event.class)));
        verify(monster).setVelocity(argThat(v -> v.getX() == KnockbackMechanic.MAX_FORCE && v.getY() == 0.0));
    }

    @Test
    void verticalIsClampedToSaneMaximum() {
        Player player = playerFacingPositiveX();
        var monster = mock(Monster.class);
        when(player.getLocation().getNearbyLivingEntities(5.0)).thenReturn(List.of(monster));

        assertTrue(mechanic.execute(player,
                Map.of("force", 1.0, "radius", 5.0, "vertical", 100.0), mock(Event.class)));
        verify(monster).setVelocity(argThat(v -> v.getY() == KnockbackMechanic.MAX_VERTICAL));
    }

    @Test
    void casterIsNeverKnocked() {
        Player player = playerFacingPositiveX();
        var monster = mock(Monster.class);
        when(player.getLocation().getNearbyLivingEntities(5.0)).thenReturn(List.of(player, monster));

        assertTrue(mechanic.execute(player, Map.of("force", 2.0, "radius", 5.0), mock(Event.class)));
        verify(monster).setVelocity(any(Vector.class));
        verify(player, never()).setVelocity(any(Vector.class));
    }

    @Test
    void otherPlayersAreNeverKnockedEvenUnderAllTargets() {
        Player player = playerFacingPositiveX();
        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        var monster = mock(Monster.class);
        when(player.getLocation().getNearbyLivingEntities(5.0)).thenReturn(List.of(other, monster));

        assertTrue(mechanic.execute(player,
                Map.of("force", 2.0, "radius", 5.0, "targets", "all"), mock(Event.class)));
        verify(monster).setVelocity(any(Vector.class));
        verify(other, never()).setVelocity(any(Vector.class));
    }

    @Test
    void singleTargetPathKnocksHostileButNotAPlayer() {
        Player player = playerFacingPositiveX();
        var monster = mock(Monster.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(monster);
        assertTrue(mechanic.execute(player, Map.of("force", 2.0), event));
        verify(monster).setVelocity(any(Vector.class));

        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        var pvpEvent = mock(EntityDamageByEntityEvent.class);
        when(pvpEvent.getEntity()).thenReturn(other);
        assertFalse(mechanic.execute(player, Map.of("force", 2.0), pvpEvent));
        verify(other, never()).setVelocity(any(Vector.class));
    }
}
