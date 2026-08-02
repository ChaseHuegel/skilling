package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.FishingYieldMechanic;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FishingYieldMechanicTest {

    /** CAUGHT_FISH event whose caught item holds {@code amount} items. */
    private PlayerFishEvent caughtEvent(int amount, ItemStack original) {
        var event = mock(PlayerFishEvent.class);
        when(event.getState()).thenReturn(PlayerFishEvent.State.CAUGHT_FISH);
        var caught = mock(Item.class);
        when(caught.getItemStack()).thenReturn(original);
        when(caught.getLocation()).thenReturn(mock(Location.class));
        when(event.getCaught()).thenReturn(caught);
        return event;
    }

    private ItemStack originalStack(int amount) {
        var stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(amount);
        return stack;
    }

    @Test
    void returnsFalseForNonFishEvent() {
        var mechanic = new FishingYieldMechanic();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("yield_chance", 100.0), BukkitMock.mockInteractEvent(player)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var mechanic = new FishingYieldMechanic();
        var player = BukkitMock.mockPlayer();
        var event = caughtEvent(2, originalStack(2));
        assertFalse(mechanic.execute(player, Map.of(), event));
    }

    @Test
    void doublingDropsOnlyTheExtraAmountNotAFullClone() {
        var mechanic = new FishingYieldMechanic();
        var player = BukkitMock.mockPlayer();
        var original = originalStack(2);
        var bonus = mock(ItemStack.class);
        when(original.clone()).thenReturn(bonus);
        var event = caughtEvent(2, original);

        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        assertTrue(mechanic.execute(player, Map.of("yield_chance", 100.0), event));

        // The bonus is exactly the extra (original × (multiplier − 1) with
        // multiplier 2 → original amount), so total collected = 2 + 2 = 4 = 2x.
        verify(bonus).setAmount(2);
        verify(world).dropItemNaturally(any(Location.class), eq(bonus));
        // The original caught stack is never modified.
        verify(original, never()).setAmount(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void zeroChanceLeavesCatchUnchanged() {
        var mechanic = new FishingYieldMechanic();
        var player = BukkitMock.mockPlayer();
        var event = caughtEvent(2, originalStack(2));
        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);

        assertFalse(mechanic.execute(player, Map.of("yield_chance", 0.0), event));
        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }
}
