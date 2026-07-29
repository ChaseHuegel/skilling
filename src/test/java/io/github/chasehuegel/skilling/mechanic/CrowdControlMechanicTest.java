package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.CrowdControlMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrowdControlMechanicTest {

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new CrowdControlMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("effect", "speed"), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWhenDamagerDoesntMatch() {
        var mechanic = new CrowdControlMechanic();
        var player = BukkitMock.mockPlayer();
        var event = BukkitMock.mockDamageEvent(BukkitMock.mockPlayer(), 5.0);
        assertFalse(mechanic.execute(player, Map.of("effect", "speed"), event));
    }

    @Test
    void returnsFalseWithNullEffect() {
        var mechanic = new CrowdControlMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void returnsFalseWithBlankEffect() {
        var mechanic = new CrowdControlMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("effect", ""), BukkitMock.mockDamageEvent(player, 10.0)));
    }
}
