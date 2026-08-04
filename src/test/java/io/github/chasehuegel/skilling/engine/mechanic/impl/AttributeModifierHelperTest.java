package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
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
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
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
        when(access.getRegistry(any(Class.class))).thenAnswer(inv -> {
            Class<?> requested = inv.getArgument(0);
            return Attribute.class.equals(requested)
                    ? makeAttributeRegistry()
                    : io.github.chasehuegel.skilling.testutil.FakeRegistryAccess.registryFor(requested);
        });
        when(access.getRegistry(any(RegistryKey.class))).thenAnswer(inv -> {
            RegistryKey<?> requested = inv.getArgument(0);
            return requested == RegistryKey.ATTRIBUTE
                    ? makeAttributeRegistry()
                    : io.github.chasehuegel.skilling.testutil.FakeRegistryAccess.registryFor(requested);
        });
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
     * so the test can assert modifier counts. The entity scheduler hands back a
     * mock task per scheduled removal so the helper can track them.
     */
    private static Player playerWithRecordingInstance(Attribute attribute, List<AttributeModifier> active) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
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
        doAnswer(inv -> {
            UUID id = inv.getArgument(0);
            active.removeIf(am -> am.getUniqueId().equals(id));
            return null;
        }).when(inst).removeModifier(any(UUID.class));
        when(inst.getModifier(any(UUID.class))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return active.stream().filter(am -> am.getUniqueId().equals(id)).findFirst().orElse(null);
        });
        EntityScheduler scheduler = mock(EntityScheduler.class);
        when(scheduler.runDelayed(any(), any(), any(), anyLong()))
                .thenReturn(mock(ScheduledTask.class));
        when(player.getScheduler()).thenReturn(scheduler);
        return player;
    }

    /**
     * Builds a scheduler that records every scheduled removal consumer and its
     * task, returning a fresh task per call so the test can drive the removal.
     */
    private static EntityScheduler capturingScheduler(List<Consumer<ScheduledTask>> removals,
                                                      List<ScheduledTask> tasks) {
        EntityScheduler scheduler = mock(EntityScheduler.class);
        when(scheduler.runDelayed(any(), any(), any(), anyLong())).thenAnswer(inv -> {
            removals.add(inv.getArgument(1));
            ScheduledTask task = mock(ScheduledTask.class);
            when(task.isCancelled()).thenReturn(false);
            tasks.add(task);
            return task;
        });
        return scheduler;
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
    void refreshCancelsStaleRemovalSoBuffLastsFullNewDuration() {
        List<AttributeModifier> active = new ArrayList<>();
        List<Consumer<ScheduledTask>> removals = new ArrayList<>();
        List<ScheduledTask> tasks = new ArrayList<>();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        AttributeInstance inst = mock(AttributeInstance.class);
        when(player.getAttribute(attribute)).thenReturn(inst);
        doAnswer(inv -> {
            active.removeIf(m -> m.getUniqueId().equals(((AttributeModifier) inv.getArgument(0)).getUniqueId()));
            active.add(inv.getArgument(0));
            return null;
        }).when(inst).addTransientModifier(any(AttributeModifier.class));
        doAnswer(inv -> {
            active.removeIf(m -> m.getUniqueId().equals(inv.getArgument(0)));
            return null;
        }).when(inst).removeModifier(any(UUID.class));
        when(inst.getModifier(any(UUID.class))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return active.stream().filter(m -> m.getUniqueId().equals(id)).findFirst().orElse(null);
        });
        EntityScheduler scheduler = capturingScheduler(removals, tasks);
        when(player.getScheduler()).thenReturn(scheduler);

        UUID uuid = UUID.randomUUID();
        assertTrue(AttributeModifierHelper.applyTransient(player, attribute, uuid, "test", 1.0, 10));
        assertTrue(AttributeModifierHelper.applyTransient(player, attribute, uuid, "test", 1.0, 10));

        // The stale first removal must be cancelled so it cannot fire early and
        // cut the refreshed buff back to the previous expiry.
        verify(tasks.get(0)).cancel();
        assertEquals(1, active.size());
        // The buff lives until the replacement removal fires.
        removals.get(1).accept(tasks.get(1));
        assertTrue(active.isEmpty());
    }

    @Test
    void malformedUuidFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> AttributeModifierHelper.resolveUuid("not-a-uuid"));
    }

    @Test
    void clearAllStripsModifiersAndClearsTracker() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            List<AttributeModifier> active = new ArrayList<>();
            Player player = playerWithRecordingInstance(attribute, active);
            UUID uuid = UUID.randomUUID();
            when(Bukkit.getOnlinePlayers()).thenAnswer(inv -> List.of(player));

            assertTrue(AttributeModifierHelper.applyTransient(player, attribute, uuid, "test", 1.0, 60));
            assertEquals(1, active.size());
            assertEquals(1, AttributeModifierHelper.pendingRemovalsSize());

            // Plugin disable / reload: the scheduled removal tasks would be
            // retired without running, so the modifier must be stripped directly.
            AttributeModifierHelper.clearAll();

            assertTrue(active.isEmpty(), "transient modifier must be removed from the player");
            assertEquals(0, AttributeModifierHelper.pendingRemovalsSize(),
                    "the pending-removal tracker must be emptied");
        }
    }

    @Test
    void clearAllDoesNothingWhenNothingPending() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getOnlinePlayers()).thenReturn(List.of());
            AttributeModifierHelper.clearAll();
            assertEquals(0, AttributeModifierHelper.pendingRemovalsSize());
        }
    }
}
