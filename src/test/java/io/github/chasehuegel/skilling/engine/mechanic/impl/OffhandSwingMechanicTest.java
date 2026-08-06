package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies {@code core:offhand_swing} plays the off-hand swing animation and
 * nothing else, regardless of the triggering event.
 */
class OffhandSwingMechanicTest {

    private final OffhandSwingMechanic mechanic = new OffhandSwingMechanic();

    @Test
    void executePlaysOffHandSwingAnimation() {
        Player player = mock(Player.class);

        assertTrue(mechanic.execute(player, Map.of(), mock(Event.class)));
        verify(player).swingOffHand();
    }

    @Test
    void executeIgnoresParamsAndEventType() {
        Player player = mock(Player.class);
        Map<String, Object> params = Map.of("unexpected", "value");

        assertTrue(mechanic.execute(player, params, mock(Event.class)));
        verify(player).swingOffHand();
    }
}
