package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.LifestealMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LifestealMechanicTest {

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new LifestealMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWhenDamagerDoesNotMatchPlayer() {
        var mechanic = new LifestealMechanic();
        var player = BukkitMock.mockPlayer();
        var otherPlayer = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(otherPlayer);
        assertFalse(mechanic.execute(player, Map.of("percentage", 50.0), event));
    }

    @Test
    void returnsFalseWithPercentageZero() {
        var mechanic = new LifestealMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockDamageEvent(player, 100.0)));
    }

    @Test
    void healsPlayerForPercentageOfDamage() {
        var mechanic = new LifestealMechanic();
        var player = BukkitMock.mockPlayer();
        var event = BukkitMock.mockDamageEvent(player, 100.0);
        assertTrue(mechanic.execute(player, Map.of("percentage", 50.0), event));
        // 50% of 100 damage = 50 heal, capped at the default max health (20) since
        // the mocked player has no MAX_HEALTH attribute.
        verify(player).setHealth(20.0);
    }
}
