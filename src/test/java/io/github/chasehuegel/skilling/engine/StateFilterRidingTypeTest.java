package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Strider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code riding_type} state filter: it classifies the vehicle the
 * player is riding (specific mounts, the {@code equine} and {@code living_mount}
 * umbrellas), and fails closed when the player rides nothing or the value is
 * unknown.
 */
class StateFilterRidingTypeTest {

    private StateFilterRegistry registry;
    private Player player;

    @BeforeEach
    void setUp() {
        var loader = new CustomTagLoader();
        var resolver = new TagResolver(loader);
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry, resolver, new EntityTagResolver(loader));
        player = mock(Player.class);
    }

    private void riding(Object vehicle) {
        when(player.getVehicle()).thenReturn((org.bukkit.entity.Entity) vehicle);
    }

    @Test
    void matchesSpecificMount() {
        riding(mock(Horse.class));
        assertTrue(registry.evaluate("riding_type", player, null, "horse"));
        assertFalse(registry.evaluate("riding_type", player, null, "pig"));

        riding(mock(Pig.class));
        assertTrue(registry.evaluate("riding_type", player, null, "pig"));

        riding(mock(Strider.class));
        assertTrue(registry.evaluate("riding_type", player, null, "strider"));
    }

    @Test
    void equineUmbrellaCoversAnyAbstractHorse() {
        riding(mock(org.bukkit.entity.Donkey.class));
        assertTrue(registry.evaluate("riding_type", player, null, "equine"));
        assertTrue(registry.evaluate("riding_type", player, null, "donkey"));
        assertFalse(registry.evaluate("riding_type", player, null, "horse"));
    }

    @Test
    void livingMountUmbrellaMatchesLivingVehicles() {
        riding(mock(Horse.class));
        assertTrue(registry.evaluate("riding_type", player, null, "living_mount"));

        riding(mock(org.bukkit.entity.Boat.class));
        assertTrue(registry.evaluate("riding_type", player, null, "boat"));
        assertFalse(registry.evaluate("riding_type", player, null, "living_mount"));
    }

    @Test
    void failsClosedWhenNotRiding() {
        when(player.getVehicle()).thenReturn(null);
        assertFalse(registry.evaluate("riding_type", player, null, "horse"));
    }

    @Test
    void failsClosedForUnknownValue() {
        riding(mock(Horse.class));
        assertFalse(registry.evaluate("riding_type", player, null, "gryphon"));
        assertFalse(registry.evaluate("riding_type", player, null, ""));
    }
}