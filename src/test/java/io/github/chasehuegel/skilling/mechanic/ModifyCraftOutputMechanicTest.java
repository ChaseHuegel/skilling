package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyCraftOutputMechanic;
import org.bukkit.event.inventory.CraftItemEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModifyCraftOutputMechanicTest {

    @Test
    void returnsFalseForNonCraftEvent() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithMultiplierOne() {
        var mechanic = new ModifyCraftOutputMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), mock(CraftItemEvent.class)));
    }
}
