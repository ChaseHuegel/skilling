package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.CancelDamageMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancelDamageMechanicTest {

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new CancelDamageMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("chance", 100.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWhenEntityNotPlayer() {
        var mechanic = new CancelDamageMechanic();
        var player = BukkitMock.mockPlayer();
        var otherPlayer = mock(Player.class);
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(otherPlayer);
        assertFalse(mechanic.execute(player, Map.of("chance", 100.0), event));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var mechanic = new CancelDamageMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void returnsTrueWithChanceHundred() {
        var mechanic = new CancelDamageMechanic();
        var player = BukkitMock.mockPlayer();
        var event = BukkitMock.mockDamageEvent(player, 10.0);
        assertTrue(mechanic.execute(player, Map.of("chance", 100.0), event));
    }
}
