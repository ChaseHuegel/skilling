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

    @Test
    void failedPreferenceLoadNeverOverwritesThePersistedRow() throws Exception {
        DatabaseManager db = newDatabase();
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();

        // Seed a real, non-default preferences row.
        PlayerProfile seed = new PlayerProfile(uuid);
        seed.setPreferences(new PlayerPreferences(true, false, false, false));
        String realJson = seed.getPreferencesJson();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO player_preferences (player_uuid, preferences) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, realJson);
            stmt.executeUpdate();
        }

        // Simulate a hydration whose prefs-read failed: the profile holds defaults
        // and was never marked preferencesLoaded, yet is initialized and dirty on
        // XP. Mirror the production path by installing it without a prefs load.
        PlayerProfile profile = profileManager.getOrCreate(mockPlayer(uuid));
        profile.setXp("mining", 100);
        profile.markInitialized();

        newWorker(db, profileManager).flushDirtyProfilesAsync().get(5, TimeUnit.SECONDS);

        // The XP must persist, but the real preferences row must survive untouched.
        try (Connection conn = db.getConnection();
             PreparedStatement xpStmt = conn.prepareStatement(
                     "SELECT xp FROM player_skills WHERE player_uuid = ? AND skill_id = 'mining'");
             PreparedStatement prefStmt = conn.prepareStatement(
                     "SELECT preferences FROM player_preferences WHERE player_uuid = ?")) {
            xpStmt.setString(1, uuid.toString());
            try (var rs = xpStmt.executeQuery()) {
                assertEquals(true, rs.next());
                assertEquals(100L, rs.getLong(1));
            }
            prefStmt.setString(1, uuid.toString());
            try (var rs = prefStmt.executeQuery()) {
                assertEquals(true, rs.next());
                assertEquals(realJson, rs.getString(1),
                        "a failed prefs load must not overwrite the persisted row with defaults");
            }
        }
    }

    @Test
    void successfulPreferenceLoadResumesNormalFlushing() throws Exception {
        DatabaseManager db = newDatabase();
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();

        String realJson;
        {
            PlayerProfile seed = new PlayerProfile(uuid);
            seed.setPreferences(new PlayerPreferences(true, false, false, false));
            realJson = seed.getPreferencesJson();
        }
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO player_preferences (player_uuid, preferences) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, realJson);
            stmt.executeUpdate();
        }

        // A fresh hydration loads the preferences successfully, so the next flush
        // must persist them again (the "later successful load" edge case).
        PlayerProfile profile = profileManager.loadProfile(uuid).join();
        profile.setPreferences(new PlayerPreferences(true, true, false, false));

        newWorker(db, profileManager).flushDirtyProfilesAsync().get(5, TimeUnit.SECONDS);

        try (Connection conn = db.getConnection();
             PreparedStatement prefStmt = conn.prepareStatement(
                     "SELECT preferences FROM player_preferences WHERE player_uuid = ?")) {
            prefStmt.setString(1, uuid.toString());
            try (var rs = prefStmt.executeQuery()) {
                assertEquals(true, rs.next());
                String json = rs.getString(1);
                assertEquals(true, json.contains("\"logXp\":true"),
                        "a later successful load must resume flushing preferences: " + json);
            }
        }
    }

    @Test
    void contendedPeriodicFlushIsDeferredAndNotSilentlyDropped() throws Exception {
        DatabaseManager db = newDatabase();
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        PlayerProfile profile = profileManager.loadProfile(uuid).join();
        profile.setXp("mining", 500);

        AsyncBatchWorker worker = newWorker(db, profileManager);

        // Simulate a quit-flush holding the write lock while the periodic run fires:
        // the run must queue a deferred flush instead of silently skipping.
        java.lang.reflect.Field lockField = AsyncBatchWorker.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        var lock = (java.util.concurrent.locks.ReentrantLock) lockField.get(worker);
        java.lang.reflect.Field deferredField = AsyncBatchWorker.class.getDeclaredField("deferredFlush");
        deferredField.setAccessible(true);
        var deferred = (java.util.concurrent.atomic.AtomicBoolean) deferredField.get(worker);

        // Hold the lock on a separate thread so run()'s tryLock genuinely fails
        // (ReentrantLock would let the same thread re-acquire it).
        var held = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                held.countDown();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        holder.start();
        held.await();

        worker.run();
        assertEquals(true, deferred.get(), "a contended periodic run must queue a deferred flush");

        release.countDown();
        holder.join();

        // The holder's next pass consumes the deferred request and persists the
        // profile (including XP earned after the contention) promptly.
        profile.setXp("mining", 900);
        worker.flushDirtyProfiles();
        assertEquals(false, deferred.get(), "the deferred flush must be consumed");

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT xp FROM player_skills WHERE player_uuid = ? AND skill_id = 'mining'")) {
            stmt.setString(1, uuid.toString());
            try (var rs = stmt.executeQuery()) {
                assertEquals(true, rs.next());
                assertEquals(900L, rs.getLong(1));
            }
        }
    }

    private static org.bukkit.entity.Player mockPlayer(UUID uuid) {
        org.bukkit.entity.Player player = mock(org.bukkit.entity.Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }
}
