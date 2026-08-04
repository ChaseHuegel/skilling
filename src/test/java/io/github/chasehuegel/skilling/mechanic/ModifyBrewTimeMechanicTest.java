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

    @Test
    void executesOnBrewingStartEvent() {
        var mechanic = new ModifyBrewTimeMechanic();
        var player = BukkitMock.mockPlayer();
        var event = mock(BrewingStartEvent.class);
        when(event.getBrewingTime()).thenReturn(400);

        assertTrue(mechanic.execute(player, Map.of("multiplier", 0.5), event));
        verify(event).setBrewingTime(200);
    }

    @Test
    void doesNotExecuteOnBrewEvent() {
        var mechanic = new ModifyBrewTimeMechanic();
        var player = BukkitMock.mockPlayer();
        // The mechanic acts only on BrewingStartEvent; a BrewEvent (the brew_potion
        // trigger) must be a no-op so abilities must pair with brew_start.
        assertFalse(mechanic.execute(player, Map.of("multiplier", 0.5),
                mock(org.bukkit.event.inventory.BrewEvent.class)));
    }
}
