package io.github.chasehuegel.skilling.engine.profile;

import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import org.bukkit.entity.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(ProfileManager.class.getName());

    private final ConcurrentHashMap<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> sessionGenerations = new ConcurrentHashMap<>();
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
     * <p>On a transient database failure the profile is NOT treated as
     * authoritative: it is installed uninitialized (in-memory only, never
     * persisted by the write-behind flush) so the player can play on a best-effort
     * basis without the empty placeholder ever overwriting their real persisted XP.
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

            String sql = "SELECT skill_id, xp, fanfare_pending FROM player_skills WHERE player_uuid = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String skillId = rs.getString("skill_id");
                        long xp = rs.getLong("xp");
                        profile.getXpMap().put(skillId, xp);
                        if (rs.getInt("fanfare_pending") > 0) {
                            profile.addPendingFanfare(skillId);
                        }
                    }
                }
            } catch (Exception e) {
                // A transient DB failure must not produce an authoritative empty
                // profile: the write-behind flush would UPSERT the empty values
                // over the player's real rows once they earn any XP. Leave the
                // profile uninitialized so it is in-memory only.
                LOGGER.log(Level.SEVERE, "Failed to hydrate profile for player " + playerUuid
                        + "; using an in-memory-only placeholder", e);
                installPlaceholder(playerUuid, profile);
                return profile;
            }

            loadPreferences(profile, playerUuid);
            profile.markInitialized();

            installHydrated(playerUuid, profile);
            return profile;
        });
    }

    private void installHydrated(UUID playerUuid, PlayerProfile hydrated) {
        // Bump the session generation so an in-flight quit-flush completion knows a
        // new session began. A reconnect keeps the same dirty instance in the map
        // (below), so identity alone cannot tell the completion apart from the old
        // session; the generation can.
        sessionGenerations.compute(playerUuid, (uuid, gen) -> (gen == null ? 0L : gen) + 1L);
        profiles.compute(playerUuid, (uuid, existing) -> {
            if (existing == null) {
                return hydrated;
            }
            if (existing.isInitialized()) {
                // A dirty in-memory profile holds mutations the DB snapshot does
                // not contain; never clobber it with the snapshot.
                return existing.isDirty() ? existing : hydrated;
            }
            // The existing profile is an uninitialized best-effort placeholder whose
            // values are session-only and never persisted (see getDirtyProfiles).
            // Carry the persisted snapshot into it so the player's real progress
            // loads and no session XP is silently dropped, then mark it
            // authoritative. addXp sums the persisted baseline onto the placeholder's
            // session-only delta, which started at zero.
            for (var entry : hydrated.getXpSnapshot().entrySet()) {
                existing.addXp(entry.getKey(), entry.getValue());
            }
            for (String skillId : hydrated.pendingFanfareSkills()) {
                existing.addPendingFanfare(skillId);
            }
            if (existing.getPreferences() == PlayerPreferences.DEFAULTS) {
                existing.setPreferences(hydrated.getPreferences());
            }
            existing.markInitialized();
            return existing;
        });
    }

    /**
     * Installs an in-memory-only placeholder profile for a player whose hydration
     * failed, without disturbing any existing profile. The placeholder stays
     * uninitialized and is therefore never persisted by the write-behind flush.
     *
     * @param playerUuid  the player's UUID
     * @param placeholder the uninitialized placeholder profile
     */
    private void installPlaceholder(UUID playerUuid, PlayerProfile placeholder) {
        sessionGenerations.compute(playerUuid, (uuid, gen) -> (gen == null ? 0L : gen) + 1L);
        profiles.putIfAbsent(playerUuid, placeholder);
    }

    /**
     * Returns the session generation for a player. Each hydration install bumps
     * the generation, so an async quit-flush completion can capture it at quit
     * time and skip removal if the player has since rejoined.
     *
     * @param playerUuid the player's UUID
     * @return the current session generation for the player
     */
    public long sessionGeneration(UUID playerUuid) {
        return sessionGenerations.getOrDefault(playerUuid, 0L);
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
     * Removes a player's profile from the cache only if the cached entry is the
     * given instance AND the session generation still matches the one captured
     * at quit. The generation guard prevents an in-flight quit flush from
     * evicting the live profile of a player who reconnected while the flush ran:
     * the reconnect keeps the same dirty instance in the map (so identity alone
     * would match), but its hydration bumped the generation.
     *
     * @param playerUuid         the player's UUID
     * @param instance           the profile instance that was unloaded
     * @param expectedGeneration the session generation captured at quit
     * @return true if the entry was removed
     */
    public boolean unloadProfile(UUID playerUuid, PlayerProfile instance, long expectedGeneration) {
        if (sessionGenerations.getOrDefault(playerUuid, 0L) != expectedGeneration) {
            return false;
        }
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
     * <p>Uninitialized profiles are never included: they were never hydrated from
     * the database, so flushing them would UPSERT an empty or session-only
     * snapshot over the player's real persisted rows.
     *
     * @return a snapshot of dirty, initialized profiles
     */
    public Map<UUID, PlayerProfile> getDirtyProfiles() {
        Map<UUID, PlayerProfile> dirty = new HashMap<>();
        for (var entry : profiles.entrySet()) {
            PlayerProfile profile = entry.getValue();
            if (profile.isInitialized() && profile.isDirty()) {
                dirty.put(entry.getKey(), profile);
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load preferences for player " + playerUuid, e);
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