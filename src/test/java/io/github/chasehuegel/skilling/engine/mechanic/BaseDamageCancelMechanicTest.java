package io.github.chasehuegel.skilling.engine.mechanic;

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
 * Verifies the shared chance-roll semantics of {@link BaseDamageCancelMechanic}
 * (used by {@code core:dodge}, {@code core:block_damage}, and
 * {@code core:cancel_damage}): reaching the roll counts as an activation attempt
 * so a failed roll cannot be retried for free.
 */
class BaseDamageCancelMechanicTest {

    @AfterEach
    void tearDown() {
        BaseDamageCancelMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    private static final class FixedChance extends BaseDamageCancelMechanic {
        private final double chance;

        FixedChance(double chance) {
            this.chance = chance;
        }

        @Override
        protected double getChance(Map<String, Object> params) {
            return chance;
        }
    }

    private EntityDamageEvent damageOn(Player player) {
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        return event;
    }

    @Test
    void returnsFalseForNonDamageEvent() {
        var player = mock(Player.class);
        assertFalse(new FixedChance(100).execute(player, Map.of(),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWhenDamageNotOnPlayer() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        assertFalse(new FixedChance(100).execute(player, Map.of(), damageOn(other)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var player = mock(Player.class);
        assertFalse(new FixedChance(0).execute(player, Map.of(), damageOn(player)));
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        BaseDamageCancelMechanic.setRandomSource(() -> 99.0);
        var player = mock(Player.class);
        var event = damageOn(player);

        assertTrue(new FixedChance(50).execute(player, Map.of(), event));
        verify(event, never()).setCancelled(true);
    }

    @Test
    void successfulRollCancelsDamage() {
        BaseDamageCancelMechanic.setRandomSource(() -> 10.0);
        var player = mock(Player.class);
        var event = damageOn(player);

        assertTrue(new FixedChance(50).execute(player, Map.of(), event));
        verify(event).setCancelled(true);
    }
}
