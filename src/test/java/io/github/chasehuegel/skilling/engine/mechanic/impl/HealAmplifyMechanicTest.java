package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link HealAmplifyMechanic} multiplies event-driven health
 * regain and no-ops when the event is not the activating player's recovery.
 */
class HealAmplifyMechanicTest {

    @Test
    void multipliesRegainedHealth() {
        var mechanic = new HealAmplifyMechanic();
        var player = mock(Player.class);
        var heal = mock(EntityRegainHealthEvent.class);
        when(heal.isCancelled()).thenReturn(false);
        when(heal.getEntity()).thenReturn(player);
        when(heal.getAmount()).thenReturn(4.0);

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), heal));
        verify(heal).setAmount(8.0);
    }

    @Test
    void ignoresHealOfAnotherEntity() {
        var mechanic = new HealAmplifyMechanic();
        var player = mock(Player.class);
        var other = mock(Player.class);
        var heal = mock(EntityRegainHealthEvent.class);
        when(heal.isCancelled()).thenReturn(false);
        when(heal.getEntity()).thenReturn(other);

        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0), heal));
        verify(heal, never()).setAmount(org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void noOpForDefaultMultiplier() {
        var mechanic = new HealAmplifyMechanic();
        var player = mock(Player.class);
        var heal = mock(EntityRegainHealthEvent.class);
        when(heal.isCancelled()).thenReturn(false);
        when(heal.getEntity()).thenReturn(player);

        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), heal));
        verify(heal, never()).setAmount(org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void returnsFalseForNonHealEvent() {
        var mechanic = new HealAmplifyMechanic();
        assertFalse(mechanic.execute(mock(Player.class), Map.of("multiplier", 2.0), mock(org.bukkit.event.Event.class)));
    }
}