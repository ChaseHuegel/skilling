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

    @Test
    void returnsFalseWhenPlayerIsDamagedNotDamager() {
        var mechanic = new ModifyDamageMechanic();
        var player = BukkitMock.mockPlayer();
        var attacker = mock(org.bukkit.entity.Zombie.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getEntity()).thenReturn(player);

        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0), event),
                "a wrong trigger binding (player is the damaged entity) must not scale damage");
        verify(event, never()).setDamage(anyDouble());
    }

    @Test
    void projectileDamagerResolvesShooterAsThePlayer() {
        var mechanic = new ModifyDamageMechanic();
        var player = BukkitMock.mockPlayer();
        var arrow = mock(org.bukkit.entity.Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(arrow);
        when(event.getDamage()).thenReturn(10.0);

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        verify(event).setDamage(20.0);
    }
}
