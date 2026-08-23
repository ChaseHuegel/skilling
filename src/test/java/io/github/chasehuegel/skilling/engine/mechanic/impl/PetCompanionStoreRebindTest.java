package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.impl.PetCompanionStore.CompanionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link PetCompanionStore#rebind} carries a captured companion snapshot
 * forward onto a freshly spawned copy with a new active UUID, so the dup-sweep
 * recognizes the respawned pet as canonical.
 */
class PetCompanionStoreRebindTest {

    @Test
    void rebindPreservesFieldsAndUpdatesBoundUuid() {
        UUID oldUuid = UUID.randomUUID();
        UUID newUuid = UUID.randomUUID();
        CompanionSnapshot snapshot = new CompanionSnapshot(
                "WOLF", "Rex", 0.9, 0.3, null, null, 0.0, null, null, oldUuid.toString());

        CompanionSnapshot rebound = PetCompanionStore.rebind(snapshot, newUuid);

        assertEquals("WOLF", rebound.species());
        assertEquals("Rex", rebound.customName());
        assertEquals(0.9, rebound.hpScale());
        assertEquals(0.3, rebound.movementSpeed());
        assertEquals(0.0, rebound.jumpStrength());
        assertEquals(newUuid.toString(), rebound.boundUuid());
    }
}