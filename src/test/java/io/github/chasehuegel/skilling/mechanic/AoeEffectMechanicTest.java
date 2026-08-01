package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.AoeEffectMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AoeEffectMechanicTest {

    @Test
    void throwsOnBlankEffect() {
        var mechanic = new AoeEffectMechanic();
        var player = BukkitMock.mockPlayer();
        assertThrows(IllegalArgumentException.class,
                () -> mechanic.execute(player, Map.of("effect", ""), BukkitMock.mockBlockBreakEvent(player)));
    }

}
