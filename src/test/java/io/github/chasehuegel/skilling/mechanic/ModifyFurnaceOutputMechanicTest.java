package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyFurnaceOutputMechanic;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModifyFurnaceOutputMechanicTest {

    @Test
    void returnsFalseForNonFurnaceEvent() {
        var mechanic = new ModifyFurnaceOutputMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithMultiplierOne() {
        var mechanic = new ModifyFurnaceOutputMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), mock(FurnaceExtractEvent.class)));
    }
}
