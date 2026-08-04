package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:saturation_inject} clamps to the vanilla saturation cap
 * (20) instead of letting the value exceed it.
 */
class SaturationInjectMechanicTest {

    @Test
    void addsSaturationBelowCap() {
        var mechanic = new SaturationInjectMechanic();
        var player = mock(Player.class);
        when(player.getSaturation()).thenReturn(5.0f);
        var event = mock(PlayerItemConsumeEvent.class);

        assertTrue(mechanic.execute(player, Map.of("saturation", 3.0), event));
        verify(player).setSaturation(8.0f);
    }

    @Test
    void clampsSaturationToVanillaCap() {
        var mechanic = new SaturationInjectMechanic();
        var player = mock(Player.class);
        when(player.getSaturation()).thenReturn(15.0f);
        var event = mock(PlayerItemConsumeEvent.class);

        assertTrue(mechanic.execute(player, Map.of("saturation", 10.0), event));
        verify(player).setSaturation(20.0f);
    }

    @Test
    void returnsFalseForNonConsumeEvent() {
        var mechanic = new SaturationInjectMechanic();
        var player = mock(Player.class);
        assertFalse(mechanic.execute(player, Map.of("saturation", 5.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }
}
