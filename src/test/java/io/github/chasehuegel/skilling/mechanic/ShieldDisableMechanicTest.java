package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ShieldDisableMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShieldDisableMechanicTest {

    private final ShieldDisableMechanic mechanic = new ShieldDisableMechanic();

    @Test
    void returnsFalseWithNonPositiveTicks() {
        var player = BukkitMock.mockPlayer();
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(player);
        assertFalse(mechanic.execute(player, Map.of("ticks", 0.0), event));
    }

    @Test
    void defaultTargetsVictimOnDamageEvent() {
        var player = BukkitMock.mockPlayer();
        var victim = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        assertTrue(mechanic.execute(player, Map.of("ticks", 30.0), event));
        verify(victim).setCooldown(Material.SHIELD, 30);
    }

    @Test
    void nonPlayerDamagedEntityIsASafeNoOp() {
        var player = BukkitMock.mockPlayer();
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(mock(Entity.class));
        assertFalse(mechanic.execute(player, Map.of("ticks", 20.0), event));
        verify(player, never()).setCooldown(any(Material.class), anyInt());
    }

    @Test
    void attackerTargetDisablesTheDamagingPlayer() {
        // entity_damage_taken binding: the activating player is the victim and a
        // player attacker's shield is disabled.
        var player = BukkitMock.mockPlayer();
        var attacker = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getEntity()).thenReturn(player);
        assertTrue(mechanic.execute(player, Map.of("ticks", 30.0, "target", "attacker"), event));
        verify(attacker).setCooldown(Material.SHIELD, 30);
        verify(player, never()).setCooldown(any(Material.class), anyInt());
    }

    @Test
    void attackerTargetNoOpsWhenAttackerIsNotAPlayer() {
        var player = BukkitMock.mockPlayer();
        var zombie = mock(org.bukkit.entity.Zombie.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(zombie);
        when(event.getEntity()).thenReturn(player);
        assertFalse(mechanic.execute(player, Map.of("ticks", 30.0, "target", "attacker"), event));
        verify(player, never()).setCooldown(any(Material.class), anyInt());
    }

    @Test
    void selfTargetAlwaysDisablesTheActivatingPlayer() {
        var player = BukkitMock.mockPlayer();
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(mock(Player.class));
        assertTrue(mechanic.execute(player, Map.of("ticks", 20.0, "target", "self"), event));
        verify(player).setCooldown(Material.SHIELD, 20);
    }

    @Test
    void interactEventFallsBackToSelf() {
        var player = BukkitMock.mockPlayer();
        assertTrue(mechanic.execute(player, Map.of("ticks", 20.0), BukkitMock.mockInteractEvent(player)));
        verify(player).setCooldown(Material.SHIELD, 20);
    }
}
