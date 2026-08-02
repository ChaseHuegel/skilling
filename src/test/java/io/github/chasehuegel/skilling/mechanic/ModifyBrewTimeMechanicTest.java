package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyBrewTimeMechanic;
import org.bukkit.event.block.BrewingStartEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModifyBrewTimeMechanicTest {

    @Test
    void returnsFalseForNonBrewEvent() {
        var mechanic = new ModifyBrewTimeMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithMultiplierZero() {
        var mechanic = new ModifyBrewTimeMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 0.0), mock(BrewingStartEvent.class)));
    }
}
