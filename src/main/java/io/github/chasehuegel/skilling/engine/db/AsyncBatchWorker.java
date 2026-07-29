package io.github.chasehuegel.skilling.engine.db;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.Bukkit;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public final class AsyncBatchWorker implements Runnable {

    private static final long INTERVAL_TICKS = 20 * 240;

    private final Skilling plugin;
    private final DatabaseManager databaseManager;
    private final ProfileManager profileManager;
    private final ReentrantLock lock = new ReentrantLock();
    private int taskId = -1;

    public AsyncBatchWorker(Skilling plugin, DatabaseManager databaseManager, ProfileManager profileManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.profileManager = profileManager;
    }

    public void start() {
        this.taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, INTERVAL_TICKS, INTERVAL_TICKS).getTaskId();
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
                doFlush();
            } finally {
                lock.unlock();
            }
        }
    }

    public void flushDirtyProfiles() {
        lock.lock();
        try {
            doFlush();
        } finally {
            lock.unlock();
        }
    }

    private void doFlush() {
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
                Map<String, Long> xpSnapshot = profile.getXpSnapshot();

                for (var xpEntry : xpSnapshot.entrySet()) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, xpEntry.getKey());
                    stmt.setLong(3, xpEntry.getValue());
                    stmt.addBatch();
                }
            }

            int[] results = stmt.executeBatch();
            for (int i = 0; i < results.length; i++) {
                if (results[i] == Statement.EXECUTE_FAILED) {
                    plugin.getLogger().warning("Batch entry " + i + " failed during flush");
                    return;
                }
            }
            dirty.values().forEach(PlayerProfile::markSaved);

        } catch (BatchUpdateException e) {
            plugin.getLogger().log(Level.SEVERE, "Batch flush partially failed", e);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to flush dirty profiles", e);
        }
    }
}
