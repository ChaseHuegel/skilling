package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpBonusMechanicTest {

    private static final long T0 = 1_000_000_000L; // 1s in nanos

    @AfterEach
    void tearDown() {
        XpBonusMechanic.setClockOverrideNanos(0);
        XpBonusMechanic.clearAll();
    }

    @Test
    void multiplierAppliesWithinDuration() {
        XpBonusMechanic.setClockOverrideNanos(T0);
        var player = BukkitMock.mockPlayer();
        assertTrue(new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", 5.0),
                BukkitMock.mockInteractEvent(player)));
        assertEquals(1.5, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);
    }

    @Test
    void multiplierExpiresAfterDuration() {
        XpBonusMechanic.setClockOverrideNanos(T0);
        var player = BukkitMock.mockPlayer();
        new XpBonusMechanic().execute(player, Map.of("multiplier", 2.0, "duration", 5.0),
                BukkitMock.mockInteractEvent(player));
        assertEquals(2.0, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);

        // Past the expiry (T0 + 5s).
        XpBonusMechanic.setClockOverrideNanos(7_000_000_000L);
        assertEquals(1.0, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);
    }

    @Test
    void quitClearsEntry() {
        XpBonusMechanic.setClockOverrideNanos(T0);
        var player = BukkitMock.mockPlayer();
        new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", 5.0),
                BukkitMock.mockInteractEvent(player));
        XpBonusMechanic.clear(player.getUniqueId());
        assertEquals(1.0, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);
    }

    @Test
    void reactivationRefreshesTtlWithoutStacking() {
        XpBonusMechanic.setClockOverrideNanos(T0);
        var player = BukkitMock.mockPlayer();
        new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", 5.0),
                BukkitMock.mockInteractEvent(player));

        // Re-activate at t=4s, refreshing the window to t=9s (no stacking).
        XpBonusMechanic.setClockOverrideNanos(4_000_000_000L);
        new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", 5.0),
                BukkitMock.mockInteractEvent(player));

        // t=6s: the original window (1-6s) elapsed but the refreshed one is active.
        XpBonusMechanic.setClockOverrideNanos(6_000_000_000L);
        assertEquals(1.5, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);

        // t=10s: refreshed window elapsed too.
        XpBonusMechanic.setClockOverrideNanos(10_000_000_000L);
        assertEquals(1.0, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);
    }

    @Test
    void clearAllRemovesEveryEntry() {
        XpBonusMechanic.setClockOverrideNanos(T0);
        var player = BukkitMock.mockPlayer();
        new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", 5.0),
                BukkitMock.mockInteractEvent(player));
        XpBonusMechanic.clearAll();
        assertEquals(1.0, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);
    }

    @Test
    void hugeDurationDoesNotOverflowToInstantExpiry() {
        XpBonusMechanic.setClockOverrideNanos(T0);
        var player = BukkitMock.mockPlayer();
        // 1e12 seconds is far past the long nano ceiling; the old arithmetic
        // wrapped negative so the buff expired immediately.
        assertTrue(new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", 1e12),
                BukkitMock.mockInteractEvent(player)));
        // Bonus is still active well after any realistic expiry window.
        XpBonusMechanic.setClockOverrideNanos(Long.MAX_VALUE / 2);
        assertEquals(1.5, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);
    }

    @Test
    void zeroDurationIsANoOpWithoutConsumingActivation() {
        XpBonusMechanic.setClockOverrideNanos(T0);
        var player = BukkitMock.mockPlayer();
        // Returns false so the dispatch does not spend the ability's cost/cooldown.
        assertFalse(new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", 0.0),
                BukkitMock.mockInteractEvent(player)));
        assertEquals(1.0, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);
    }

    @Test
    void negativeAndNaNdurationAreAlsoNoOps() {
        XpBonusMechanic.setClockOverrideNanos(T0);
        var player = BukkitMock.mockPlayer();
        assertFalse(new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", -5.0),
                BukkitMock.mockInteractEvent(player)));
        assertFalse(new XpBonusMechanic().execute(player, Map.of("multiplier", 1.5, "duration", Double.NaN),
                BukkitMock.mockInteractEvent(player)));
        assertEquals(1.0, XpBonusMechanic.getMultiplier(player.getUniqueId()), 1e-9);
    }
}
