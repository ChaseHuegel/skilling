package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Applies a multiplicative XP bonus for a configured duration when activated.
 * The bonus expires after {@code duration} seconds, is refreshed (never stacked)
 * on re-activation, and is cleared on quit and plugin reload.
 *
 * <p>YAML key: {@code core:xp_bonus}
 * <br>Params:
 * <ul>
 *   <li>{@code multiplier} (multiplicative factor, not a percentage increase;
 *   1.0 = no bonus, 1.5 = +50%, 2.0 = double)</li>
 *   <li>{@code duration} (seconds, optional, default 30) — how long the bonus lasts</li>
 * </ul>
 */
public record XpBonusMechanic() implements SkillMechanic {

    private static final double DEFAULT_DURATION_SECONDS = 30.0;

    private static final Map<UUID, Bonus> multipliers = new ConcurrentHashMap<>();
    private static volatile long clockOverrideNanos = 0;

    private record Bonus(double multiplier, long expiryNanos) {}

    private static long now() {
        return clockOverrideNanos == 0 ? System.nanoTime() : clockOverrideNanos;
    }

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to force a deterministic
     * clock for bonus-expiry tests; a zero value uses the real system clock.
     *
     * @param nanos the overridden time in nanoseconds, or 0 to use the system clock
     */
    static void setClockOverrideNanos(long nanos) {
        clockOverrideNanos = nanos;
    }

    /**
     * Returns the active XP multiplier for the given player, defaulting to 1.0.
     * Expired bonuses are evicted on read so they never apply.
     */
    public static double getMultiplier(UUID playerId) {
        Bonus bonus = multipliers.get(playerId);
        if (bonus == null) return 1.0;
        if (bonus.expiryNanos() < now()) {
            multipliers.remove(playerId, bonus);
            return 1.0;
        }
        return bonus.multiplier();
    }

    /**
     * Clears the player's active bonus (called on quit).
     *
     * @param playerId the player's UUID
     */
    public static void clear(UUID playerId) {
        multipliers.remove(playerId);
    }

    /**
     * Clears all active bonuses (called on reload/disable).
     */
    public static void clearAll() {
        multipliers.clear();
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double mult = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (mult <= 0) return false;
        double durationSec = ((Number) params.getOrDefault("duration", DEFAULT_DURATION_SECONDS)).doubleValue();
        // A zero, negative, or NaN duration is a no-op: never create an
        // already-expired buff that consumes the activation's cost/cooldown.
        if (!(durationSec > 0)) return false;
        long durationNanos = (long) (durationSec * 1_000_000_000L);
        // Re-activation refreshes the TTL rather than stacking multiple bonuses.
        multipliers.put(player.getUniqueId(), new Bonus(mult, saturatingAdd(now(), durationNanos)));
        return true;
    }

    /**
     * Adds two nanosecond timestamps without wraparound: a huge duration (or a
     * clock near the long ceiling) saturates at {@link Long#MAX_VALUE} so the
     * buff becomes practically permanent instead of wrapping negative and
     * expiring instantly.
     *
     * @param now           the current time in nanoseconds
     * @param durationNanos the bonus duration in nanoseconds
     * @return the expiry time, saturated at {@link Long#MAX_VALUE}
     */
    private static long saturatingAdd(long now, long durationNanos) {
        try {
            return Math.addExact(now, durationNanos);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }
}
