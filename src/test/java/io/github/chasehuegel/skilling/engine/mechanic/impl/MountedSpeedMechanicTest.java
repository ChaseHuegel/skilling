package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Boat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link MountedSpeedMechanic}: it boosts the movement speed of a
 * living mount the player rides, and is a no-op when the player rides no living
 * mount (boats and minecarts have no speed attribute).
 */
class MountedSpeedMechanicTest {

    @AfterEach
    void clearTracker() {
        MountedSpeedMechanic.clearAll();
    }

    private LivingEntity livingMount(double baseSpeed) {
        LivingEntity mount = mock(LivingEntity.class);
        when(mount.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(mount.getScheduler()).thenReturn(mock(io.papermc.paper.threadedregions.scheduler.EntityScheduler.class));
        AttributeInstance inst = mock(AttributeInstance.class);
        when(inst.getBaseValue()).thenReturn(baseSpeed);
        when(mount.getAttribute(Attribute.MOVEMENT_SPEED)).thenReturn(inst);
        return mount;
    }

    @Test
    void boostsTheRiddenMountsSpeed() {
        Player player = mock(Player.class);
        LivingEntity mount = livingMount(0.25);
        when(player.getVehicle()).thenReturn(mount);
        AttributeInstance inst = mount.getAttribute(Attribute.MOVEMENT_SPEED);

        assertTrue(new MountedSpeedMechanic().execute(player,
                Map.of("multiplier", 1.1, "uuid", "b4f2c9d1-6a3e-4b5c-8d7f-9c1a2e3b4d5f"), mock(Event.class)));
        verify(inst).addTransientModifier(any());
    }

    @Test
    void noOpWhenNotRidingAnything() {
        Player player = mock(Player.class);
        when(player.getVehicle()).thenReturn(null);
        assertFalse(new MountedSpeedMechanic().execute(player,
                Map.of("multiplier", 1.1), mock(Event.class)));
    }

    @Test
    void noOpWhenRidingANonLivingVehicle() {
        Player player = mock(Player.class);
        when(player.getVehicle()).thenReturn(mock(Boat.class));
        assertFalse(new MountedSpeedMechanic().execute(player,
                Map.of("multiplier", 1.1), mock(Event.class)));
    }

    @Test
    void noOpWithNonPositiveMultiplier() {
        Player player = mock(Player.class);
        LivingEntity mount = livingMount(0.25);
        when(player.getVehicle()).thenReturn(mount);
        assertFalse(new MountedSpeedMechanic().execute(player,
                Map.of("multiplier", 1.0), mock(Event.class)));
        verify(mount.getAttribute(Attribute.MOVEMENT_SPEED), never()).addTransientModifier(any());
    }
}