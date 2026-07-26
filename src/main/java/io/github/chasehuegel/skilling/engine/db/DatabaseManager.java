package io.github.chasehuegel.skilling.engine.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages the HikariCP connection pool for SQLite persistence.
 *
 * <p>Initializes the database in WAL (Write-Ahead Logging) mode at
 * startup for concurrent read/write performance. Creates the required
 * schema tables if they do not exist.
 *
 * <p>YAML configuration keys:
 * <ul>
 *   <li>{@code database.pool_size} — max pool size (default: 10)</li>
 *   <li>{@code database.wal_mode} — enable WAL mode (default: true)</li>
 * </ul>
 */
public final class DatabaseManager {

    private HikariDataSource dataSource;
    private final File dataFolder;

    /**
     * Constructs a new database manager.
     *
     * @param dataFolder the plugin's data folder for the SQLite file
     */
    public DatabaseManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    /**
     * Initializes the HikariCP pool and ensures the database schema exists.
     *
     * @param config the plugin config for pool settings
     * @throws SQLException if pool initialization fails
     */
    public void initialize(YamlConfiguration config) throws SQLException {
        int poolSize = config.getInt("database.pool_size", 10);
        boolean walMode = config.getBoolean("database.wal_mode", true);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "data.db").getAbsolutePath());
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setPoolName("skilling-pool");

        // SQLite-specific optimizations
        hikariConfig.addDataSourceProperty("journal_mode", walMode ? "WAL" : "DELETE");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
        hikariConfig.addDataSourceProperty("foreign_keys", "ON");

        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection conn = getConnection()) {
            if (walMode) {
                try (var stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL;");
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
        }
    }

    /**
     * Returns a connection from the pool.
     *
     * @return a pooled connection
     * @throws SQLException if a connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database pool is not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Gracefully shuts down the connection pool.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Whether the pool is initialized and open.
     *
     * @return true if the pool is active
     */
    public boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }
}