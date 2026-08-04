package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ModifyAttackSpeedMechanic;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModifyAttackSpeedMechanicTest {

    private final ModifyAttackSpeedMechanic mechanic = new ModifyAttackSpeedMechanic();
    private MockedStatic<RegistryAccess> registryMock;

    /**
     * Installs a fake {@link RegistryAccess} so the {@code Attribute} interface static
     * fields resolve outside a running server. The registry lazily returns a fake
     * {@link Attribute} for any key, satisfying both the {@code Registry} and
     * {@code Attribute} class initializers.
     */
    private void bootstrapAttributeRegistry() {
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

    @Test
    void returnsFalseWithMultiplierZero() {
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 0.0), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWithMultiplierOne() {
        bootstrapAttributeRegistry();
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("multiplier", 1.0), BukkitMock.mockBlockBreakEvent()));
    }
}
