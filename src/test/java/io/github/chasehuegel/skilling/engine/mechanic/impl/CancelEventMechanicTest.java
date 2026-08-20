package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Verifies {@code core:cancel_event}: an absent chance cancels unconditionally,
 * a present chance gates the cancel like the chance mechanics, and a rolled
 * attempt counts as an activation so a failed roll cannot be retried for free.
 */
class CancelEventMechanicTest {

    @AfterEach
    void tearDown() {
        CancelEventMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    private static Event cancellableEvent() {
        return mock(Event.class, withSettings().extraInterfaces(Cancellable.class));
    }

    @Test
    void returnsFalseForNonCancellableEvent() {
        assertFalse(new CancelEventMechanic().execute(mock(Player.class), Map.of(), mock(Event.class)));
    }

    @Test
    void returnsFalseWithNonPositiveChance() {
        assertFalse(new CancelEventMechanic().execute(mock(Player.class), Map.of("chance", 0.0),
                cancellableEvent()));
    }

    @Test
    void absentChanceCancelsUnconditionally() {
        var event = cancellableEvent();
        assertTrue(new CancelEventMechanic().execute(mock(Player.class), Map.of(), event));
        verify((Cancellable) event).setCancelled(true);
    }

    @Test
    void chanceHundredAlwaysCancels() {
        var event = cancellableEvent();
        assertTrue(new CancelEventMechanic().execute(mock(Player.class), Map.of("chance", 100.0), event));
        verify((Cancellable) event).setCancelled(true);
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        CancelEventMechanic.setRandomSource(() -> 99.0);
        var event = cancellableEvent();
        var mechanic = new CancelEventMechanic();
        assertTrue(mechanic.execute(mock(Player.class), Map.of("chance", 50.0), event),
                "a failed roll still counts as an activation attempt");
        verify((Cancellable) event, never()).setCancelled(true);
        assertFalse(mechanic.didProc());
    }

    @Test
    void successfulRollCancelsAndReportsProc() {
        CancelEventMechanic.setRandomSource(() -> 10.0);
        var event = cancellableEvent();
        var mechanic = new CancelEventMechanic();
        assertTrue(mechanic.execute(mock(Player.class), Map.of("chance", 50.0), event));
        verify((Cancellable) event).setCancelled(true);
        assertTrue(mechanic.didProc());
    }

    @Test
    void nonCancellableNoOpReportsNoProc() {
        var mechanic = new CancelEventMechanic();
        assertFalse(mechanic.execute(mock(Player.class), Map.of("chance", 100.0), mock(Event.class)));
        assertFalse(mechanic.didProc());
    }
}
