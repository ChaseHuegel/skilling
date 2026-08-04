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
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(target);

        assertTrue(mechanic.execute(player, Map.of("threshold", 50.0), event));
        // 5/20 (default max health) = 25% <= 50%, so the target is killed
        // through the damage pipeline (attributed to the player, vanilla XP).
        verify(target).damage(5.0, player);
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
}
