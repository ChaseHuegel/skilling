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
     * <p>The hydrated profile is installed only when no newer dirty in-memory
     * profile exists; a dirty profile holds mutations that the DB snapshot does
     * not contain, so replacing it would lose XP.
     *
     * @param playerUuid the player's UUID
     * @return a future that completes with the loaded profile
     */
    public CompletableFuture<PlayerProfile> loadProfile(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerProfile profile = new PlayerProfile(playerUuid);

            if (!databaseManager.isInitialized()) {
                profile.markInitialized();
                installHydrated(playerUuid, profile);
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

            loadPreferences(profile, playerUuid);
            profile.markInitialized();

            installHydrated(playerUuid, profile);
            return profile;
        });
    }

    private void installHydrated(UUID playerUuid, PlayerProfile hydrated) {
        profiles.compute(playerUuid, (uuid, existing) -> {
            if (existing != null && existing.isDirty()) {
                return existing;
            }
            return hydrated;
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
     * Returns a player's profile from cache, creating an empty profile and inserting it
     * if one does not already exist.
     *
     * <p>This is a synchronous operation intended for use during gameplay where an
     * existing profile is expected. Profiles are normally loaded and marked
     * initialized asynchronously during login via {@link #loadProfile}, which is
     * awaited before the player joins, so the empty fallback is only returned for a
     * player whose cached profile is unexpectedly absent. The fallback is left
     * uninitialized so callers can distinguish it from hydrated data; any hydration
     * that later lands is installed by {@link #installHydrated}, which never
     * clobbers mutations made after this call.
     *
     * @param player the player
     * @return the existing or newly created profile
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
     * Removes a player's profile from the cache only if the cached entry is the
     * given instance. Prevents an in-flight quit flush from evicting a newer
     * profile installed by a reconnect.
     *
     * @param playerUuid the player's UUID
     * @param instance   the profile instance that was unloaded
     * @return true if the entry was removed
     */
    public boolean unloadProfile(UUID playerUuid, PlayerProfile instance) {
        return profiles.remove(playerUuid, instance);
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

    private void loadPreferences(PlayerProfile profile, UUID playerUuid) {
        if (!databaseManager.isInitialized()) return;
        String sql = "SELECT preferences FROM player_preferences WHERE player_uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    profile.setPreferencesFromJson(rs.getString("preferences"));
                }
            }
        } catch (Exception ignored) {
            // Use defaults on error
        }
    }

    /**
     * Saves a player's preferences to the database.
     *
     * @param playerUuid the player's UUID
     * @param json       the JSON-serialized preferences
     */
    public void savePreferences(UUID playerUuid, String json) {
        if (!databaseManager.isInitialized()) return;
        String sql = "INSERT INTO player_preferences (player_uuid, preferences) VALUES (?, ?) ON CONFLICT(player_uuid) DO UPDATE SET preferences = excluded.preferences";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, json);
            stmt.executeUpdate();
        } catch (Exception ignored) {
            // Best-effort save
        }
    }
}