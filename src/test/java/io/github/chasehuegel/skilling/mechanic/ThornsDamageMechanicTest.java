package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ThornsDamageMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ThornsDamageMechanicTest {

    private static EntityDamageByEntityEvent damageEvent(LivingEntity damaged, org.bukkit.entity.Entity damager) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(damaged);
        when(event.getDamager()).thenReturn(damager);
        return event;
    }

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new ThornsDamageMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent()));
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

    @Test
    void projectileDamagerResolvesShooterAsAttacker() {
        var mechanic = new ThornsDamageMechanic();
        var player = BukkitMock.mockPlayer();
        var zombie = mock(org.bukkit.entity.Zombie.class);
        var arrow = mock(org.bukkit.entity.Arrow.class);
        when(arrow.getShooter()).thenReturn(zombie);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getDamager()).thenReturn(arrow);

        assertTrue(mechanic.execute(player, Map.of("damage", 5.0), event));
        verify(zombie).damage(5.0, player);
    }

    @Test
    void twoReflectingEntitiesTerminateWithoutRecursion() {
        var mechanic = new ThornsDamageMechanic();
        var a = mock(Player.class);
        var b = mock(Player.class);

        // Simulate the re-entrant pipeline: damaging an entity synchronously
        // fires its damage event, which re-runs the reflect mechanic for it.
        // Without the re-entrancy guard this ping-pongs until StackOverflowError.
        doAnswer(inv -> {
            mechanic.execute(a, Map.of("damage", 5.0), damageEvent(a, b));
            return null;
        }).when(a).damage(anyDouble(), eq(b));
        doAnswer(inv -> {
            mechanic.execute(b, Map.of("damage", 5.0), damageEvent(b, a));
            return null;
        }).when(b).damage(anyDouble(), eq(a));

        mechanic.execute(b, Map.of("damage", 5.0), damageEvent(b, a));

        // B reflects onto A, A reflects back onto B once, and the cycle stops.
        verify(a).damage(anyDouble(), eq(b));
        verify(b).damage(anyDouble(), eq(a));
    }

    @Test
    void selfReflectTerminatesAfterSingleHit() {
        var mechanic = new ThornsDamageMechanic();
        var a = mock(Player.class);
        AtomicInteger damageCalls = new AtomicInteger();
        doAnswer(inv -> {
            mechanic.execute(a, Map.of("damage", 5.0), damageEvent(a, a));
            damageCalls.incrementAndGet();
            return null;
        }).when(a).damage(anyDouble(), eq(a));

        mechanic.execute(a, Map.of("damage", 5.0), damageEvent(a, a));

        verify(a).damage(anyDouble(), eq(a));
        assertEquals(1, damageCalls.get(), "the reflected self-hit must not reflect again");
    }
}
