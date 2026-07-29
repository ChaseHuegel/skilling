package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.KnockbackResistMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KnockbackResistMechanicTest {

    @Test
    void returnsFalseWithAmountZero() {
        var mechanic = new KnockbackResistMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("amount", 0.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithNegativeAmount() {
        var mechanic = new KnockbackResistMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("amount", -1.0), BukkitMock.mockBlockBreakEvent(player)));
    }
}
