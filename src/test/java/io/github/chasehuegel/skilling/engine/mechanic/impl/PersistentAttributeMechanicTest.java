package io.github.chasehuegel.skilling.engine.mechanic.impl;

import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link PersistentAttributeMechanic}: a level-scaled transient
 * modifier under a stable marker key that replaces (never stacks), is idempotent
 * per amount, removes on a non-positive amount, clamps max health, and strips
 * every owned modifier during reconciliation.
 */
class PersistentAttributeMechanicTest {

    private static final UUID UUID_1 = UUID.fromString("3f2b9c4a-1e5d-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Builds a player whose attribute instance records add/remove calls into the
     * given live list, so tests can assert modifier counts and content.
     */
    private static Player recordingPlayer(Attribute attribute, List<AttributeModifier> active) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        AttributeInstance inst = mock(AttributeInstance.class);
        when(player.getAttribute(attribute)).thenReturn(inst);
        when(inst.getModifier(any(Key.class))).thenAnswer(inv -> {
            Key key = inv.getArgument(0);
            return active.stream().filter(m -> m.getKey().equals(key)).findFirst().orElse(null);
        });
        when(inst.getModifiers()).thenReturn(active);
        org.mockito.Mockito.doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            active.removeIf(am -> am.getKey().equals(m.getKey()));
            active.add(m);
            return null;
        }).when(inst).addTransientModifier(any(AttributeModifier.class));
        org.mockito.Mockito.doAnswer(inv -> {
            Key key = inv.getArgument(0);
            active.removeIf(am -> am.getKey().equals(key));
            return null;
        }).when(inst).removeModifier(any(Key.class));
        org.mockito.Mockito.doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            active.removeIf(am -> am.getKey().equals(m.getKey()));
            return null;
        }).when(inst).removeModifier(any(AttributeModifier.class));
        return player;
    }

    @Test
    void applyAddsTransientModifierWithMarkerKey() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = recordingPlayer(Attribute.MAX_HEALTH, active);

        assertTrue(PersistentAttributeMechanic.applyPersistent(player, Attribute.MAX_HEALTH, UUID_1, 10.0));
        assertEquals(1, active.size());
        assertEquals(PersistentAttributeMechanic.modifierKey(UUID_1), active.get(0).getKey());
        assertEquals(10.0, active.get(0).getAmount());
    }

    @Test
    void sameAmountIsNoOp() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = recordingPlayer(Attribute.ARMOR, active);

        assertTrue(PersistentAttributeMechanic.applyPersistent(player, Attribute.ARMOR, UUID_1, 5.0));
        assertFalse(PersistentAttributeMechanic.applyPersistent(player, Attribute.ARMOR, UUID_1, 5.0));
        assertEquals(1, active.size(), "an identical re-apply must be a no-op");
    }

    @Test
    void differentAmountReplacesInsteadOfStacking() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = recordingPlayer(Attribute.ARMOR, active);

        assertTrue(PersistentAttributeMechanic.applyPersistent(player, Attribute.ARMOR, UUID_1, 5.0));
        assertTrue(PersistentAttributeMechanic.applyPersistent(player, Attribute.ARMOR, UUID_1, 8.0));

        assertEquals(1, active.size(), "the same UUID must replace, not stack");
        assertEquals(8.0, active.get(0).getAmount());
    }

    @Test
    void nonPositiveAmountRemovesTheModifier() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = recordingPlayer(Attribute.ARMOR, active);

        assertTrue(PersistentAttributeMechanic.applyPersistent(player, Attribute.ARMOR, UUID_1, 5.0));
        assertFalse(PersistentAttributeMechanic.applyPersistent(player, Attribute.ARMOR, UUID_1, 0.0));
        assertTrue(active.isEmpty(), "a non-positive amount must remove the modifier");
        assertFalse(PersistentAttributeMechanic.applyPersistent(player, Attribute.ARMOR, UUID_1, -3.0));
        assertTrue(active.isEmpty());
    }

    @Test
    void missingAttributeInstanceIsNoOp() {
        Player player = mock(Player.class);
        when(player.getAttribute(any())).thenReturn(null);

        assertFalse(PersistentAttributeMechanic.applyPersistent(player, Attribute.ARMOR, UUID_1, 5.0));
    }

    @Test
    void maxHealthApplyClampsHealthAboveNewCap() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = recordingPlayer(Attribute.MAX_HEALTH, active);
        when(player.getHealth()).thenReturn(50.0);
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        when(maxHealth.getValue()).thenReturn(40.0);

        PersistentAttributeMechanic.applyPersistent(player, Attribute.MAX_HEALTH, UUID_1, 10.0);

        verify(player).setHealth(40.0);
    }

    @Test
    void executeResolvesAttributeAndAppliesEvenWithNullEvent() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = recordingPlayer(Attribute.MAX_HEALTH, active);
        // The registry hands back a mock attribute under the real JVM; route any
        // resolved attribute onto the recording instance.
        AttributeInstance recording = player.getAttribute(Attribute.MAX_HEALTH);
        when(player.getAttribute(any())).thenReturn(recording);

        // The reconcile path passes a null event; the mechanic must not need it.
        assertTrue(new PersistentAttributeMechanic().execute(
                player, Map.of("attribute", "minecraft:max_health", "amount", 5.0, "uuid", UUID_1.toString()), null));
        assertEquals(1, active.size());
    }

    @Test
    void executeWithoutUuidIsNoOp() {
        Player player = recordingPlayer(Attribute.ARMOR, new ArrayList<>());
        assertFalse(new PersistentAttributeMechanic().execute(
                player, Map.of("attribute", "minecraft:armor", "amount", 5.0), null));
        verify(player, never()).getAttribute(any());
    }

    @Test
    void stripRemovesOnlyOwnedModifiers() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = recordingPlayer(Attribute.MAX_HEALTH, active);
        when(player.getHealth()).thenReturn(30.0);
        when(player.getAttribute(Attribute.MAX_HEALTH).getValue()).thenReturn(30.0);

        // Seed the registry of touched attributes, then add a foreign modifier.
        assertTrue(PersistentAttributeMechanic.applyPersistent(player, Attribute.MAX_HEALTH, UUID_1, 10.0));
        UUID foreignUuid = UUID.randomUUID();
        active.add(new AttributeModifier(foreignUuid, "foreign", 3.0, AttributeModifier.Operation.ADD_NUMBER));

        PersistentAttributeMechanic.stripPersistentModifiers(player);

        assertEquals(1, active.size(), "only the owned modifier must be stripped");
        assertEquals(foreignUuid, active.get(0).getUniqueId());
    }

    @Test
    void modifierKeyIsStableAndNamespaced() {
        NamespacedKey key = PersistentAttributeMechanic.modifierKey(UUID_1);
        assertEquals(PersistentAttributeMechanic.MODIFIER_NAMESPACE, key.getNamespace());
        assertTrue(key.getKey().startsWith(PersistentAttributeMechanic.MODIFIER_KEY_PREFIX + "_"));
        assertEquals(PersistentAttributeMechanic.modifierKey(UUID_1), key);
    }
}