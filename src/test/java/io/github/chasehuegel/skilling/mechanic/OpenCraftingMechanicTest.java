package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.OpenCraftingMechanic;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OpenCraftingMechanicTest {

    @Test
    void returnsFalseForNonInteractEvent() {
        var mechanic = new OpenCraftingMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of(), BukkitMock.mockBlockBreakEvent()));
        verify(player, never()).openWorkbench(isNull(), anyBoolean());
    }

    @Test
    void leftClickIsANoOpThatDoesNotOpenWorkbench() {
        var mechanic = new OpenCraftingMechanic();
        var player = BukkitMock.mockPlayer();
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);

        assertFalse(mechanic.execute(player, Map.of(), event));
        verify(player, never()).openWorkbench(isNull(), anyBoolean());
    }

    @Test
    void rightClickOpensFullScreenWorkbench() {
        var mechanic = new OpenCraftingMechanic();
        var player = BukkitMock.mockPlayer();
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertTrue(mechanic.execute(player, Map.of(), event));
        verify(player).openWorkbench(isNull(), eq(true));
    }
}
