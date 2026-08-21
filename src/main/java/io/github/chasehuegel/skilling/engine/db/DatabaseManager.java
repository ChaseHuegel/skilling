package io.github.chasehuegel.skilling.engine.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public final class DatabaseManager {

    private HikariDataSource dataSource;
    private final File dataFolder;

    public DatabaseManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void initialize(YamlConfiguration config) throws SQLException {
        boolean walMode = config.getBoolean("database.wal_mode", true);

        HikariConfig hikariConfig = new HikariConfig();
        // busy_timeout is a per-connection pragma in SQLite; the sqlite-jdbc
        // driver applies it from the URL on every pooled connection. A write that
        // finds the single writer busy waits up to 5s instead of failing with
        // "database is locked".
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "data.db").getAbsolutePath()
                + "?busy_timeout=5000");
        // SQLite permits exactly one writer. A multi-connection pool only adds
        // lock contention that surfaces as SQLITE_BUSY, so cap it at a single
        // connection; the write-behind flush, quit flushes, and offline admin
        // commands serialize on it instead of fighting each other.
        hikariConfig.setMaximumPoolSize(1);
        // foreign_keys is a per-connection pragma in SQLite, so run it on every
        // pooled connection instead of just the bootstrap one.
        hikariConfig.setConnectionInitSql("PRAGMA foreign_keys = ON;");
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setPoolName("skilling-pool");

        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection conn = getConnection()) {
            if (walMode) {
                try (var stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL;");
                    try (ResultSet rs = stmt.getResultSet()) {
                        if (rs.next()) {
                            String mode = rs.getString(1);
                            if (!"wal".equalsIgnoreCase(mode)) {
                                Bukkit.getLogger().warning("Failed to enable WAL mode, got: " + mode);
                            }
                        }
                    }
                }
            }
            createSchema(conn);
        }
    }
    private void createSchema(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_skills (
                        player_uuid TEXT NOT NULL,
                        skill_id TEXT NOT NULL,
                        xp INTEGER NOT NULL DEFAULT 0,
                        fanfare_pending INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, skill_id)
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_preferences (
                        player_uuid TEXT NOT NULL PRIMARY KEY,
                        preferences TEXT NOT NULL DEFAULT '{}'
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_progress (
                        player_uuid TEXT NOT NULL,
                        progress_key TEXT NOT NULL,
                        progress_value TEXT NOT NULL,
                        PRIMARY KEY (player_uuid, progress_key)
                    )
                    """);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database pool is not initialized");
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            try (Connection conn = getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("PRAGMA wal_checkpoint(TRUNCATE);");
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to checkpoint WAL on shutdown", e);
            }
            dataSource.close();
        }
    }

    public boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }
}
