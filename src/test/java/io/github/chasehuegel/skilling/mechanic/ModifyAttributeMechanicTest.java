package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyAttributeMechanic;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModifyAttributeMechanicTest {

    @Test
    void throwsOnBlankAttribute() {
        var mechanic = new ModifyAttributeMechanic();
        var player = BukkitMock.mockPlayer();
        assertThrows(IllegalArgumentException.class,
                () -> mechanic.execute(player, Map.of("attribute", ""), BukkitMock.mockBlockBreakEvent(player)));
    }
}
