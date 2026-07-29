package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.AutoSmeltMechanic;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutoSmeltMechanicTest {

    @Test
    void returnsFalseForNonBlockBreakEvent() {
        var mechanic = new AutoSmeltMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("chance", 100.0), BukkitMock.mockDamageEvent(player, 10.0)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var mechanic = new AutoSmeltMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithUnsmeltableMaterial() {
        var mechanic = new AutoSmeltMechanic();
        var player = BukkitMock.mockPlayer();
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);
        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);
        assertFalse(mechanic.execute(player, Map.of("chance", 100.0), event));
    }
}
