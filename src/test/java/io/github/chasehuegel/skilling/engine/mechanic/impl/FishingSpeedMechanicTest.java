package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.FishHook;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/**
 * Verifies {@code core:fishing_speed}: shortens the single in-flight hook's wait
 * time and re-roll bounds by a percentage, decaying toward a floor so a cast
 * never yields a second catch.
 */
class FishingSpeedMechanicTest {

    private static PlayerFishEvent cast(FishHook hook) {
        var event = mock(PlayerFishEvent.class);
        when(event.getHook()).thenReturn(hook);
        return event;
    }

    @Test
    void returnsFalseForNonFishEvent() {
        assertFalse(new FishingSpeedMechanic().execute(mock(Player.class),
                Map.of("reduction", 50.0), mock(org.bukkit.event.Event.class)));
    }

    @Test
    void returnsFalseWithoutReduction() {
        var hook = mock(FishHook.class);
        when(hook.getWaitTime()).thenReturn(200);
        when(hook.getMinWaitTime()).thenReturn(20);
        when(hook.getMaxWaitTime()).thenReturn(300);
        assertFalse(new FishingSpeedMechanic().execute(mock(Player.class),
                Map.of("reduction", 0.0), cast(hook)));
    }

    @Test
    void returnsFalseWithoutHook() {
        var event = mock(PlayerFishEvent.class);
        when(event.getHook()).thenReturn(null);
        assertFalse(new FishingSpeedMechanic().execute(mock(Player.class), Map.of("reduction", 50.0), event));
    }

    @Test
    void shortensWaitTimesByPercentage() {
        var hook = mock(FishHook.class);
        when(hook.getWaitTime()).thenReturn(200);
        when(hook.getMinWaitTime()).thenReturn(20);
        when(hook.getMaxWaitTime()).thenReturn(300);

        assertTrue(new FishingSpeedMechanic().execute(mock(Player.class),
                Map.of("reduction", 50.0), cast(hook)));

        // All three values decay by 50%, clamped to a minimum of 1.
        verify(hook).setWaitTime(100);
        verify(hook).setMinWaitTime(10);
        verify(hook).setMaxWaitTime(150);
    }

    @Test
    void waitsDecayTowardALargeReductionFloor() {
        var hook = mock(FishHook.class);
        when(hook.getWaitTime()).thenReturn(2);
        when(hook.getMinWaitTime()).thenReturn(1);
        when(hook.getMaxWaitTime()).thenReturn(50);

        assertTrue(new FishingSpeedMechanic().execute(mock(Player.class),
                Map.of("reduction", 90.0), cast(hook)));

        // A near-total reduction floors at 1 tick; never 0.
        verify(hook).setWaitTime(1);
        verify(hook).setMinWaitTime(1);
        verify(hook).setMaxWaitTime(5);
    }

    @Test
    void doesNotReelOrModifyLoot() {
        var hook = mock(FishHook.class);
        when(hook.getWaitTime()).thenReturn(200);
        when(hook.getMinWaitTime()).thenReturn(20);
        when(hook.getMaxWaitTime()).thenReturn(300);

        assertTrue(new FishingSpeedMechanic().execute(mock(Player.class),
                Map.of("reduction", 50.0), cast(hook)));

        verify(hook, never()).retrieve(any(org.bukkit.inventory.EquipmentSlot.class));
        verify(hook, never()).resetFishingState();
    }
}