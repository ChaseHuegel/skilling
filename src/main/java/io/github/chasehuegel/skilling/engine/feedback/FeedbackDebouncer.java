package io.github.chasehuegel.skilling.engine.feedback;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles failure feedback messages to prevent client-side spam.
 *
 * <p>Maintains a timestamp cache per player-ability key. If the same
 * ability fires a failure within the configured interval (default 500ms),
 * the feedback is silently dropped.
 *
 * <p>YAML configuration key: {@code debouncer.interval_ms}
 */
public final class FeedbackDebouncer {

    private volatile long intervalMs;
    private final Map<UUID, Map<String, Long>> lastFeedback;

    /**
     * Constructs a new debouncer with the given interval.
     *
     * @param intervalMs minimum interval in milliseconds between identical feedback events
     */
    public FeedbackDebouncer(long intervalMs) {
        this.intervalMs = intervalMs;
        this.lastFeedback = new ConcurrentHashMap<>();
    }

    /**
     * Updates the debounce interval at runtime (from config).
     *
     * @param intervalMs minimum interval in milliseconds
     */
    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    /**
     * Attempts to mark feedback for the given player and ability key.
     *
     * @param player    the player receiving feedback
     * @param abilityId the ability identifier
     * @return true if this feedback should proceed, false if it should be suppressed
     */
    public boolean tryDebounce(Player player, String abilityId) {
        return tryDebounce(player.getUniqueId(), abilityId);
    }

    /**
     * Attempts to mark feedback for the given player UUID and ability key.
     *
     * @param playerUuid the player's UUID
     * @param abilityId  the ability identifier
     * @return true if this feedback should proceed, false if it should be suppressed
     */
    public boolean tryDebounce(UUID playerUuid, String abilityId) {
        long now = System.currentTimeMillis();

        Map<String, Long> abilities = lastFeedback.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        Long last = abilities.get(abilityId);

        if (last != null && (now - last) < intervalMs) {
            return false;
        }

        abilities.put(abilityId, now);
        return true;
    }

    /**
     * Clears all cached timestamps for a player.
     *
     * @param player the player to clear
     */
    public void clear(Player player) {
        lastFeedback.remove(player.getUniqueId());
    }

    /**
     * Clears all cached timestamps for a player UUID.
     *
     * <p>Call this from a {@code PlayerQuitEvent} handler to prevent
     * the map from growing unboundedly with disconnected players.
     *
     * @param playerUuid the player's UUID
     */
    public void clear(UUID playerUuid) {
        lastFeedback.remove(playerUuid);
    }
}