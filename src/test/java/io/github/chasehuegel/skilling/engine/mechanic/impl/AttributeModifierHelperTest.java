package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies the replace-not-stack behavior of attribute-modifier mechanics:
 * repeated activations sharing a stable {@code uuid} refresh a single modifier
 * instead of stacking, while absent or distinct uuids keep legacy behavior.
 */
class AttributeModifierHelperTest {

    private MockedStatic<RegistryAccess> registryMock;
    private Attribute attribute;

    @BeforeEach
    void setUp() {
        registryMock = mockStatic(RegistryAccess.class);
        RegistryAccess access = mock(RegistryAccess.class);
        registryMock.when(RegistryAccess::registryAccess).thenReturn(access);
        when(access.getRegistry(any(Class.class))).thenAnswer(inv -> makeAttributeRegistry());
        when(access.getRegistry(any(RegistryKey.class))).thenAnswer(inv -> makeAttributeRegistry());
        attribute = Attribute.ARMOR;
    }

    @SuppressWarnings("unchecked")
    private static Registry<Attribute> makeAttributeRegistry() {
        return new Registry<Attribute>() {
            @Override
            public Attribute get(NamespacedKey key) {
                return fakeAttribute(key.getKey());
            }

            @Override
            public NamespacedKey getKey(Attribute entry) {
                return entry.getKey();
            }

            @Override
            public boolean hasTag(TagKey<Attribute> key) {
                return false;
            }

            @Override
            public Tag<Attribute> getTag(TagKey<Attribute> key) {
                return null;
            }

            @Override
            public Collection<Tag<Attribute>> getTags() {
                return List.of();
            }

            @Override
            public Stream<Attribute> stream() {
                return Stream.empty();
            }

            @Override
            public Stream<NamespacedKey> keyStream() {
                return Stream.empty();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public Iterator<Attribute> iterator() {
                return stream().iterator();
            }
        };
    }

    private static Attribute fakeAttribute(String key) {
        NamespacedKey nk = NamespacedKey.minecraft(key);
        return new Attribute() {
            @Override
            public org.bukkit.attribute.Attribute.Sentiment getSentiment() {
                return org.bukkit.attribute.Attribute.Sentiment.POSITIVE;
            }

            @Override
            public int compareTo(Attribute o) {
                return name().compareTo(o.name());
            }

            @Override
            public String name() {
                return key;
            }

            @Override
            public int ordinal() {
                return 0;
            }

            @Override
            public NamespacedKey getKey() {
                return nk;
            }

            @Override
            public String getTranslationKey() {
                return "attribute." + key;
            }

            @Override
            public String translationKey() {
                return "attribute." + key;
            }
        };
    }

    @AfterEach
    void tearDown() {
        if (registryMock != null) {
            registryMock.close();
            registryMock = null;
        }
    }

    /**
     * Builds a player whose attribute instance records every modifier add/remove
     * so the test can assert modifier counts.
     */
    private static Player playerWithRecordingInstance(Attribute attribute, List<AttributeModifier> active) {
        Player player = mock(Player.class);
        AttributeInstance inst = mock(AttributeInstance.class);
        when(player.getAttribute(attribute)).thenReturn(inst);
        doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            active.removeIf(am -> am.getUniqueId().equals(m.getUniqueId()));
            active.add(m);
            return null;
        }).when(inst).addTransientModifier(any(AttributeModifier.class));
        doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            active.removeIf(am -> am.getUniqueId().equals(m.getUniqueId()));
            return null;
        }).when(inst).removeModifier(any(AttributeModifier.class));
        when(inst.getModifier(any(UUID.class))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return active.stream().filter(am -> am.getUniqueId().equals(id)).findFirst().orElse(null);
        });
        when(player.getScheduler()).thenReturn(mock(io.papermc.paper.threadedregions.scheduler.EntityScheduler.class));
        return player;
    }

    @Test
    void absentUuidProducesFreshRandomModifierEachActivation() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = playerWithRecordingInstance(attribute, active);

        assertTrue(AttributeModifierHelper.applyTransient(player, attribute,
                AttributeModifierHelper.resolveUuid(null), "test", 1.0, 5));
        UUID first = active.get(0).getUniqueId();
        assertTrue(AttributeModifierHelper.applyTransient(player, attribute,
                AttributeModifierHelper.resolveUuid(null), "test", 1.0, 5));
        assertEquals(2, active.size());
        assertNotEquals(first, active.get(1).getUniqueId());
    }

    @Test
    void sameUuidReplacesModifierInsteadOfStacking() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = playerWithRecordingInstance(attribute, active);
        UUID uuid = UUID.randomUUID();

        assertTrue(AttributeModifierHelper.applyTransient(player, attribute, uuid, "test", 1.0, 5));
        assertTrue(AttributeModifierHelper.applyTransient(player, attribute, uuid, "test", 1.0, 5));
        assertTrue(AttributeModifierHelper.applyTransient(player, attribute, uuid, "test", 1.0, 5));

        assertEquals(1, active.size());
        assertEquals(uuid, active.get(0).getUniqueId());
    }

    @Test
    void differentUuidsMayCoexist() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = playerWithRecordingInstance(attribute, active);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(AttributeModifierHelper.applyTransient(player, attribute, first, "test", 1.0, 5));
        assertTrue(AttributeModifierHelper.applyTransient(player, attribute, second, "test", 2.0, 5));

        assertEquals(2, active.size());
    }

    @Test
    void malformedUuidFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> AttributeModifierHelper.resolveUuid("not-a-uuid"));
    }
}
