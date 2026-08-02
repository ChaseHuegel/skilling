package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.BukkitMock;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTameEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModifyTameChanceMechanicTest {

    @AfterEach
    void tearDown() {
        ModifyTameChanceMechanic.setRandomSource(() -> java.util.concurrent.ThreadLocalRandom.current().nextDouble());
    }

    private EntityTameEvent tameEvent() {
        return mock(EntityTameEvent.class);
    }

    @Test
    void returnsFalseForNonTameEvent() {
        var player = BukkitMock.mockPlayer();
        assertFalse(new ModifyTameChanceMechanic().execute(player, Map.of("multiplier", 1.0),
                BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void returnsFalseWithNonPositiveMultiplier() {
        var player = BukkitMock.mockPlayer();
        assertFalse(new ModifyTameChanceMechanic().execute(player, Map.of("multiplier", 0.0), tameEvent()));
    }

    @Test
    void multiplierAboveOneNeverCancelsASuccessfulTame() {
        // The old logic cancelled with probability 1 - 1/multiplier (50% at 2.0),
        // making higher multipliers worse. A roll of 0.999 must not cancel.
        ModifyTameChanceMechanic.setRandomSource(() -> 0.999);
        var player = BukkitMock.mockPlayer();
        var event = tameEvent();

        assertTrue(new ModifyTameChanceMechanic().execute(player, Map.of("multiplier", 2.0), event));
        verify(event, never()).setCancelled(true);
    }

    @Test
    void multiplierOneLeavesTameUntouched() {
        ModifyTameChanceMechanic.setRandomSource(() -> 0.0);
        var player = BukkitMock.mockPlayer();
        var event = tameEvent();

        assertTrue(new ModifyTameChanceMechanic().execute(player, Map.of("multiplier", 1.0), event));
        verify(event, never()).setCancelled(true);
    }

    @Test
    void multiplierBelowOneCancelsWithProbabilityOneMinusMultiplier() {
        // Deterministic check with a controllable roll: for multiplier 0.5, a roll
        // above 0.5 cancels and a roll at/below 0.5 keeps the success.
        ModifyTameChanceMechanic.setRandomSource(() -> 0.9);
        var player = BukkitMock.mockPlayer();
        var event = tameEvent();
        assertTrue(new ModifyTameChanceMechanic().execute(player, Map.of("multiplier", 0.5), event));
        verify(event).setCancelled(true);

        ModifyTameChanceMechanic.setRandomSource(() -> 0.1);
        var kept = tameEvent();
        assertTrue(new ModifyTameChanceMechanic().execute(player, Map.of("multiplier", 0.5), kept));
        verify(kept, never()).setCancelled(true);
    }

    @Test
    void higherMultiplierNeverCancelsMoreOftenThanLower() {
        // Frequency check: multiplier 2.0 with an adversarial roll of 0.999 must
        // never cancel; multiplier 0.5 cancels about half the time over a sample.
        ModifyTameChanceMechanic.setRandomSource(() -> 0.999);
        var player = BukkitMock.mockPlayer();
        for (int i = 0; i < 100; i++) {
            var event = tameEvent();
            new ModifyTameChanceMechanic().execute(player, Map.of("multiplier", 2.0), event);
            verify(event, never()).setCancelled(true);
        }
    }
}
