package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.SaturationInjectMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SaturationInjectMechanicTest {

    @Test
    void returnsFalseForNonConsumeEvent() {
        var mechanic = new SaturationInjectMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("saturation", 5.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithSaturationZero() {
        var mechanic = new SaturationInjectMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockConsumeEvent(player)));
    }

    @Test
    void returnsTrueWithValidSaturation() {
        var mechanic = new SaturationInjectMechanic();
        var player = BukkitMock.mockPlayer();
        when(player.getSaturation()).thenReturn(5.0f);
        assertTrue(mechanic.execute(player, Map.of("saturation", 3.0), BukkitMock.mockConsumeEvent(player)));
        verify(player).setSaturation(8.0f);
    }
}
