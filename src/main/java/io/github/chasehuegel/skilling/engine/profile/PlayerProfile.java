package io.github.chasehuegel.skilling.engine.profile;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private volatile boolean preferencesLoaded;
    private volatile boolean progressLoaded;
    private final ConcurrentHashMap<String, Long> xpMap;
    private final Set<String> pendingFanfareSkills = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, String> progress;
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
        this.progress = new ConcurrentHashMap<>();
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
     * <p>An explicit set is the player's real intent, so it also marks the
     * preferences as loaded: a failed hydration load must not let the write-behind
     * flush overwrite the persisted row with defaults, but a player-set value is
     * always safe to persist.
     *
     * @param preferences the new preferences
     */
    public void setPreferences(PlayerPreferences preferences) {
        this.preferences = preferences;
        this.preferencesLoaded = true;
        modCount.incrementAndGet();
    }

    /**
     * Returns whether the preferences were loaded successfully (or explicitly set
     * by the player). A profile whose hydration prefs-read failed keeps this
     * false so the flush never overwrites the player's real persisted row with
     * defaults.
     *
     * @return true when the in-memory preferences are safe to persist
     */
    public boolean preferencesLoaded() {
        return preferencesLoaded;
    }

    /**
     * Marks the preferences as loaded after a successful database read.
     */
    public void markPreferencesLoaded() {
        this.preferencesLoaded = true;
    }

    /**
     * Returns the generic per-player progress store. Mechanics persist arbitrary
     * string-keyed data here (for example a discovery set serialized to JSON),
     * riding the same write-behind flush as the XP map and preferences.
     *
     * <p><b>Thread safety:</b> callers mutate the returned map on the main
     * thread and then call {@link #markProgressDirty}. Bulk-loading during
     * profile hydration must use {@link #putAllProgress}.
     *
     * @return the modifiable progress map
     */
    public Map<String, String> getProgress() {
        return progress;
    }

    /**
     * Loads progress rows into the map during hydration and marks it as loaded,
     * so the write-behind flush never overwrites the persisted rows with an
     * unhydrated empty map.
     *
     * @param rows the persisted key/value rows
     */
    public void putAllProgress(Map<String, String> rows) {
        progress.putAll(rows);
        this.progressLoaded = true;
    }

    /**
     * Returns whether progress was loaded successfully. A profile whose hydration
     * progress-read failed keeps this false so the flush never overwrites the
     * player's real persisted rows.
     *
     * @return true when the in-memory progress is safe to persist
     */
    public boolean progressLoaded() {
        return progressLoaded;
    }

    /**
     * Marks the progress store dirty so the write-behind worker persists it.
     * Call after mutating {@link #getProgress()}.
     */
    public void markProgressDirty() {
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
        // compute() (not merge) so the clamp runs even for a brand-new key; merge
        // would insert a negative amount verbatim on first use.
        xpMap.compute(skillId, (k, old) -> {
            long sum = (old == null ? 0L : old) + amount;
            if (sum < 0) {
                // A positive overflow must saturate to MAX_VALUE instead of
                // wrapping negative; a genuinely negative result clamps to zero
                // rather than flipping to an absurd MAX_VALUE.
                return old != null && old > 0 && amount > 0 ? Long.MAX_VALUE : 0L;
            }
            return sum;
        });
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
     * Returns the current modification counter.
     *
     * @return the number of mutations applied to this profile
     */
    public long getModCount() {
        return modCount.get();
    }

    /**
     * Returns the modification counter captured at database snapshot time as
     * saved, making the profile appear clean only if no further modifications
     * occurred after that snapshot.
     *
     * <p>This should only be called by the {@code AsyncBatchWorker} after a
     * successful database flush of the corresponding snapshot. If XP was added
     * between taking the snapshot and this call, the live modCount exceeds the
     * passed marker and the profile correctly remains dirty so the next flush
     * persists the newer data.
     *
     * @param snapshotModCount the modCount captured when the flushed snapshot was taken
     */
    public void markSaved(long snapshotModCount) {
        this.savedModCount = snapshotModCount;
    }

    /**
     * Records that a skill has a level-up/XP fanfare pending, set when an offline
     * admin command changed the skill while the player was away.
     *
     * @param skillId the skill identifier
     */
    public void addPendingFanfare(String skillId) {
        if (skillId != null) pendingFanfareSkills.add(skillId);
    }

    /**
     * Whether a fanfare is pending for the given skill.
     *
     * @param skillId the skill identifier
     * @return true if a fanfare is pending
     */
    public boolean hasPendingFanfare(String skillId) {
        return pendingFanfareSkills.contains(skillId);
    }

    /**
     * Returns a copy of all skill IDs with pending fanfare.
     *
     * @return the pending fanfare skill IDs
     */
    public Set<String> pendingFanfareSkills() {
        return Set.copyOf(pendingFanfareSkills);
    }

    /**
     * Consumes the pending fanfare for a skill, marking the profile dirty so the
     * next flush clears the {@code fanfare_pending} flag in the database. The
     * flag is consumed exactly once per pending entry.
     *
     * @param skillId the skill identifier
     * @return true if a fanfare was pending and was consumed
     */
    public boolean consumePendingFanfare(String skillId) {
        boolean consumed = pendingFanfareSkills.remove(skillId);
        if (consumed) {
            modCount.incrementAndGet();
        }
        return consumed;
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