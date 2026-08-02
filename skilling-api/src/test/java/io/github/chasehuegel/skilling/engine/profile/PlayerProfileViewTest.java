package io.github.chasehuegel.skilling.engine.profile;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link PlayerProfileView} contract with a minimal fake
 * implementation: read-only XP access, zero for untracked skills, and a
 * defensive snapshot.
 */
class PlayerProfileViewTest {

    static final class FakeView implements PlayerProfileView {
        private final UUID id;
        private final Map<String, Long> xp;

        FakeView(UUID id, Map<String, Long> xp) {
            this.id = id;
            this.xp = xp;
        }

        @Override
        public UUID getPlayerId() {
            return id;
        }

        @Override
        public long getXp(String skillId) {
            return xp.getOrDefault(skillId, 0L);
        }

        @Override
        public Map<String, Long> getXpSnapshot() {
            return new HashMap<>(xp);
        }

        @Override
        public boolean isInitialized() {
            return true;
        }
    }

    @Test
    void getXpReturnsZeroForUntrackedSkill() {
        var view = new FakeView(UUID.randomUUID(), Map.of());
        assertEquals(0L, view.getXp("mining"));
    }

    @Test
    void getXpReturnsStoredValue() {
        var view = new FakeView(UUID.randomUUID(), Map.of("mining", 150L));
        assertEquals(150L, view.getXp("mining"));
    }

    @Test
    void snapshotIsDefensiveCopy() {
        var view = new FakeView(UUID.randomUUID(), Map.of("mining", 150L));
        var snapshot = view.getXpSnapshot();
        snapshot.put("mining", 999L);
        assertEquals(150L, view.getXp("mining"));
    }

    @Test
    void exposesPlayerIdAndInitializedState() {
        UUID id = UUID.randomUUID();
        var view = new FakeView(id, Map.of());
        assertEquals(id, view.getPlayerId());
        assertTrue(view.isInitialized());
    }
}
