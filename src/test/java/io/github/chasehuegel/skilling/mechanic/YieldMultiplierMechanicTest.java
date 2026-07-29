package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.YieldMultiplierMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YieldMultiplierMechanicTest {

    @Test
    void returnsFalseForNonBlockBreakEvent() {
        var mechanic = new YieldMultiplierMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var mechanic = new YieldMultiplierMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent(player)));
    }
}
