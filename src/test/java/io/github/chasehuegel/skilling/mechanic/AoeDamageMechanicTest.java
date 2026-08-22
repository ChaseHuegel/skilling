package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.AoeDamageMechanic;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AoeDamageMechanicTest {

    private org.bukkit.entity.Player player = BukkitMock.mockPlayer();

    private EntityDamageByEntityEvent mockHit(LivingEntity victim, double damage) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamage()).thenReturn(damage);
        return event;
    }

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new AoeDamageMechanic();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.2), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWhenPlayerIsNotDamager() {
        var mechanic = new AoeDamageMechanic();
        var other = BukkitMock.mockPlayer();
        var victim = mock(LivingEntity.class);
        var event = mockHit(victim, 10.0);
        when(event.getDamager()).thenReturn(other);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.2), event));
        verify(victim, never()).getNearbyEntities(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void returnsFalseWithNonPositiveMultiplier() {
        var mechanic = new AoeDamageMechanic();
        var victim = mock(LivingEntity.class);
        var event = mockHit(victim, 10.0);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 0.0), event));
    }

    @Test
    void damagesNearbyHostilesButNotPrimaryTargetOrPlayers() {
        var mechanic = new AoeDamageMechanic();
        var victim = mock(LivingEntity.class);
        var zombie = mock(Zombie.class);
        var allyPlayer = mock(Player.class);
        var event = mockHit(victim, 10.0);
        when(victim.getNearbyEntities(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(zombie, player, allyPlayer, victim));

        assertTrue(mechanic.execute(player, Map.of("radius", 3.0, "multiplier", 1.2), event));
        verify(zombie).damage(12.0, player);
        verify(victim, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }

    @Test
    void filtersByTargetsAll() {
        var mechanic = new AoeDamageMechanic();
        var victim = mock(LivingEntity.class);
        var cow = mock(org.bukkit.entity.Cow.class);
        var event = mockHit(victim, 10.0);
        when(victim.getNearbyEntities(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(cow));

        assertTrue(mechanic.execute(player, Map.of("radius", 2.0, "multiplier", 1.0, "targets", "all"), event));
        verify(cow).damage(10.0, player);
    }

    @Test
    void returnsFalseWhenNoAdjacentFoe() {
        var mechanic = new AoeDamageMechanic();
        var victim = mock(LivingEntity.class);
        var event = mockHit(victim, 10.0);
        when(victim.getNearbyEntities(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of());
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.2), event));
    }
}