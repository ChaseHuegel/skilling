package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link ReduceDamageMechanic}: it scales the activating player's
 * incoming {@link EntityDamageEvent} damage down by a flat percentage, is a
 * no-op when it cannot act, and clamps the reduction to at most 100%.
 */
class ReduceDamageMechanicTest {

    private EntityDamageEvent damageOn(Player player, double base) {
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getDamage()).thenReturn(base);
        return event;
    }

    @Test
    void reducesDamageByPercentage() {
        var player = mock(Player.class);
        var event = damageOn(player, 40.0);
        assertTrue(new ReduceDamageMechanic().execute(player, Map.of("reduction", 25.0), event));
        verify(event).setDamage(30.0);
    }

    @Test
    void fullReductionLeavesNoDamage() {
        var player = mock(Player.class);
        var event = damageOn(player, 40.0);
        assertTrue(new ReduceDamageMechanic().execute(player, Map.of("reduction", 100.0), event));
        verify(event).setDamage(0.0);
    }

    @Test
    void reductionIsClampedToOneHundredPercent() {
        var player = mock(Player.class);
        var event = damageOn(player, 40.0);
        assertTrue(new ReduceDamageMechanic().execute(player, Map.of("reduction", 200.0), event));
        verify(event).setDamage(0.0);
    }

    @Test
    void returnsFalseForNonDamageEvent() {
        var player = mock(Player.class);
        assertFalse(new ReduceDamageMechanic().execute(player, Map.of("reduction", 25.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWhenDamageNotOnPlayer() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        assertFalse(new ReduceDamageMechanic().execute(player, Map.of("reduction", 25.0), damageOn(other, 40.0)));
    }

    @Test
    void returnsFalseWithReductionZero() {
        var player = mock(Player.class);
        assertFalse(new ReduceDamageMechanic().execute(player, Map.of(), damageOn(player, 40.0)));
    }

    @Test
    void reductionIsDeterministic() {
        var player = mock(Player.class);
        var event = damageOn(player, 100.0);
        assertTrue(new ReduceDamageMechanic().execute(player, Map.of("reduction", 10.0), event));
        verify(event).setDamage(90.0);
    }
}