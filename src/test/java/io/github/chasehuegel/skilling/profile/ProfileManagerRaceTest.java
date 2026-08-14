package io.github.chasehuegel.skilling.profile;

import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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
    void unloadWithGenerationEvictsTheSessionGenerationEntry() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();
        profile.addXp("mining", 500);
        long generationAtQuit = manager.sessionGeneration(uuid);
        assertEquals(1L, generationAtQuit);

        // A successful no-reconnect unload must also drop the generation entry so
        // sessionGenerations cannot grow without bound across unique players.
        assertTrue(manager.unloadProfile(uuid, profile, generationAtQuit));
        assertEquals(0L, manager.sessionGeneration(uuid),
                "the generation entry must be evicted with the unloaded profile");
    }

    @Test
    void unloadWithoutGenerationEvictsTheSessionGenerationEntry() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();
        assertTrue(manager.unloadProfile(uuid, profile));
        assertEquals(0L, manager.sessionGeneration(uuid),
                "the generation entry must be evicted with the unloaded profile");
    }

    @Test
    void reconnectedProfileKeepsItsGenerationEntry() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();
        long generationAtQuit = manager.sessionGeneration(uuid);
        // A reconnect while the quit flush is in flight bumps the generation and
        // the unload is rejected; the entry must stay for the live session.
        manager.loadProfile(uuid).join();
        assertFalse(manager.unloadProfile(uuid, profile, generationAtQuit));
        assertTrue(manager.sessionGeneration(uuid) > generationAtQuit,
                "a reconnected player must keep a live generation entry");
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
    void rejoinHydrationDoesNotClobberXpWithInFlightQuitFlush() throws Exception {
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(true);
        ProfileManager manager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();

        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        doReturn(conn).when(db).getConnection();
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);

        // First session: persisted XP is 100 for mining.
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("skill_id")).thenReturn("mining");
        when(rs.getLong("xp")).thenReturn(100L);
        when(rs.getInt("fanfare_pending")).thenReturn(0);

        PlayerProfile profile = manager.loadProfile(uuid).join();
        assertEquals(100L, profile.getXp("mining"));

        // The player earns more XP, then quits: the quit-flush writes the newer
        // value and marks the profile clean.
        profile.addXp("mining", 20);
        profile.markSaved(profile.getModCount());

        // Rejoin hydration reads a STALE snapshot (the DB read races the flush's
        // write and the mock never saw the +20). Pause the read mid-result-set so
        // the test can simulate the flush completing while the read is in flight.
        java.util.concurrent.CountDownLatch readStarted = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch releaseRead = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger row = new java.util.concurrent.atomic.AtomicInteger();
        when(rs.next()).thenAnswer(inv -> {
            if (row.getAndIncrement() == 0) {
                return true; // the stale persisted row (100)
            }
            readStarted.countDown();
            try {
                releaseRead.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false; // end of result set
        });

        java.util.concurrent.CompletableFuture<PlayerProfile> rejoin = manager.loadProfile(uuid);
        assertTrue(readStarted.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "rejoin hydration must start reading before the flush completes");
        // The in-flight quit-flush completes while the hydration holds the stale row.
        manager.noteFlushCompleted(uuid);
        releaseRead.countDown();
        rejoin.join();

        // The newer in-memory XP (120) must win over the stale snapshot (100).
        assertSame(profile, manager.getProfile(uuid),
                "the live profile must win over a stale DB snapshot");
        assertEquals(120L, manager.getProfile(uuid).getXp("mining"),
                "newer in-memory XP must not be clobbered by the stale snapshot");
    }

    @Test
    void loadProfileInstallsInitializedProfile() {
        ProfileManager manager = newManager();
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();
        assertTrue(profile.isInitialized());
        assertSame(profile, manager.getProfile(uuid));
    }

    @Test
    void hydrationFailureNeverPersistsEmptyProfile() throws Exception {
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(true);
        doThrow(new java.sql.SQLException("database is locked")).when(db).getConnection();
        ProfileManager manager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();

        PlayerProfile profile = manager.loadProfile(uuid).join();

        assertFalse(profile.isInitialized(), "failed hydration must not be authoritative");
        assertSame(profile, manager.getProfile(uuid), "player still gets an in-memory profile");

        // Even once the player earns XP, the uninitialized placeholder must never
        // be offered to the write-behind flush, so it cannot overwrite persisted XP.
        profile.addXp("mining", 50);
        assertTrue(manager.getDirtyProfiles().isEmpty(),
                "an uninitialized profile must never be flushed over persisted rows");
    }

    @Test
    void successfulRehydrationCarriesPersistedXpIntoPlaceholder() throws Exception {
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(true);
        doThrow(new java.sql.SQLException("database is locked")).when(db).getConnection();
        ProfileManager manager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();

        // First login hits a DB failure; the player plays on the placeholder.
        PlayerProfile placeholder = manager.loadProfile(uuid).join();
        assertFalse(placeholder.isInitialized());
        placeholder.addXp("mining", 50);

        // DB recovers; persisted XP is 1000 for mining.
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("skill_id")).thenReturn("mining");
        when(rs.getLong("xp")).thenReturn(1000L);
        when(rs.getInt("fanfare_pending")).thenReturn(0);
        doReturn(conn).when(db).getConnection();
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);

        manager.loadProfile(uuid).join();

        PlayerProfile recovered = manager.getProfile(uuid);
        assertSame(placeholder, recovered, "the live placeholder must be upgraded, not replaced");
        assertTrue(recovered.isInitialized(), "recovery must mark the profile authoritative");
        assertEquals(1050L, recovered.getXp("mining"),
                "persisted XP must load and session-only XP must be carried on top");
    }
}
