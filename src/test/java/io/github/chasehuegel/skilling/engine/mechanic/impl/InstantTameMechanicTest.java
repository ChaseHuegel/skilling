package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the tame-on-right-click semantics of {@link InstantTameMechanic}:
 * an untamed tameable is owned on a successful roll, an already-tamed target is
 * a no-op, and a failed roll still counts as an activation attempt.
 */
class InstantTameMechanicTest {

    @AfterEach
    void tearDown() {
        InstantTameMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    private static PlayerInteractEntityEvent eventClicking(Tameable tameable) {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(tameable);
        return event;
    }

    @Test
    void tamesUntamedTameableOnRightClick() {
        InstantTameMechanic.setRandomSource(() -> 0.0);
        var mechanic = new InstantTameMechanic();
        var player = mock(Player.class);
        var tameable = mock(Tameable.class);
        when(tameable.isTamed()).thenReturn(false);
        var event = eventClicking(tameable);

        assertTrue(mechanic.execute(player, Map.of("chance", 100.0), event));
        verify(event).setCancelled(true);
        verify(tameable).setOwner(player);
        verify(tameable).setTamed(true);
    }

    @Test
    void alreadyTamedIsNoOp() {
        var mechanic = new InstantTameMechanic();
        var tameable = mock(Tameable.class);
        when(tameable.isTamed()).thenReturn(true);
        var event = eventClicking(tameable);

        assertFalse(mechanic.execute(mock(Player.class), Map.of("chance", 100.0), event));
        verify(event, never()).setCancelled(true);
        verify(tameable, never()).setOwner(any());
        verify(tameable, never()).setTamed(true);
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        InstantTameMechanic.setRandomSource(() -> 99.0);
        var mechanic = new InstantTameMechanic();
        var tameable = mock(Tameable.class);
        when(tameable.isTamed()).thenReturn(false);
        var event = eventClicking(tameable);

        assertTrue(mechanic.execute(mock(Player.class), Map.of("chance", 50.0), event));
        // The interaction is still cancelled so the vanilla tame/feed cannot
        // double-consume the food; only the ability's cost is spent.
        verify(event).setCancelled(true);
        verify(tameable, never()).setOwner(any());
        verify(tameable, never()).setTamed(true);
    }
}
