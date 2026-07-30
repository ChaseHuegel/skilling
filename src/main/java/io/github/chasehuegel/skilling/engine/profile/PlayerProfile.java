package io.github.chasehuegel.skilling.engine.profile;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.inventory.Inventory;

/**
 * In-memory representation of a player's skill state.
 *
 * <p>Holds raw XP values per skill in a {@link ConcurrentHashMap} and
 * tracks modification count for the write-behind cache. The dirty
 * state is derived by comparing the current modCount against the
 * last-saved modCount, eliminating the race between marking dirty
 * in the main thread and clearing it in the batch worker.
 *
 * <p><b>Thread safety:</b> {@link #addXp} and {@link #setXp} are safe
 * to call from any thread. The {@link #getXpMap()} raw map should only
 * be used for read-only access or bulk loading during profile hydration.
 */
public final class PlayerProfile implements PlayerProfileView {

    private static final Gson GSON = new Gson();

    private final UUID playerId;
    private volatile boolean initialized;
    private final ConcurrentHashMap<String, Long> xpMap;
    private final AtomicLong modCount;
    private volatile long savedModCount;
    private volatile PlayerPreferences preferences;
    private volatile Map<Integer, Inventory> cachedPageInventories;

    /**
     * Constructs a new player profile.
     *
     * @param playerId the player's UUID
     */
    public PlayerProfile(UUID playerId) {
        this.playerId = playerId;
        this.xpMap = new ConcurrentHashMap<>();
        this.modCount = new AtomicLong(0);
        this.savedModCount = 0;
        this.preferences = PlayerPreferences.DEFAULTS;
    }

    /**
     * Returns the player's logging preferences.
     *
     * @return the player's preferences
     */
    public PlayerPreferences getPreferences() {
        return preferences;
    }

    /**
     * Sets the player's logging preferences and marks the profile as dirty.
     *
     * @param preferences the new preferences
     */
    public void setPreferences(PlayerPreferences preferences) {
        this.preferences = preferences;
        modCount.incrementAndGet();
    }

    /**
     * Deserializes preferences from a JSON string.
     *
     * @param json the JSON string
     */
    public void setPreferencesFromJson(String json) {
        if (json == null || json.isBlank()) {
            this.preferences = PlayerPreferences.DEFAULTS;
        } else {
            try {
                this.preferences = GSON.fromJson(json, PlayerPreferences.class);
            } catch (Exception e) {
                this.preferences = PlayerPreferences.DEFAULTS;
            }
        }
    }

    /**
     * Serializes preferences to a JSON string.
     *
     * @return the JSON string
     */
    public String getPreferencesJson() {
        return GSON.toJson(preferences);
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
        modCount.incrementAndGet();
    }

    /**
     * Adds raw XP for the given skill and marks the profile as dirty.
     *
     * @param skillId the skill identifier
     * @param amount  the amount to add
     */
    public void addXp(String skillId, long amount) {
        xpMap.merge(skillId, amount, Long::sum);
        modCount.incrementAndGet();
    }

    /**
     * Returns a snapshot copy of the XP map for batch operations.
     * The caller sees a consistent view of XP values even if the
     * live map is concurrently modified by the main thread.
     *
     * @return a snapshot of the XP map
     */
    public Map<String, Long> getXpSnapshot() {
        return new HashMap<>(xpMap);
    }

    /**
     * Returns the underlying XP map for batch operations.
     *
     * <p>Mutating the returned map directly bypasses dirty tracking.
     * Only use this for read-only access or bulk loading during
     * profile hydration from the database.
     *
     * @return the XP map
     * @deprecated Use {@link #getXpSnapshot()} for thread-safe reads.
     */
    @Deprecated
    public ConcurrentHashMap<String, Long> getXpMap() {
        return xpMap;
    }

    /**
     * Whether the profile has unsaved changes since the last database flush.
     *
     * @return true if dirty
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Marks this profile as fully initialized after loading from the database.
     */
    public void markInitialized() {
        this.initialized = true;
    }

    public boolean isDirty() {
        return modCount.get() != savedModCount;
    }

    /**
     * Records the current modCount as saved, making the profile appear
     * clean if no further modifications have occurred.
     *
     * <p>This should only be called by the {@code AsyncBatchWorker}
     * after a successful database flush. If modifications occurred
     * between the snapshot and this call, the profile will correctly
     * remain dirty.
     */
    public void markSaved() {
        this.savedModCount = modCount.get();
    }

    /**
     * Returns the cached paginated page inventories, or null if not built.
     *
     * @return map of page index to inventory, or null
     */
    public Map<Integer, Inventory> getCachedPageInventories() {
        return cachedPageInventories;
    }

    /**
     * Stores the paginated page inventories in the profile cache.
     *
     * @param inventories map of page index to fully built inventory
     */
    public void setCachedPageInventories(Map<Integer, Inventory> inventories) {
        this.cachedPageInventories = inventories;
    }

    /**
     * Invalidates the page inventory cache, forcing a rebuild on the
     * next menu open. Called on level change or plugin reload.
     */
    public void invalidatePageCache() {
        this.cachedPageInventories = null;
    }
}