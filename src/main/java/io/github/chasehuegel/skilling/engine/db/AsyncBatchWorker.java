package io.github.chasehuegel.skilling.engine.db;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import org.bukkit.Bukkit;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public final class AsyncBatchWorker implements Runnable {

    // Periodic flush interval: 30 seconds (30s * 20 ticks/s). This bounds the
    // crash-loss window for XP written only to the in-memory write-behind cache.
    private static final long INTERVAL_TICKS = 20 * 30;

    /**
     * Player-skill UPSERT. Writing a profile row clears {@code fanfare_pending}
     * so a consumed offline fanfare is never re-fired on a later login.
     */
    public static final String PLAYER_SKILLS_UPSERT = """
            INSERT INTO player_skills (player_uuid, skill_id, xp)
            VALUES (?, ?, ?)
            ON CONFLICT(player_uuid, skill_id) DO UPDATE SET xp = excluded.xp, fanfare_pending = 0
            """;

    private static final String PREFERENCES_UPSERT = """
            INSERT INTO player_preferences (player_uuid, preferences) VALUES (?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET preferences = excluded.preferences
            """;

    private final Skilling plugin;
    private final DatabaseManager databaseManager;
    private final ProfileManager profileManager;
    private final RequirementEngine requirementEngine;
    private final ReentrantLock lock = new ReentrantLock();
    /** Set when a periodic run finds the lock held; the holder flushes again. */
    private final java.util.concurrent.atomic.AtomicBoolean deferredFlush = new java.util.concurrent.atomic.AtomicBoolean(false);
    private int taskId = -1;

    public AsyncBatchWorker(Skilling plugin, DatabaseManager databaseManager, ProfileManager profileManager) {
        this(plugin, databaseManager, profileManager, null);
    }

    public AsyncBatchWorker(Skilling plugin, DatabaseManager databaseManager, ProfileManager profileManager,
                            RequirementEngine requirementEngine) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.profileManager = profileManager;
        this.requirementEngine = requirementEngine;
    }

    public void start() {
        // Run the first flush promptly (1s after enable) rather than waiting a
        // full interval, so freshly-earned XP is persisted quickly after startup.
        this.taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, 20L, INTERVAL_TICKS).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public void run() {
        if (lock.tryLock()) {
            try {
                flushLoop();
            } finally {
                lock.unlock();
            }
        } else {
            // A quit-flush or manual flush holds the lock; queue a deferred flush
            // so this skipped periodic run is retried promptly instead of waiting
            // a full interval.
            deferredFlush.set(true);
        }
    }

    public void flushDirtyProfiles() {
        lock.lock();
        try {
            flushLoop();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs a flush, re-running while a deferred request arrived (a periodic run
     * that found the lock held), so a contended flush is retried right after the
     * holder finishes instead of silently skipping a full interval.
     */
    private void flushLoop() {
        do {
            deferredFlush.set(false);
            doFlush();
        } while (deferredFlush.get());
    }

    /**
     * Runs a dirty-profile flush on a worker thread and returns a future that
     * completes when the batch has been written. The JDBC work never executes on
     * the calling (Bukkit main) thread; callers that must proceed only after the
     * flush should await the returned future with a bounded timeout.
     *
     * @return a future completed when the flush finishes
     */
    public CompletableFuture<Void> flushDirtyProfilesAsync() {
        return CompletableFuture.runAsync(this::flushDirtyProfiles);
    }

    private void doFlush() {
        // Cooldowns survive a quit/relog, so prune expired entries periodically
        // on the worker thread to keep per-player state bounded.
        if (requirementEngine != null) {
            requirementEngine.pruneExpiredCooldowns();
        }

        Map<UUID, PlayerProfile> dirty = profileManager.getDirtyProfiles();
        if (dirty.isEmpty()) return;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement skillsStmt = conn.prepareStatement(PLAYER_SKILLS_UPSERT);
             PreparedStatement prefsStmt = conn.prepareStatement(PREFERENCES_UPSERT)) {

            // Capture each profile's modCount *before* its XP snapshot so the
            // saved marker never counts mutations the DB write did not include.
            // Reading the counter first means any XP added concurrently leaves
            // modCount > savedModCount and the profile stays dirty for a retry.
            Map<UUID, Long> snapshotModCounts = new HashMap<>();
            for (var entry : dirty.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerProfile profile = entry.getValue();
                long snapshotModCount = profile.getModCount();
                snapshotModCounts.put(uuid, snapshotModCount);
                Map<String, Long> xpSnapshot = profile.getXpSnapshot();

                for (var xpEntry : xpSnapshot.entrySet()) {
                    skillsStmt.setString(1, uuid.toString());
                    skillsStmt.setString(2, xpEntry.getKey());
                    skillsStmt.setLong(3, xpEntry.getValue());
                    skillsStmt.addBatch();
                }
            }

            int[] results = skillsStmt.executeBatch();
            for (int i = 0; i < results.length; i++) {
                if (results[i] == Statement.EXECUTE_FAILED) {
                    plugin.getLogger().warning("Batch entry " + i + " failed during flush");
                    return;
                }
            }

            // Persist preferences for every dirty profile so /skills log and
            // other preference changes ride the async write-behind path. A
            // profile whose hydration prefs-read failed keeps preferencesLoaded
            // false and is skipped: flushing its defaults would permanently
            // overwrite the player's real persisted row.
            for (var entry : dirty.entrySet()) {
                if (!entry.getValue().preferencesLoaded()) continue;
                prefsStmt.setString(1, entry.getKey().toString());
                prefsStmt.setString(2, entry.getValue().getPreferencesJson());
                prefsStmt.addBatch();
            }
            int[] prefResults = prefsStmt.executeBatch();
            for (int result : prefResults) {
                if (result == Statement.EXECUTE_FAILED) {
                    plugin.getLogger().warning("Preference batch entry failed during flush");
                    return;
                }
            }

            dirty.forEach((uuid, profile) -> profile.markSaved(snapshotModCounts.get(uuid)));

        } catch (BatchUpdateException e) {
            plugin.getLogger().log(Level.SEVERE, "Batch flush partially failed", e);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to flush dirty profiles", e);
        }
    }
}
