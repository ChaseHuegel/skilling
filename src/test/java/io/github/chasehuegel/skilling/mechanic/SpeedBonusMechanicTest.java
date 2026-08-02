package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.SpeedBonusMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpeedBonusMechanicTest {

    @Test
    void returnsFalseWithMultiplierZero() {
        var mechanic = new SpeedBonusMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 0.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithNegativeMultiplier() {
        var mechanic = new SpeedBonusMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", -1.0), BukkitMock.mockBlockBreakEvent()));
    }
}
