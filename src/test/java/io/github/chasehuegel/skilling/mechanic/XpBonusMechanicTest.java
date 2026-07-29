package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XpBonusMechanicTest {

    @Test
    void returnsFalseWithMultiplierZero() {
        var mechanic = new XpBonusMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 0.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithNegativeMultiplier() {
        var mechanic = new XpBonusMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", -1.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsTrueWithMultiplierTwo() {
        var mechanic = new XpBonusMechanic();
        var player = BukkitMock.mockPlayer();
        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void getMultiplierReturnsCorrectValueAfterExecution() {
        var mechanic = new XpBonusMechanic();
        var player = BukkitMock.mockPlayer();
        var uuid = player.getUniqueId();
        assertEquals(1.0, XpBonusMechanic.getMultiplier(uuid), 1e-9);
        mechanic.execute(player, Map.of("multiplier", 3.0), BukkitMock.mockBlockBreakEvent(player));
        assertEquals(3.0, XpBonusMechanic.getMultiplier(uuid), 1e-9);
    }
}
