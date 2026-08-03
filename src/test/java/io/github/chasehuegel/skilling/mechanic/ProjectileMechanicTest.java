package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ProjectileMechanic;
import org.bukkit.Location;
import org.bukkit.entity.Snowball;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectileMechanicTest {

    @Test
    void returnsFalseForNonInteractEvent() {
        var mechanic = new ProjectileMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsTrueWithDefaultParams() {
        var mechanic = new ProjectileMechanic();
        var player = BukkitMock.mockPlayer();
        var snowball = mock(Snowball.class);
        when(player.launchProjectile(any())).thenReturn(snowball);
        when(snowball.getPersistentDataContainer()).thenReturn(mock(org.bukkit.persistence.PersistentDataContainer.class));
        var loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);
        when(loc.getDirection()).thenReturn(mock(org.bukkit.util.Vector.class));
        assertTrue(mechanic.execute(player, Map.of(), BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void leftClickIsANoOpThatDoesNotLaunch() {
        var mechanic = new ProjectileMechanic();
        var player = BukkitMock.mockPlayer();
        var event = mock(org.bukkit.event.player.PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(org.bukkit.event.block.Action.LEFT_CLICK_AIR);

        assertFalse(mechanic.execute(player, Map.of("speed", 1.5, "damage", 4.0), event));
        verify(player, never()).launchProjectile(any());
    }
}
