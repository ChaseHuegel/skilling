package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyShearOutputMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ModifyShearOutputMechanicTest {

    private Player mockPlayer() {
        return BukkitMock.mockPlayer();
    }

    /**
     * A drop mock with a pre-built {@code clone()} target, so the mechanic's
     * copy loop runs without the ITEM registry (null in a plain-JUnit JVM) and
     * without a nested {@code when} inside a thenAnswer.
     */
    private static ItemStack drop(int amount) {
        var copy = mock(ItemStack.class);
        when(copy.getAmount()).thenReturn(amount);
        when(copy.getMaxStackSize()).thenReturn(64);

        var stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.getMaxStackSize()).thenReturn(64);
        when(stack.clone()).thenReturn(copy);
        return stack;
    }

    @Test
    void returnsFalseForNonShearEvent() {
        var mechanic = new ModifyShearOutputMechanic();
        var player = mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 2.0),
                BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void returnsFalseWithMultiplierOne() {
        var mechanic = new ModifyShearOutputMechanic();
        var player = mockPlayer();
        var event = mock(PlayerShearEntityEvent.class);
        var drops = List.of(drop(2));
        when(event.getDrops()).thenReturn(drops);
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), event));
        verifyNoInteractions(event);
    }

    @Test
    void scalesEveryDropRespectingTheStackCap() {
        var mechanic = new ModifyShearOutputMechanic();
        var player = mockPlayer();
        var event = mock(PlayerShearEntityEvent.class);
        // 2 wool at 2.0x -> 2 original + 2 bonus.
        var drops = List.of(drop(2));
        when(event.getDrops()).thenReturn(drops);

        List<ItemStack> written = new ArrayList<>();
        doAnswer(invocation -> {
            written.addAll(invocation.getArgument(0));
            return null;
        }).when(event).setDrops(anyList());

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        assertEquals(2, written.size());
        assertEquals(2, written.get(0).getAmount());
        assertEquals(2, written.get(1).getAmount());
    }

    @Test
    void bonusAmplifiesAsOriginalTimesMinusOne() {
        var mechanic = new ModifyShearOutputMechanic();
        var player = mockPlayer();
        var event = mock(PlayerShearEntityEvent.class);
        // 4 wool at 2.0x yields 4 bonus -> 4 original + 4 extra = 8 total.
        var drops = List.of(drop(4));
        when(event.getDrops()).thenReturn(drops);

        List<ItemStack> written = new ArrayList<>();
        doAnswer(invocation -> {
            written.addAll(invocation.getArgument(0));
            return null;
        }).when(event).setDrops(anyList());

        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        assertEquals(8, written.stream().mapToInt(ItemStack::getAmount).sum());
    }
}