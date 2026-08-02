package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityDamageResolverTest {

    @Test
    void meleePlayerDamagerResolvesToThatPlayer() {
        var player = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        assertSame(player, EntityDamageResolver.resolveDamagerPlayer(event));
    }

    @Test
    void projectileResolvesShooterAsPlayer() {
        var player = mock(Player.class);
        var arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(arrow);
        assertSame(player, EntityDamageResolver.resolveDamagerPlayer(event));
    }

    @Test
    void mobProjectileResolvesToNullPlayer() {
        var zombie = mock(Zombie.class);
        var snowball = mock(Snowball.class);
        when(snowball.getShooter()).thenReturn(zombie);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(snowball);
        assertNull(EntityDamageResolver.resolveDamagerPlayer(event));
    }

    @Test
    void projectileResolvesShooterAsLivingEntity() {
        var zombie = mock(Zombie.class);
        var arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(zombie);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(arrow);
        assertSame(zombie, EntityDamageResolver.resolveDamagerEntity(event));
    }

    @Test
    void nonProjectileNonLivingDamagerResolvesToNull() {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(mock(org.bukkit.entity.Item.class));
        assertNull(EntityDamageResolver.resolveDamagerPlayer(event));
        assertNull(EntityDamageResolver.resolveDamagerEntity(event));
    }
}
