package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.SetCooldownMechanic;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SetCooldownMechanicTest {

    private final SetCooldownMechanic mechanic = new SetCooldownMechanic();

    @Test
    void resolvesNamespacedShieldMaterial() {
        var player = BukkitMock.mockPlayer();
        assertTrue(mechanic.execute(player, Map.of("material", "minecraft:shield", "ticks", 40.0),
                BukkitMock.mockBlockBreakEvent(player)));
        verify(player).setCooldown(Material.SHIELD, 40);
    }

    @Test
    void throwsOnInvalidMaterial() {
        var player = BukkitMock.mockPlayer();
        assertThrows(IllegalArgumentException.class,
                () -> mechanic.execute(player, Map.of("material", "minecraft:not_a_real_material", "ticks", 40.0),
                        BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithoutMaterialParam() {
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("ticks", 40.0), BukkitMock.mockBlockBreakEvent(player)));
    }

    @Test
    void returnsFalseWithNonPositiveTicks() {
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("material", "minecraft:shield", "ticks", 0.0),
                BukkitMock.mockBlockBreakEvent(player)));
    }
}
