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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the chance-roll semantics of {@link FishingYieldMechanic}: reaching
 * the roll counts as an activation attempt, so a failed roll cannot be retried
 * for free.
 */
class FishingYieldMechanicTest {

    @AfterEach
    void tearDown() {
        FishingYieldMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        FishingYieldMechanic.setRandomSource(() -> 99.0);
        var mechanic = new FishingYieldMechanic();
        var player = mock(Player.class);
        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        var caught = mock(Item.class);
        when(caught.getItemStack()).thenReturn(mock(ItemStack.class));
        when(caught.getLocation()).thenReturn(mock(Location.class));
        var event = mock(PlayerFishEvent.class);
        when(event.getState()).thenReturn(PlayerFishEvent.State.CAUGHT_FISH);
        when(event.getCaught()).thenReturn(caught);

        assertTrue(mechanic.execute(player, Map.of("yield_chance", 50.0), event));
        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }
}
