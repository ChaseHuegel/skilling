package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.KnockbackMechanic;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnockbackMechanicTest {

    private io.github.chasehuegel.skilling.engine.mechanic.impl.KnockbackMechanic mechanic = new KnockbackMechanic();

    @Test
    void returnsFalseWithForceZero() {
        var player = BukkitMock.mockPlayer();
        var event = mock(EntityDamageByEntityEvent.class);
        assertFalse(mechanic.execute(player, Map.of("force", 0.0), event));
    }

    @Test
    void setsVelocityOnDamagedEntity() {
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var loc = spy(new Location(world, 0, 0, 0));
        when(player.getLocation()).thenReturn(loc);

        var target = mock(LivingEntity.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(target);

        assertTrue(mechanic.execute(player, Map.of("force", 2.0), event));
        verify(target).setVelocity(any(Vector.class));
    }

    @Test
    void shovesNearbyEntitiesWhenRadiusPositive() {
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var loc = spy(new Location(world, 0, 0, 0));
        when(player.getLocation()).thenReturn(loc);

        var target = mock(LivingEntity.class);
        when(loc.getNearbyLivingEntities(3.0)).thenReturn(List.of(target, player));

        assertTrue(mechanic.execute(player, Map.of("force", 2.0, "radius", 3.0),
                BukkitMock.mockInteractEvent(player)));
        verify(target).setVelocity(any(Vector.class));
    }

    @Test
    void returnsFalseWithNonDamageEventAndNoRadius() {
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var loc = spy(new Location(world, 0, 0, 0));
        when(player.getLocation()).thenReturn(loc);

        assertFalse(mechanic.execute(player, Map.of("force", 2.0), BukkitMock.mockInteractEvent(player)));
    }
}
