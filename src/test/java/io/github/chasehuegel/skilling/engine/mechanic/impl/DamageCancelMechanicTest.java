package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the shared chance-roll semantics of {@link DamageCancelMechanic}
 * (registered under {@code core:dodge}, {@code core:block_damage}, and
 * {@code core:cancel_damage}): reaching the roll counts as an activation attempt
 * so a failed roll cannot be retried for free.
 */
class DamageCancelMechanicTest {

    @AfterEach
    void tearDown() {
        DamageCancelMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    private EntityDamageEvent damageOn(Player player) {
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        return event;
    }

    @Test
    void returnsFalseForNonDamageEvent() {
        var player = mock(Player.class);
        assertFalse(new DamageCancelMechanic().execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWhenDamageNotOnPlayer() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        assertFalse(new DamageCancelMechanic().execute(player, Map.of("chance", 100.0), damageOn(other)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var player = mock(Player.class);
        assertFalse(new DamageCancelMechanic().execute(player, Map.of(), damageOn(player)));
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        DamageCancelMechanic.setRandomSource(() -> 99.0);
        var player = mock(Player.class);
        var event = damageOn(player);

        var mechanic = new DamageCancelMechanic();
        assertTrue(mechanic.execute(player, Map.of("chance", 50.0), event));
        verify(event, never()).setCancelled(true);
    }

    @Test
    void failedRollReportsNoProc() {
        DamageCancelMechanic.setRandomSource(() -> 99.0);
        var player = mock(Player.class);
        var mechanic = new DamageCancelMechanic();
        mechanic.execute(player, Map.of("chance", 50.0), damageOn(player));

        assertFalse(mechanic.didProc(), "a missed roll must not report a proc");
    }

    @Test
    void successfulRollCancelsDamage() {
        DamageCancelMechanic.setRandomSource(() -> 10.0);
        var player = mock(Player.class);
        var event = damageOn(player);

        var mechanic = new DamageCancelMechanic();
        assertTrue(mechanic.execute(player, Map.of("chance", 50.0), event));
        verify(event).setCancelled(true);
    }

    @Test
    void successfulRollReportsProc() {
        DamageCancelMechanic.setRandomSource(() -> 10.0);
        var player = mock(Player.class);
        var mechanic = new DamageCancelMechanic();
        mechanic.execute(player, Map.of("chance", 50.0), damageOn(player));

        assertTrue(mechanic.didProc(), "a landed roll must report a proc");
    }

    @Test
    void noOpExecutionReportsNoProc() {
        var player = mock(Player.class);
        var mechanic = new DamageCancelMechanic();
        // Wrong event type: execute returns false and nothing should have procced.
        assertFalse(mechanic.execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
        assertFalse(mechanic.didProc());
    }

    @Test
    void chanceHundredAlwaysCancels() {
        var player = mock(Player.class);
        var event = damageOn(player);
        assertTrue(new DamageCancelMechanic().execute(player, Map.of("chance", 100.0), event));
        verify(event).setCancelled(true);
    }
}
