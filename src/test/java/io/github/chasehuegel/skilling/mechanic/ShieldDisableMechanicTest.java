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
    void returnsFalseWithNonPlayerTarget() {
        var player = BukkitMock.mockPlayer();
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(mock(Entity.class));
        assertFalse(mechanic.execute(player, Map.of("ticks", 20.0), event));
    }

    @Test
    void setsCooldownOnPlayerVictim() {
        var player = BukkitMock.mockPlayer();
        var victim = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        assertTrue(mechanic.execute(player, Map.of("ticks", 30.0), event));
        verify(victim).setCooldown(Material.SHIELD, 30);
    }

    @Test
    void setsCooldownOnSelfForInteractEvent() {
        var player = BukkitMock.mockPlayer();
        assertTrue(mechanic.execute(player, Map.of("ticks", 20.0), BukkitMock.mockInteractEvent(player)));
        verify(player).setCooldown(Material.SHIELD, 20);
    }
}
