package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
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
 * Verifies {@link DurabilitySaveMechanic}: reaching the chance roll counts as an
 * activation attempt, so a failed roll cannot be retried for free; a mechanic
 * that could not act at all is a true no-op.
 */
class DurabilitySaveMechanicTest {

    @AfterEach
    void tearDown() {
        DurabilitySaveMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    private PlayerItemDamageEvent damageOn(Player player) {
        var event = mock(PlayerItemDamageEvent.class);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    @Test
    void returnsFalseForNonItemDamageEvent() {
        var player = mock(Player.class);
        assertFalse(new DurabilitySaveMechanic().execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWhenDamageNotOnPlayer() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        assertFalse(new DurabilitySaveMechanic().execute(player, Map.of("chance", 100.0), damageOn(other)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var player = mock(Player.class);
        assertFalse(new DurabilitySaveMechanic().execute(player, Map.of(), damageOn(player)));
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        DurabilitySaveMechanic.setRandomSource(() -> 99.0);
        var player = mock(Player.class);
        var event = damageOn(player);

        assertTrue(new DurabilitySaveMechanic().execute(player, Map.of("chance", 50.0), event));
        verify(event, never()).setCancelled(true);
    }

    @Test
    void successfulRollNegatesDurabilityDamage() {
        DurabilitySaveMechanic.setRandomSource(() -> 10.0);
        var player = mock(Player.class);
        var event = damageOn(player);

        assertTrue(new DurabilitySaveMechanic().execute(player, Map.of("chance", 50.0), event));
        verify(event).setCancelled(true);
    }
}
