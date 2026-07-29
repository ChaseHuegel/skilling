package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ThornsDamageMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ThornsDamageMechanicTest {

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new ThornsDamageMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWhenEntityIsNotPlayer() {
        var mechanic = new ThornsDamageMechanic();
        var player = BukkitMock.mockPlayer();
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(mock(Player.class));
        assertFalse(mechanic.execute(player, Map.of("damage", 5.0), event));
    }

    @Test
    void returnsFalseWithDamageZero() {
        var mechanic = new ThornsDamageMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void returnsTrueWithValidDamage() {
        var mechanic = new ThornsDamageMechanic();
        var player = BukkitMock.mockPlayer();
        var attacker = mock(LivingEntity.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getDamager()).thenReturn(attacker);
        assertTrue(mechanic.execute(player, Map.of("damage", 5.0), event));
        verify(attacker).damage(5.0, player);
    }
}
