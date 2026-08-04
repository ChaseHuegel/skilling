package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ExecuteMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecuteMechanicTest {

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("threshold", 50.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWhenDamagerNotPlayer() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        var otherPlayer = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(otherPlayer);
        assertFalse(mechanic.execute(player, Map.of("threshold", 50.0), event));
    }

    @Test
    void returnsFalseWithThresholdZero() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void executesTargetBelowThreshold() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        var target = mock(LivingEntity.class);
        when(target.getHealth()).thenReturn(5.0);
        // Alive before the blow (guard), dead after it (verification).
        when(target.isDead()).thenReturn(false, true);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(target);

        assertTrue(mechanic.execute(player, Map.of("threshold", 50.0), event));
        // 5/20 (default max health) = 25% <= 50%, so the target is killed
        // through the damage pipeline (attributed to the player, vanilla XP).
        verify(target).damage(Double.MAX_VALUE, player);
        verify(target, never()).setHealth(0);
    }

    @Test
    void leavesTargetAboveThresholdAlive() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        var target = mock(LivingEntity.class);
        when(target.getHealth()).thenReturn(15.0);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(target);

        assertFalse(mechanic.execute(player, Map.of("threshold", 50.0), event));
        verify(target, never()).setHealth(0);
    }

    @Test
    void survivingArmoredTargetDoesNotConsume() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        var target = mock(LivingEntity.class);
        when(target.getHealth()).thenReturn(5.0);
        when(target.isDead()).thenReturn(false); // armor/absorption reduced the blow
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(target);

        assertFalse(mechanic.execute(player, Map.of("threshold", 50.0), event),
                "a target that survives must not consume cost/cooldown");
        verify(target).damage(Double.MAX_VALUE, player);
    }

    @Test
    void deadTargetIsNoOp() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        var target = mock(LivingEntity.class);
        when(target.isDead()).thenReturn(true);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(target);

        assertFalse(mechanic.execute(player, Map.of("threshold", 50.0), event));
        verify(target, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }

    @Test
    void creativePlayerTargetIsNoOp() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        var target = mock(org.bukkit.entity.Player.class);
        when(target.getGameMode()).thenReturn(org.bukkit.GameMode.CREATIVE);
        when(target.getHealth()).thenReturn(2.0);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(target);

        assertFalse(mechanic.execute(player, Map.of("threshold", 50.0), event));
        verify(target, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }

    @Test
    void spectatorPlayerTargetIsNoOp() {
        var mechanic = new ExecuteMechanic();
        var player = BukkitMock.mockPlayer();
        var target = mock(org.bukkit.entity.Player.class);
        when(target.getGameMode()).thenReturn(org.bukkit.GameMode.SPECTATOR);
        when(target.getHealth()).thenReturn(2.0);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(target);

        assertFalse(mechanic.execute(player, Map.of("threshold", 50.0), event));
        verify(target, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }
}
