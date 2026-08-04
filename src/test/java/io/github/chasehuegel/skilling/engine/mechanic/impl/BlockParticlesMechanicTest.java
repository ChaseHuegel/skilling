package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.BukkitMock;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockParticlesMechanicTest {

    @Test
    void spawnsParticleAtClickedBlockLocation() {
        var mechanic = new BlockParticlesMechanic();
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var block = mock(Block.class);
        var location = new Location(world, 1, 2, 3);
        when(block.getLocation()).thenReturn(location);

        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);

        assertTrue(mechanic.execute(player, Map.of("particle", "minecraft:happy_villager", "count", 8, "speed", 0.1), event));
        verify(world).spawnParticle(any(Particle.class), eq(location), eq(8), eq(0.0), eq(0.0), eq(0.0), eq(0.1));
    }

    @Test
    void returnsFalseWithoutABlockTarget() {
        var mechanic = new BlockParticlesMechanic();
        var player = BukkitMock.mockPlayer();

        var leftClick = mock(PlayerInteractEvent.class);
        when(leftClick.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        assertFalse(mechanic.execute(player, Map.of("particle", "minecraft:happy_villager"), leftClick));

        var air = mock(PlayerInteractEvent.class);
        when(air.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        assertFalse(mechanic.execute(player, Map.of("particle", "minecraft:happy_villager"), air));
    }

    @Test
    void unknownParticleNeverThrowsAtExecute() {
        var mechanic = new BlockParticlesMechanic();
        var player = BukkitMock.mockPlayer();
        var world = mock(World.class);
        var block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(world, 1, 2, 3));

        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);

        // Unknown particles are rejected at skill load by the registry validator;
        // if one still reaches execute it must be a defensive no-op, never a throw.
        assertDoesNotThrow(() ->
                mechanic.execute(player, Map.of("particle", "minecraft:not_a_particle"), event));
    }
}
