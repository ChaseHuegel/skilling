package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ChainBreakMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChainBreakMechanicTest {

    @Test
    void returnsFalseForNonBlockBreakEvent() {
        var mechanic = new ChainBreakMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("chain_limit", 10), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void returnsFalseWithChainLimitZero() {
        var mechanic = new ChainBreakMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent(player)));
    }
}
