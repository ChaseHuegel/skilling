package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the probabilistic fractional rounding of {@link FishingLootMechanic}:
 * a fractional multiplier on a small stack rounds up by its fractional
 * probability instead of flooring to nothing.
 */
class FishingLootMechanicTest {

    @AfterEach
    void tearDown() {
        FishingLootMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble());
    }

    private static PlayerFishEvent eventWithCatch(int amount) {
        var caught = mock(Item.class);
        var stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(amount);
        when(caught.getItemStack()).thenReturn(stack);
        when(caught.getLocation()).thenReturn(mock(Location.class));

        var event = mock(PlayerFishEvent.class);
        when(event.getState()).thenReturn(PlayerFishEvent.State.CAUGHT_FISH);
        when(event.getCaught()).thenReturn(caught);
        return event;
    }

    @Test
    void fractionalMultiplierRoundsUpProbabilistically() {
        FishingLootMechanic.setRandomSource(() -> 0.0); // always round the fraction up
        var mechanic = new FishingLootMechanic();
        var world = mock(World.class);
        var player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);

        var event = eventWithCatch(1);
        var bonus = mock(ItemStack.class);
        when(((Item) event.getCaught()).getItemStack().clone()).thenReturn(bonus);

        // 1 fish x 1.5 -> 1.5: the 0.5 fraction rounds up to a bonus of 1.
        assertTrue(mechanic.execute(player, Map.of("multiplier", 1.5), event));
        verify(bonus).setAmount(1);
        verify(world).dropItemNaturally(any(Location.class), eq(bonus));
    }

    @Test
    void integerMultiplierDropsExactBonus() {
        var mechanic = new FishingLootMechanic();
        var world = mock(World.class);
        var player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);

        var event = eventWithCatch(2);
        var bonus = mock(ItemStack.class);
        when(((Item) event.getCaught()).getItemStack().clone()).thenReturn(bonus);

        // 2 fish x 2.0 -> 4: bonus of 2.
        assertTrue(mechanic.execute(player, Map.of("multiplier", 2.0), event));
        verify(bonus).setAmount(2);
        verify(world).dropItemNaturally(any(Location.class), eq(bonus));
    }

    @Test
    void missedFractionDropsNothing() {
        FishingLootMechanic.setRandomSource(() -> 0.9); // miss the 0.5 fraction
        var mechanic = new FishingLootMechanic();
        var world = mock(World.class);
        var player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);

        assertTrue(mechanic.execute(player, Map.of("multiplier", 1.5), eventWithCatch(1)));
        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }
}
