package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.combat.BlockHistory;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code timed_block} state filter: a hit counts as timed only when
 * the player is blocking and raised the shield within the configurable window of
 * the hit. A stale raise or a non-blocking stance never matches.
 */
class StateFilterTimedBlockTest {

    private StateFilterRegistry registry;
    private Player player;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry,
                new TagResolver(new CustomTagLoader()), new EntityTagResolver(new CustomTagLoader()));
        player = mock(Player.class);
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
    }

    @AfterEach
    void tearDown() {
        BlockHistory.clear();
    }

    @Test
    void notBlockingNeverMatches() {
        when(player.isBlocking()).thenReturn(false);
        BlockHistory.record(player);
        assertFalse(registry.evaluate("timed_block", player, null, ""));
    }

    @Test
    void neverRecordedRaiseDoesNotMatch() {
        when(player.isBlocking()).thenReturn(true);
        assertFalse(registry.evaluate("timed_block", player, null, ""));
    }

    @Test
    void recentRaiseMatches() {
        when(player.isBlocking()).thenReturn(true);
        BlockHistory.record(player);
        // Right after a raise, well inside the default ~300ms window.
        assertTrue(registry.evaluate("timed_block", player, null, ""));
    }

    @Test
    void staleRaiseDoesNotMatch() {
        when(player.isBlocking()).thenReturn(true);
        BlockHistory.recordAt(playerId, System.currentTimeMillis() - 5_000);
        assertFalse(registry.evaluate("timed_block", player, null, ""));
    }
}