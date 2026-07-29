package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.TeleportMechanic;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeleportMechanicTest {

    @Test
    void returnsFalseForNonInteractEvent() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("range", 10.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithRangeZero() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("range", 0.0), BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void returnsTrueWithValidRange() {
        var mechanic = new TeleportMechanic();
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var block = mock(Block.class);
        when(block.isEmpty()).thenReturn(true);
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        var loc = spy(new Location(world, 0, 0, 0));
        when(player.getLocation()).thenReturn(loc);
        when(player.getTargetBlockExact(10)).thenReturn(null);
        assertTrue(mechanic.execute(player, Map.of("range", 10.0), BukkitMock.mockInteractEvent(player)));
    }
}
