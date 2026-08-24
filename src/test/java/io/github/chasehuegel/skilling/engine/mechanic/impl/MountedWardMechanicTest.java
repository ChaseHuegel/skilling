package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link MountedWardMechanic}: it reduces damage dealt to the vehicle
 * the activating player is riding, is a no-op on any other damaged entity, and
 * clamps the reduction to at most 100%.
 */
class MountedWardMechanicTest {

    private EntityDamageEvent damageOn(Vehicle vehicle, double base) {
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(vehicle);
        when(event.getDamage()).thenReturn(base);
        return event;
    }

    private Vehicle vehicleRiddenBy(Player player) {
        var vehicle = mock(Vehicle.class);
        when(vehicle.getPassengers()).thenReturn(List.of(player));
        return vehicle;
    }

    @Test
    void reducesDamageOfTheRiddenMount() {
        var player = mock(Player.class);
        var vehicle = vehicleRiddenBy(player);
        var event = damageOn(vehicle, 40.0);
        assertTrue(new MountedWardMechanic().execute(player, Map.of("reduction", 25.0), event));
        verify(event).setDamage(30.0);
    }

    @Test
    void fullReductionLeavesNoDamage() {
        var player = mock(Player.class);
        var vehicle = vehicleRiddenBy(player);
        var event = damageOn(vehicle, 40.0);
        assertTrue(new MountedWardMechanic().execute(player, Map.of("reduction", 100.0), event));
        verify(event).setDamage(0.0);
    }

    @Test
    void returnsFalseWhenNotRidingTheDamagedVehicle() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        var vehicle = vehicleRiddenBy(other); // ridden by someone else
        assertFalse(new MountedWardMechanic().execute(player, Map.of("reduction", 25.0), damageOn(vehicle, 40.0)));
    }

    @Test
    void returnsFalseForNonVehicleDamage() {
        var player = mock(Player.class);
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player); // a player, not a vehicle
        when(event.getDamage()).thenReturn(40.0);
        assertFalse(new MountedWardMechanic().execute(player, Map.of("reduction", 25.0), event));
    }

    @Test
    void returnsFalseWhenNotRiding() {
        var player = mock(Player.class);
        var vehicle = mock(Vehicle.class);
        when(vehicle.getPassengers()).thenReturn(List.of());
        assertFalse(new MountedWardMechanic().execute(player, Map.of("reduction", 25.0), damageOn(vehicle, 40.0)));
    }

    @Test
    void returnsFalseForNegativeReduction() {
        var player = mock(Player.class);
        var vehicle = vehicleRiddenBy(player);
        assertFalse(new MountedWardMechanic().execute(player, Map.of("reduction", -1.0), damageOn(vehicle, 40.0)));
    }
}