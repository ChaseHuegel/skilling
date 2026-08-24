package io.github.chasehuegel.skilling.engine.combat;

import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Tracks when each player last raised a shield, to drive the {@code timed_block}
 * state filter.
 *
 * <p>Bukkit exposes blocking only as a boolean ({@link Player#isBlocking()}), not
 * as a timestamp, so the engine records the moment a player raises a shield on the
 * shield-raise {@code PlayerInteractEvent}. The {@code timed_block} filter then
 * compares the current time against this recorded raise and the configurable
 * {@code combat.timed_block_window_ticks} window to answer "blocked in time."
 *
 * <p>State lives in a static map keyed by player UUID and is event-driven (no
 * per-tick task), matching the engine's "zero constant ticking" pillar. Stale
 * entries are retained harmlessly; the window check rejects anything older than
 * the configured window, so an abandoned raise simply stops matching.
 */
public final class BlockHistory {

    private static final Map<UUID, Long> LAST_RAISE_MILLIS = new ConcurrentHashMap<>();

    private BlockHistory() {}

    /**
     * Records the current time as the player's most recent shield raise.
     *
     * @param player the player who raised a shield
     */
    public static void record(Player player) {
        LAST_RAISE_MILLIS.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Returns the wall-clock millisecond time the player last raised a shield.
     *
     * @param playerId the player's UUID
     * @return the recorded raise time in epoch millis, or {@code 0} if never recorded
     */
    public static long lastRaise(UUID playerId) {
        return LAST_RAISE_MILLIS.getOrDefault(playerId, 0L);
    }

    /**
     * Test seam to backdate a raise so a {@code timed_block} window test can assert
     * that an old raise no longer matches.
     *
     * @param playerId the player's UUID
     * @param millis   the epoch-millisecond raise time to record
     */
    public static void recordAt(UUID playerId, long millis) {
        LAST_RAISE_MILLIS.put(playerId, millis);
    }

    /**
     * Clears all recorded shield raises. Used by tests between cases.
     */
    public static void clear() {
        LAST_RAISE_MILLIS.clear();
    }
}