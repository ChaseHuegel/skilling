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
    void setXpMarksDirty() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("mining", 200);
        assertTrue(profile.isDirty());
    }

    @Test
    void markSavedClearsDirty() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        profile.markSaved();
        assertFalse(profile.isDirty());
    }

    @Test
    void markSavedOnlyIfNoConcurrentModifications() {
        var profile = new PlayerProfile(UUID.randomUUID());
        profile.addXp("mining", 100);
        profile.markSaved();
        profile.addXp("mining", 50);
        assertTrue(profile.isDirty());
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
