package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:fishing_loot} floors the multiplied amount so a
 * fractional multiplier never inflates a small caught stack.
 */
class FishingLootMechanicTest {

    private PlayerFishEvent caughtEvent(Item caught) {
        var event = mock(PlayerFishEvent.class);
        when(event.getState()).thenReturn(PlayerFishEvent.State.CAUGHT_FISH);
        when(event.getCaught()).thenReturn(caught);
        return event;
    }

    @Test
    void fractionalMultiplierNeverInflatesSmallStacks() {
        var mechanic = new FishingLootMechanic();
        var player = mock(Player.class);
        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        var stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(1);
        var caught = mock(Item.class);
        when(caught.getItemStack()).thenReturn(stack);
        when(caught.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        // 1 fish * 1.5 floors to 1 -> no bonus, so nothing is dropped.
        assertTrue(mechanic.execute(player, Map.of("multiplier", 1.5), caughtEvent(caught)));
        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void multiplierDropsFlooredBonus() {
        var mechanic = new FishingLootMechanic();
        var player = mock(Player.class);
        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        var stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(5);
        when(stack.clone()).thenReturn(mock(ItemStack.class));
        var caught = mock(Item.class);
        when(caught.getItemStack()).thenReturn(stack);
        when(caught.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        // 5 * 2.5 floors to 12 -> bonus of 7.
        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.5), caughtEvent(caught)));
        verify(world).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }
}
