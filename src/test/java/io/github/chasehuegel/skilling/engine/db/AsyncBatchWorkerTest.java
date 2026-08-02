package io.github.chasehuegel.skilling.engine.db;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerPreferences;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncBatchWorkerTest {

    @TempDir
    Path tempDir;

    private DatabaseManager newDatabase() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.toFile());
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.wal_mode", false);
        db.initialize(config);
        return db;
    }

    private AsyncBatchWorker newWorker(DatabaseManager db, ProfileManager profileManager) {
        Skilling plugin = mock(Skilling.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger(AsyncBatchWorkerTest.class.getName()));
        return new AsyncBatchWorker(plugin, db, profileManager);
    }

    @Test
    void asyncFlushPersistsXpAndPreferencesOffTheCallingThread() throws Exception {
        DatabaseManager db = newDatabase();
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = profileManager.loadProfile(uuid).join();
        profile.setXp("mining", 500);
        profile.setPreferences(new PlayerPreferences(true, false, false, false));

        newWorker(db, profileManager).flushDirtyProfilesAsync().get(5, TimeUnit.SECONDS);

        assertFalse(profile.isDirty(), "flush must mark the profile clean when quiescent");

        try (Connection conn = db.getConnection();
             PreparedStatement xpStmt = conn.prepareStatement(
                 "SELECT xp FROM player_skills WHERE player_uuid = ? AND skill_id = ?");
             PreparedStatement prefStmt = conn.prepareStatement(
                 "SELECT preferences FROM player_preferences WHERE player_uuid = ?")) {

            xpStmt.setString(1, uuid.toString());
            xpStmt.setString(2, "mining");
            try (var rs = xpStmt.executeQuery()) {
                assertEquals(true, rs.next());
                assertEquals(500L, rs.getLong(1));
            }

            prefStmt.setString(1, uuid.toString());
            try (var rs = prefStmt.executeQuery()) {
                assertEquals(true, rs.next());
                String json = rs.getString(1);
                assertEquals(true, json.contains("\"logXp\":true") || json.contains("\"logXp\" : true"),
                    "preferences must be persisted by the flush: " + json);
            }
        }
    }
}
