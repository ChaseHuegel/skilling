package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ApplyStatusMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplyStatusMechanicTest {

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new ApplyStatusMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("effect", "speed"), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWhenDamagerNotPlayer() {
        var mechanic = new ApplyStatusMechanic();
        var player = BukkitMock.mockPlayer();
        var otherPlayer = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(otherPlayer);
        assertFalse(mechanic.execute(player, Map.of("effect", "speed"), event));
    }

    @Test
    void returnsFalseWithBlankEffectParam() {
        var mechanic = new ApplyStatusMechanic();
        var player = BukkitMock.mockPlayer();
        var event = BukkitMock.mockDamageEvent(player, 10.0);
        assertFalse(mechanic.execute(player, Map.of(), event));
    }
}
