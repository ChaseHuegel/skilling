package io.github.chasehuegel.skilling.engine.profile;

import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import org.bukkit.entity.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the in-memory cache of {@link PlayerProfile} instances.
 *
 * <p>Profiles are hydrated asynchronously during
 * {@code AsyncPlayerPreLoginEvent} and stored in a
 * {@link ConcurrentHashMap} keyed by player UUID.
 *
 * <p>The {@link AsyncBatchWorker} drains dirty profiles periodically
 * and flushes them to the database.
 */
public final class ProfileManager {

    private final ConcurrentHashMap<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;

    /**
     * Constructs a new profile manager.
     *
     * @param databaseManager the database manager for persistence
     */
    public ProfileManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Asynchronously loads a player's profile from the database.
     *
     * @param playerUuid the player's UUID
     * @return a future that completes with the loaded profile
     */
    public CompletableFuture<PlayerProfile> loadProfile(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerProfile profile = new PlayerProfile(playerUuid);

            if (!databaseManager.isInitialized()) {
                profiles.put(playerUuid, profile);
                return profile;
            }

            String sql = "SELECT skill_id, xp FROM player_skills WHERE player_uuid = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String skillId = rs.getString("skill_id");
                        long xp = rs.getLong("xp");
                        profile.getXpMap().put(skillId, xp);
                    }
                }
            } catch (Exception ignored) {
                // Return empty profile on error
            }

            profiles.put(playerUuid, profile);
            return profile;
        });
    }

    /**
     * Returns a player's profile from the cache.
     *
     * @param playerUuid the player's UUID
     * @return the cached profile, or null if not loaded
     */
    public PlayerProfile getProfile(UUID playerUuid) {
        return profiles.get(playerUuid);
    }

    /**
     * Returns a player's profile, creating one if absent.
     *
     * @param player the player
     * @return the existing or new profile
     */
    public PlayerProfile getOrCreate(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), PlayerProfile::new);
    }

    /**
     * Removes a player's profile from the cache.
     *
     * @param playerUuid the player's UUID
     */
    public void unloadProfile(UUID playerUuid) {
        profiles.remove(playerUuid);
    }

    /**
     * Returns an unmodifiable view of all loaded profiles.
     *
     * @return all cached profiles
     */
    public Map<UUID, PlayerProfile> getAllProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    /**
     * Returns the set of dirty profiles that need saving.
     *
     * @return a snapshot of dirty profiles
     */
    public Map<UUID, PlayerProfile> getDirtyProfiles() {
        Map<UUID, PlayerProfile> dirty = new HashMap<>();
        for (var entry : profiles.entrySet()) {
            if (entry.getValue().isDirty()) {
                dirty.put(entry.getKey(), entry.getValue());
            }
        }
        return dirty;
    }

    /**
     * Returns the total number of cached profiles.
     *
     * @return profile count
     */
    public int size() {
        return profiles.size();
    }
}