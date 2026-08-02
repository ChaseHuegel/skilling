package io.github.chasehuegel.skilling.profile;

import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FanfarePendingTest {

    @TempDir
    Path tempDir;

    private DatabaseManager newDatabase() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.toFile());
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.wal_mode", false);
        db.initialize(config);
        return db;
    }

    /** Mirrors the offline /skills setlevel/addxp command's DB write. */
    private void seedFlaggedRow(DatabaseManager db, UUID uuid, String skillId, long xp) throws Exception {
        String sql = "INSERT INTO player_skills (player_uuid, skill_id, xp, fanfare_pending) "
                + "VALUES (?, ?, ?, 1) ON CONFLICT(player_uuid, skill_id) "
                + "DO UPDATE SET xp = ?, fanfare_pending = 1";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, skillId);
            stmt.setLong(3, xp);
            stmt.setLong(4, xp);
            stmt.executeUpdate();
        }
    }

    private long readFlag(DatabaseManager db, UUID uuid, String skillId) throws Exception {
        String sql = "SELECT fanfare_pending FROM player_skills WHERE player_uuid = ? AND skill_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, skillId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    @Test
    void profileLoadReadsAndConsumesPendingFanfare() throws Exception {
        DatabaseManager db = newDatabase();
        UUID uuid = UUID.randomUUID();
        seedFlaggedRow(db, uuid, "mining", 5000);

        ProfileManager manager = new ProfileManager(db);
        PlayerProfile profile = manager.loadProfile(uuid).join();

        assertTrue(profile.hasPendingFanfare("mining"), "flagged skill must surface on profile load");
        assertEquals(5000L, profile.getXp("mining"));

        assertTrue(profile.consumePendingFanfare("mining"));
        assertFalse(profile.hasPendingFanfare("mining"));
        assertFalse(profile.consumePendingFanfare("mining"), "fanfare must be consumed exactly once");
    }

    @Test
    void flushClearsPendingFlagSoLaterLoginDoesNotRefire() throws Exception {
        DatabaseManager db = newDatabase();
        UUID uuid = UUID.randomUUID();
        seedFlaggedRow(db, uuid, "mining", 5000);

        ProfileManager manager = new ProfileManager(db);
        PlayerProfile profile = manager.loadProfile(uuid).join();
        profile.consumePendingFanfare("mining");

        // Flush with the real AsyncBatchWorker UPSERT SQL (sets fanfare_pending = 0).
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(AsyncBatchWorker.PLAYER_SKILLS_UPSERT)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, "mining");
            stmt.setLong(3, profile.getXp("mining"));
            stmt.addBatch();
            stmt.executeBatch();
        }

        assertEquals(0L, readFlag(db, uuid, "mining"), "flush must clear fanfare_pending in the DB");

        // A later login hydrates with no pending fanfare, so fanfare never re-fires.
        manager.unloadProfile(uuid);
        PlayerProfile reloaded = manager.loadProfile(uuid).join();
        assertFalse(reloaded.hasPendingFanfare("mining"));
    }
}
