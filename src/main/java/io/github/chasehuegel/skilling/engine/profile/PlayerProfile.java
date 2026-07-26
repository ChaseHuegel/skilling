package io.github.chasehuegel.skilling.engine.profile;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory representation of a player's skill state.
 *
 * <p>Holds raw XP values per skill in a {@link ConcurrentHashMap} and
 * an {@code isDirty} flag for the write-behind cache. Instances are
 * <b>not</b> thread-safe for the dirty flag itself — the
 * {@code AsyncBatchWorker} owns the drain lifecycle.
 */
public final class PlayerProfile {

    private final UUID playerId;
    private final ConcurrentHashMap<String, Long> xpMap;
    private volatile boolean dirty;

    /**
     * Constructs a new player profile.
     *
     * @param playerId the player's UUID
     */
    public PlayerProfile(UUID playerId) {
        this.playerId = playerId;
        this.xpMap = new ConcurrentHashMap<>();
        this.dirty = false;
    }

    /**
     * Returns the player's UUID.
     *
     * @return the player's UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the raw XP for the given skill.
     *
     * @param skillId the skill identifier
     * @return the raw XP, or 0 if not tracked
     */
    public long getXp(String skillId) {
        return xpMap.getOrDefault(skillId, 0L);
    }

    /**
     * Sets the raw XP for the given skill and marks the profile as dirty.
     *
     * @param skillId the skill identifier
     * @param xp      the new raw XP value
     */
    public void setXp(String skillId, long xp) {
        xpMap.put(skillId, xp);
        this.dirty = true;
    }

    /**
     * Adds raw XP for the given skill and marks the profile as dirty.
     *
     * @param skillId the skill identifier
     * @param amount  the amount to add
     */
    public void addXp(String skillId, long amount) {
        xpMap.merge(skillId, amount, Long::sum);
        this.dirty = true;
    }

    /**
     * Returns the underlying XP map for batch operations.
     *
     * @return the XP map
     */
    public ConcurrentHashMap<String, Long> getXpMap() {
        return xpMap;
    }

    /**
     * Whether the profile has unsaved changes.
     *
     * @return true if dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Marks the profile as clean after a successful database flush.
     */
    public void markClean() {
        this.dirty = false;
    }
}