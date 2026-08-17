package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code honey_level} state filter: it reads the honey level of a
 * clicked beehive on {@code player_interact} for {@code below}/{@code above}/
 * {@code exactly} comparisons, and fails closed for non-interaction events,
 * non-beehive clicks, and malformed or unsupported values.
 */
class StateFilterHoneyLevelTest {

    private StateFilterRegistry registry;
    private Player player;

    @BeforeEach
    void setUp() {
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry,
                new TagResolver(new CustomTagLoader()), new EntityTagResolver(new CustomTagLoader()));
        player = mock(Player.class);
    }

    private PlayerInteractEvent interactWithBeehive(int honeyLevel) {
        var event = mock(PlayerInteractEvent.class);
        var block = mock(Block.class);
        var data = mock(org.bukkit.block.data.type.Beehive.class);
        when(data.getHoneyLevel()).thenReturn(honeyLevel);
        when(block.getBlockData()).thenReturn(data);
        when(event.getClickedBlock()).thenReturn(block);
        return event;
    }

    private PlayerInteractEvent interactWithNonBeehive() {
        var event = mock(PlayerInteractEvent.class);
        var block = mock(Block.class);
        when(block.getBlockData()).thenReturn(mock(org.bukkit.block.data.BlockData.class));
        when(event.getClickedBlock()).thenReturn(block);
        return event;
    }

    @Test
    void belowMatchesWhenLevelIsUnderThreshold() {
        var event = interactWithBeehive(2);
        assertTrue(registry.evaluate("honey_level", player, event, "below:5"));
        assertFalse(registry.evaluate("honey_level", player, event, "below:2"));
    }

    @Test
    void aboveMatchesWhenLevelIsOverThreshold() {
        var event = interactWithBeehive(4);
        assertTrue(registry.evaluate("honey_level", player, event, "above:3"));
        assertFalse(registry.evaluate("honey_level", player, event, "above:4"));
    }

    @Test
    void exactlyMatchesOnlyEqualLevel() {
        var event = interactWithBeehive(3);
        assertTrue(registry.evaluate("honey_level", player, event, "exactly:3"));
        assertFalse(registry.evaluate("honey_level", player, event, "exactly:2"));
    }

    @Test
    void emptyHiveFailsAboveZero() {
        var event = interactWithBeehive(0);
        assertFalse(registry.evaluate("honey_level", player, event, "above:0"));
        assertTrue(registry.evaluate("honey_level", player, event, "exactly:0"));
    }

    @Test
    void nonBeehiveClickFailsClosed() {
        var event = interactWithNonBeehive();
        assertFalse(registry.evaluate("honey_level", player, event, "below:5"));
        assertFalse(registry.evaluate("honey_level", player, event, "above:0"));
    }

    @Test
    void nonInteractEventFailsClosed() {
        assertFalse(registry.evaluate("honey_level", player, null, "below:5"));
    }

    @Test
    void malformedValueFailsClosed() {
        var event = interactWithBeehive(2);
        assertFalse(registry.evaluate("honey_level", player, event, "below"));
        assertFalse(registry.evaluate("honey_level", player, event, "below:lots"));
        assertFalse(registry.evaluate("honey_level", player, event, "around:2"));
    }
}
