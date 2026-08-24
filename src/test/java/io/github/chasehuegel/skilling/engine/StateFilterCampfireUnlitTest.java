package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code campfire_unlit} state filter: a click counts as an
 * unlit-campfire ignition only when the clicked block is a campfire that is not
 * lit. A lit campfire, a non-campfire block, a missing block, or any
 * non-interaction event never matches, so the lighting-a-campfire XP source
 * cannot be farmed off an already-burning fire.
 */
class StateFilterCampfireUnlitTest {

    private StateFilterRegistry registry;
    private Player player;

    @BeforeEach
    void setUp() {
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry,
                new TagResolver(new CustomTagLoader()), new EntityTagResolver(new CustomTagLoader()));
        player = mock(Player.class);
    }

    private PlayerInteractEvent click(Block block) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getClickedBlock()).thenReturn(block);
        return event;
    }

    private Block campfire(boolean lit) {
        Campfire data = mock(Campfire.class);
        when(data.isLit()).thenReturn(lit);
        Block block = mock(Block.class);
        when(block.getBlockData()).thenReturn(data);
        return block;
    }

    @Test
    void unlitCampfireMatches() {
        assertTrue(registry.evaluate("campfire_unlit", player, click(campfire(false)), "true"));
    }

    @Test
    void litCampfireDoesNotMatch() {
        assertFalse(registry.evaluate("campfire_unlit", player, click(campfire(true)), ""));
    }

    @Test
    void nonCampfireBlockDoesNotMatch() {
        Block block = mock(Block.class);
        when(block.getBlockData()).thenReturn(mock(org.bukkit.block.data.BlockData.class));
        assertFalse(registry.evaluate("campfire_unlit", player, click(block), ""));
    }

    @Test
    void nullBlockDoesNotMatch() {
        assertFalse(registry.evaluate("campfire_unlit", player, click(null), ""));
    }

    @Test
    void nonInteractionEventDoesNotMatch() {
        var event = mock(org.bukkit.event.block.BlockBreakEvent.class);
        assertFalse(registry.evaluate("campfire_unlit", player, event, ""));
    }
}