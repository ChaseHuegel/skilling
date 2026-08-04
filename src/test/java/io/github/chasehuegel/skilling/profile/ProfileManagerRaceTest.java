package io.github.chasehuegel.skilling.profile;

import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileManagerRaceTest {

    /**
     * A manager backed by an uninitialized database: loadProfile skips SQL and
     * installs an empty profile, which is enough to exercise the install/merge
     * and unload races deterministically.
     */
    private ProfileManager newManager() {
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        return new ProfileManager(db);
    }

    @Test
    void quitFlushCompletingAfterReconnectDoesNotEvictNewProfile() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        // First session loads profile A.
        PlayerProfile oldProfile = manager.loadProfile(uuid).join();

        // Reconnect installs a newer profile B (A is clean, so it is replaced).
        PlayerProfile newProfile = manager.loadProfile(uuid).join();
        assertSame(newProfile, manager.getProfile(uuid));

        // The old session's async quit-flush completes and tries to unload A;
        // it must not evict the freshly installed B.
        boolean removed = manager.unloadProfile(uuid, oldProfile);

        assertFalse(removed, "old profile instance should not be removable when a newer entry exists");
        assertSame(newProfile, manager.getProfile(uuid));
    }

    @Test
    void quitFlushCompletingAfterReconnectKeepsSameDirtyInstance() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        // First session loads profile A and dirties it.
        PlayerProfile profile = manager.loadProfile(uuid).join();
        profile.addXp("mining", 500);

        // Quit captures the session generation for this session.
        long generationAtQuit = manager.sessionGeneration(uuid);

        // Reconnect while the quit flush is in flight: installHydrated keeps the
        // SAME dirty instance in the map (existing.isDirty() is true).
        manager.loadProfile(uuid).join();
        assertSame(profile, manager.getProfile(uuid), "dirty instance must be retained across the reconnect");

        // The old session's async quit-flush completion must not evict the live
        // profile of the now-online player, even though the instance matches.
        boolean removed = manager.unloadProfile(uuid, profile, generationAtQuit);

        assertFalse(removed, "quit flush must not evict the live profile of a reconnected player");
        assertSame(profile, manager.getProfile(uuid), "reconnected player keeps a functional cached profile");
        assertEquals(500L, manager.getProfile(uuid).getXp("mining"), "XP reads must persist");
    }

    @Test
    void quitFlushCompletingWithoutReconnectStillUnloads() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();
        profile.addXp("mining", 500);
        long generationAtQuit = manager.sessionGeneration(uuid);

        // No reconnect happened, so the generation still matches and the flush
        // completion may evict the offline profile.
        boolean removed = manager.unloadProfile(uuid, profile, generationAtQuit);

        assertTrue(removed, "offline profile must still be unloaded when no reconnect occurred");
        assertEquals(null, manager.getProfile(uuid));
    }

    @Test
    void unloadProfileRemovesMatchingInstanceOnly() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();
        assertTrue(manager.unloadProfile(uuid, profile));
        assertFalse(manager.unloadProfile(uuid, profile), "second unload must be a no-op");
    }

    @Test
    void asyncHydrationDoesNotOverwriteXpMutatedAfterLoadBegan() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();
        profile.addXp("mining", 500);

        // A second hydration snapshot lands after the profile was mutated; the
        // dirty in-memory profile must win over the DB snapshot.
        manager.loadProfile(uuid).join();

        assertSame(profile, manager.getProfile(uuid));
        assertEquals(500L, manager.getProfile(uuid).getXp("mining"));
    }

    @Test
    void getOrCreateFallbackKeepsMutationsWhenHydrationArrives() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        PlayerProfile fallback = manager.getOrCreate(player);
        assertFalse(fallback.isInitialized(), "fallback profile must not claim hydration");
        assertEquals(0L, fallback.getXp("mining"));

        fallback.addXp("mining", 100);
        manager.loadProfile(uuid).join();

        assertSame(fallback, manager.getProfile(uuid));
        assertEquals(100L, fallback.getXp("mining"));
    }

    @Test
    void loadProfileInstallsInitializedProfile() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();
        assertTrue(profile.isInitialized());
        assertSame(profile, manager.getProfile(uuid));
    }
}
