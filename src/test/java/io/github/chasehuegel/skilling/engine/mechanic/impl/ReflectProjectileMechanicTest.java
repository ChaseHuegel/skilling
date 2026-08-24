package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link ReflectProjectileMechanic}: it reflects a hostile projectile
 * back at its shooter and reports a proc when the chance roll lands, while a
 * no-op (non-projectile damage, player-owned projectile) never spends cost.
 */
class ReflectProjectileMechanicTest {

    @AfterEach
    void tearDown() {
        ReflectProjectileMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    private EntityDamageByEntityEvent projectileHitOn(Player player, Projectile projectile) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getDamager()).thenReturn(projectile);
        return event;
    }

    @Test
    void returnsFalseForNonDamageEvent() {
        var player = mock(Player.class);
        assertFalse(new ReflectProjectileMechanic().execute(player, Map.of("chance", 100.0, "damage", 4.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWhenDamageNotOnPlayer() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        var projectile = mock(Projectile.class);
        assertFalse(new ReflectProjectileMechanic().execute(player, Map.of("chance", 100.0, "damage", 4.0),
                projectileHitOn(other, projectile)));
    }

    @Test
    void returnsFalseWhenDamagerIsNotProjectile() {
        var player = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getDamager()).thenReturn(mock(org.bukkit.entity.Zombie.class));
        assertFalse(new ReflectProjectileMechanic().execute(player, Map.of("chance", 100.0, "damage", 4.0), event));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var player = mock(Player.class);
        var projectile = mock(Projectile.class);
        assertFalse(new ReflectProjectileMechanic().execute(player, Map.of(), projectileHitOn(player, projectile)));
    }

    @Test
    void ignoresPlayerOwnedProjectile() {
        var player = mock(Player.class);
        var projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(player); // shooter is the player -> no self-rebound
        assertFalse(new ReflectProjectileMechanic().execute(player, Map.of("chance", 100.0, "damage", 4.0),
                projectileHitOn(player, projectile)));
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        ReflectProjectileMechanic.setRandomSource(() -> 99.0);
        var player = mock(Player.class);
        var attacker = mock(LivingEntity.class);
        var projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(attacker);

        var mechanic = new ReflectProjectileMechanic();
        assertTrue(mechanic.execute(player, Map.of("chance", 50.0, "damage", 4.0),
                projectileHitOn(player, projectile)));
        verify(projectile, never()).setVelocity(org.mockito.ArgumentMatchers.any());
        verify(attacker, never()).damage(org.mockito.ArgumentMatchers.anyDouble(), (org.bukkit.entity.Entity) org.mockito.ArgumentMatchers.any());
    }

    @Test
    void failedRollReportsNoProc() {
        ReflectProjectileMechanic.setRandomSource(() -> 99.0);
        var player = mock(Player.class);
        var attacker = mock(LivingEntity.class);
        var projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(attacker);

        var mechanic = new ReflectProjectileMechanic();
        mechanic.execute(player, Map.of("chance", 50.0, "damage", 4.0), projectileHitOn(player, projectile));
        assertFalse(mechanic.didProc());
    }

    @Test
    void successfulRollReboundsAndDamagesShooter() {
        ReflectProjectileMechanic.setRandomSource(() -> 10.0);
        var player = mock(Player.class);
        var attacker = mock(LivingEntity.class);
        var projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(attacker);
        when(projectile.getVelocity()).thenReturn(new org.bukkit.util.Vector(1, 0, 0));

        var mechanic = new ReflectProjectileMechanic();
        assertTrue(mechanic.execute(player, Map.of("chance", 50.0, "damage", 4.0),
                projectileHitOn(player, projectile)));
        verify(projectile).setVelocity(new org.bukkit.util.Vector(-1, 0, 0));
        verify(attacker).damage(4.0d, (org.bukkit.entity.Entity) player);
        assertTrue(mechanic.didProc());
    }
}