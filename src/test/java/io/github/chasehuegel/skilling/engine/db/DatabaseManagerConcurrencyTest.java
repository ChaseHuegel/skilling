package io.github.chasehuegel.skilling.engine.db;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the SQLite pool does not surface "database is locked" when the
 * periodic flush, a quit flush, and offline admin writes contend for the single
 * SQLite writer. The 5s {@code busy_timeout} plus the single-connection pool
 * serialize the writers instead of failing them.
 */
class DatabaseManagerConcurrencyTest {

    @TempDir
    Path tempDir;

    private DatabaseManager newDatabase() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.toFile());
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.wal_mode", false);
        db.initialize(config);
        return db;
    }

    @Test
    void pooledConnectionsRunBusyTimeoutAndForeignKeys() throws Exception {
        DatabaseManager db = newDatabase();
        try (Connection conn = db.getConnection();
             java.sql.ResultSet rs = conn.createStatement().executeQuery("PRAGMA busy_timeout");
             java.sql.ResultSet fk = conn.createStatement().executeQuery("PRAGMA foreign_keys")) {
            assertTrue(rs.next());
            assertEquals(5000, rs.getInt(1), "the pool must wait out transient write locks");
            assertTrue(fk.next());
            assertEquals(1, fk.getInt(1), "foreign keys must stay enabled on every pooled connection");
        }
        db.shutdown();
    }

    @Test
    void concurrentWritersNeverSurfaceSqliteBusy() throws Exception {
        DatabaseManager db = newDatabase();
        int writers = 8;
        int writesPerWriter = 10;
        UUID[] uuids = java.util.stream.IntStream.range(0, writers)
                .mapToObj(i -> UUID.randomUUID())
                .toArray(UUID[]::new);
        CountDownLatch gate = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicReference<String> failure = new AtomicReference<>();

        var threads = java.util.stream.IntStream.range(0, writers).mapToObj(w -> new Thread(() -> {
            try {
                gate.await();
                for (int i = 0; i < writesPerWriter; i++) {
                    String uuid = uuids[w].toString();
                    // Hold the SQLite write lock for the whole transaction so
                    // concurrent writers genuinely contend for it, mirroring a
                    // flush batch or a quit flush while an offline command writes.
                    try (Connection conn = db.getConnection();
                         Statement tx = conn.createStatement()) {
                        tx.execute("BEGIN IMMEDIATE");
                        try (PreparedStatement stmt = conn.prepareStatement("""
                                 INSERT INTO player_skills (player_uuid, skill_id, xp, fanfare_pending)
                                 VALUES (?, 'mining', ?, 0)
                                 ON CONFLICT(player_uuid, skill_id) DO UPDATE SET xp = xp + ?, fanfare_pending = 0
                                 """)) {
                            stmt.setString(1, uuid);
                            stmt.setLong(2, 1);
                            stmt.setLong(3, 1);
                            stmt.executeUpdate();
                        }
                        try (PreparedStatement sel = conn.prepareStatement(
                                "SELECT xp FROM player_skills WHERE player_uuid = ? AND skill_id = 'mining'")) {
                            sel.setString(1, uuid);
                            try (var rs = sel.executeQuery()) {
                                assertTrue(rs.next(), "the write must be visible to a subsequent read");
                            }
                        }
                        // Keep the write lock long enough that other threads'
                        // BEGIN IMMEDIATE genuinely collide with this transaction.
                        Thread.sleep(2);
                        tx.execute("COMMIT");
                    }
                }
            } catch (Throwable t) {
                failed.set(true);
                failure.compareAndSet(null, t.toString());
            }
        })).toList();
        threads.forEach(Thread::start);
        gate.countDown();
        for (Thread t : threads) t.join(60_000);

        assertFalse(failed.get(), "no writer may fail with a lock error: " + failure.get());
        db.shutdown();
    }
}
