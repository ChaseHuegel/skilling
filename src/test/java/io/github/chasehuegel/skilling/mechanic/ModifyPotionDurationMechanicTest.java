package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyPotionDurationMechanic;
import org.bukkit.event.inventory.BrewEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModifyPotionDurationMechanicTest {

    @Test
    void returnsFalseForNonBrewEvent() {
        var mechanic = new ModifyPotionDurationMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithMultiplierZero() {
        var mechanic = new ModifyPotionDurationMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 0.0), mock(BrewEvent.class)));
    }
}
