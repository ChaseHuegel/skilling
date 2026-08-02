package io.github.chasehuegel.skilling.engine.listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@code player_interact} feedback targeting resolves the clicked
 * block location (and nothing for left-clicks or air interactions), which is the
 * engine support behind the piety bury-bones ability.
 */
class SkillEventListenerResolveTargetTest {

    private PlayerInteractEvent interact(Action action, Block clicked) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        when(event.getClickedBlock()).thenReturn(clicked);
        return event;
    }

    @Test
    void rightClickOnBlockResolvesClickedBlockLocation() {
        var world = mock(World.class);
        var block = mock(Block.class);
        var location = new Location(world, 1, 2, 3);
        when(block.getLocation()).thenReturn(location);

        assertEquals(location, SkillEventListener.resolveEventTargetLocation(interact(Action.RIGHT_CLICK_BLOCK, block)));
    }

    @Test
    void leftClickResolvesNoTarget() {
        assertNull(SkillEventListener.resolveEventTargetLocation(interact(Action.LEFT_CLICK_BLOCK, mock(Block.class))));
    }

    @Test
    void rightClickAirResolvesNoTarget() {
        assertNull(SkillEventListener.resolveEventTargetLocation(interact(Action.RIGHT_CLICK_AIR, null)));
    }
}
