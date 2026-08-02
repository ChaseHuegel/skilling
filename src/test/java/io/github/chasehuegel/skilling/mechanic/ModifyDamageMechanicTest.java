package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyDamageMechanic;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModifyDamageMechanicTest {

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new ModifyDamageMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithMultiplierZero() {
        var mechanic = new ModifyDamageMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 0.0), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void returnsTrueWithValidMultiplier() {
        var mechanic = new ModifyDamageMechanic();
        var player = BukkitMock.mockPlayer();
        var event = BukkitMock.mockDamageEvent(player, 10.0);
        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        // The mechanic must actually scale the incoming damage, not just return true.
        verify(event).setDamage(20.0);
    }
}
