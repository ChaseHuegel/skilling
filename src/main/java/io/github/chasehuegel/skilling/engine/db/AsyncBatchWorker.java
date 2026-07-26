package io.github.chasehuegel.skilling.engine.db;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.Bukkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * A repeating asynchronous task that drains dirty {@link PlayerProfile}
 * instances and executes a batched SQL {@code UPSERT}.
 *
 * <p>Runs every 3-5 minutes (configurable) on the Bukkit async scheduler.
 * During {@code onDisable()}, a final synchronous flush is triggered to
 * prevent data loss.
 *
 * <p>Uses the Write-Behind Cache pattern: profiles are marked dirty in-memory,
 * and this worker periodically persists them without blocking the main thread.
 */
public final class AsyncBatchWorker implements Runnable {

    private static final long INTERVAL_TICKS = 20 * 240; // ~4 minutes at 20 TPS

    private final Skilling plugin;
    private final DatabaseManager databaseManager;
    private final ProfileManager profileManager;
    private int taskId = -1;

    /**
     * Constructs a new batch worker.
     *
     * @param plugin           the Skilling plugin instance
     * @param databaseManager  the database manager for connections
     * @param profileManager   the profile manager providing dirty profiles
     */
    public AsyncBatchWorker(Skilling plugin, DatabaseManager databaseManager, ProfileManager profileManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.profileManager = profileManager;
    }

    /**
     * Starts the repeating async task.
     */
    public void start() {
        this.taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, INTERVAL_TICKS, INTERVAL_TICKS).getTaskId();
    }

    /**
     * Stops the repeating task.
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public void run() {
        flushDirtyProfiles();
    }

    /**
     * Performs a synchronous flush of all dirty profiles.
     * Called during {@code onDisable()} to prevent data loss.
     */
    public void flushDirtyProfiles() {
        Map<UUID, PlayerProfile> dirty = profileManager.getDirtyProfiles();
        if (dirty.isEmpty()) return;

        String sql = """
                INSERT INTO player_skills (player_uuid, skill_id, xp)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid, skill_id) DO UPDATE SET xp = excluded.xp
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (var entry : dirty.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerProfile profile = entry.getValue();

                for (var xpEntry : profile.getXpMap().entrySet()) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, xpEntry.getKey());
                    stmt.setLong(3, xpEntry.getValue());
                    stmt.addBatch();
                }
            }

            stmt.executeBatch();
            dirty.values().forEach(PlayerProfile::markClean);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to flush dirty profiles", e);
        }
    }
}