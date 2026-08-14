package io.github.chasehuegel.skilling.profile;

import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProfileTest {

    @Test
    void freshProfileIsNotDirty() {
        var profile = new PlayerProfile(UUID.randomUUID());
        assertFalse(profile.isDirty());
    }

    @Test
    void addXpMarksDirty() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        assertTrue(profile.isDirty());
    }

    @Test
    void addXpSaturatesInsteadOfOverflowing() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("mining", Long.MAX_VALUE - 1);
        profile.addXp("mining", 100);
        assertEquals(Long.MAX_VALUE, profile.getXp("mining"),
                "XP must saturate at Long.MAX_VALUE, never wrap negative");
    }

    @Test
    void addXpClampsANegativeResultToZero() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", -100);
        assertEquals(0L, profile.getXp("mining"),
                "a genuinely negative result must clamp to zero, not flip to Long.MAX_VALUE");
    }

    @Test
    void addXpNegativeNetStillClampsToZero() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("mining", 10);
        profile.addXp("mining", -40);
        assertEquals(0L, profile.getXp("mining"),
                "a net-negative sum must clamp to zero");
    }

    @Test
    void setXpMarksDirty() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("mining", 200);
        assertTrue(profile.isDirty());
    }

    @Test
    void markSavedClearsDirty() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        profile.markSaved(profile.getModCount());
        assertFalse(profile.isDirty());
    }

    @Test
    void markSavedOnlyIfNoConcurrentModifications() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        profile.markSaved(profile.getModCount());
        profile.addXp("mining", 50);
        assertTrue(profile.isDirty());
    }

    @Test
    void xpAddedBetweenSnapshotAndMarkSavedKeepsProfileDirty() {
        // Simulates the flush race: the snapshot marker is captured, then XP is
        // added concurrently before markSaved. The profile must remain dirty so
        // the next flush persists the newer XP instead of silently dropping it.
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        long snapshotModCount = profile.getModCount();

        profile.addXp("mining", 50);

        profile.markSaved(snapshotModCount);
        assertTrue(profile.isDirty(), "XP gained after the snapshot must keep the profile dirty");
        assertEquals(150L, profile.getXp("mining"));

        // A retry with a fresh snapshot marker then clears the flag.
        profile.markSaved(profile.getModCount());
        assertFalse(profile.isDirty());
    }

    @Test
    void quiescentProfileIsMarkedCleanBySnapshotMarker() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        long snapshotModCount = profile.getModCount();

        profile.markSaved(snapshotModCount);
        assertFalse(profile.isDirty());
    }

    @Test
    void getXpReturnsZeroForUnknown() {
        var profile = new PlayerProfile(UUID.randomUUID());
        assertEquals(0L, profile.getXp("nonexistent"));
    }

    @Test
    void getXpReturnsSetValue() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("woodcutting", 500);
        assertEquals(500L, profile.getXp("woodcutting"));
    }

    @Test
    void addXpAccumulates() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        profile.addXp("mining", 50);
        assertEquals(150L, profile.getXp("mining"));
    }

    @Test
    void multipleSkillsTrackedIndependently() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        profile.addXp("woodcutting", 200);
        assertEquals(100L, profile.getXp("mining"));
        assertEquals(200L, profile.getXp("woodcutting"));
    }

    @Test
    void getPlayerIdReturnsCorrectUuid() {
        UUID uuid = UUID.randomUUID();
        var profile = new PlayerProfile(uuid);
        assertEquals(uuid, profile.getPlayerId());
    }
}
