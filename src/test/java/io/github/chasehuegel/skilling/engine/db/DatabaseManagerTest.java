package io.github.chasehuegel.skilling.engine.db;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void initializeOpensTheSqliteDatabaseThroughServiceLoader() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.toFile());
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.wal_mode", false);
        db.initialize(config);

        try (var conn = db.getConnection(); var stmt = conn.createStatement()) {
            assertTrue(stmt.execute("SELECT 1"), "a pooled connection must be usable");
        }
        assertTrue(tempDir.resolve("data.db").toFile().exists(), "initialize must create data.db");
        assertTrue(db.isInitialized());

        db.shutdown();
        assertFalse(db.isInitialized(), "shutdown must close the pool");
    }

    @Test
    void shutdownOnUninitializedPoolIsHarmless() {
        DatabaseManager db = new DatabaseManager(tempDir.toFile());
        db.shutdown();
        assertEquals(false, db.isInitialized());
    }
}
