package io.github.chasehuegel.skilling.testutil;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockType;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

/**
 * Test-only {@link RegistryAccess} installed via
 * {@code META-INF/services/io.papermc.paper.registry.RegistryAccess} so the
 * Bukkit/Paper static registries ({@code Registry.ATTRIBUTE}, {@code ITEM},
 * {@code ENCHANTMENT}, ...) initialize successfully in a plain-JUnit JVM no
 * matter which test class touches them first.
 *
 * <p>Bukkit's {@code Registry} static initializer runs exactly once per JVM; if
 * it throws (no server provides a real {@code RegistryAccess}) the failure is
 * cached permanently and every later test that reaches a registry-backed
 * constant ({@code Attribute.MAX_HEALTH}, {@code Material.getMaxDurability()},
 * ...) dies with {@code NoClassDefFoundError}. Tests that intentionally mock
 * {@code RegistryAccess} still work — their {@code mockStatic} simply takes
 * precedence for that test's lifetime.
 *
 * <p>Each registry hands back a fresh Mockito mock (or, for enum value types,
 * the first enum constant) for any key lookup so {@code getOrThrow}-style
 * consumers see a non-null entry.
 */
public final class FakeRegistryAccess implements RegistryAccess {

    private static final Map<RegistryKey<?>, Class<?>> KEY_TYPES = keyTypes();

    /** A real {@code DataComponentType} that is both {@code Valued} and
     * {@code NonValued}, so {@code DataComponentTypes}' static initializer
     * (which {@code instanceof}-checks each registry hit) can complete in a
     * plain-JUnit JVM. A Mockito mock of {@code DataComponentType} is neither
     * subtype, so the init would fail-and-cache. */
    private static final DataComponentType FAKE_COMPONENT = new FakeComponent();

    /** Implements both {@code Valued} and {@code NonValued} so {@code
     * DataComponentTypes}' static initializer ({@code instanceof}-checks each
     * registry hit) can complete in a plain-JUnit JVM; a mock is neither
     * subtype and would fail init. */
    @SuppressWarnings("rawtypes")
    private static final class FakeComponent implements DataComponentType.Valued, DataComponentType.NonValued {
        @Override
        public boolean isPersistent() {
            return true;
        }

        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("fake_component");
        }
    }

    @Override
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return registryOf(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> registryKey) {
        return (Registry<T>) registryFor(registryKey);
    }

    /**
     * Builds a type-appropriate fake registry for the given value type, so a
     * registry that only ever handed back one type (e.g. attributes for every
     * key) can delegate everything it does not own here and never poison other
     * registries during {@code Registry} static initialization.
     *
     * @param type the registry value type
     * @return a fake registry returning an entry per key lookup
     */
    public static Registry<? extends Keyed> registryFor(Class<?> type) {
        return registryOf(type);
    }

    /**
     * Builds a type-appropriate fake registry for the given registry key.
     *
     * @param key the registry key
     * @return a fake registry returning an entry per key lookup
     */
    public static Registry<? extends Keyed> registryFor(RegistryKey<?> key) {
        return registryOf(KEY_TYPES.getOrDefault(key, Keyed.class));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Registry<T> registryOf(Class<?> type) {
        return new Registry<T>() {
            @Override
            public T get(NamespacedKey key) {
                return (T) entry(type);
            }

            @Override
            public NamespacedKey getKey(T entry) {
                return entry.getKey();
            }

            @Override
            public boolean hasTag(TagKey<T> key) {
                return false;
            }

            @Override
            public Tag<T> getTag(TagKey<T> key) {
                return null;
            }

            @Override
            public Collection<Tag<T>> getTags() {
                return List.of();
            }

            @Override
            public Stream<T> stream() {
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
            public Iterator<T> iterator() {
                return stream().iterator();
            }
        };
    }

    private static Object entry(Class<?> type) {
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            if (constants.length > 0) return constants[0];
        }
        // ItemType cannot be instrumented by Mockito (a supertype fails to
        // initialize), but Material.getMaxDurability() treats a null ItemType
        // as "no durability", so null is a safe entry for the ITEM registry.
        if (ItemType.class.equals(type)) return null;
        // BlockType likewise cannot be instrumented by Mockito in a plain-JUnit
        // JVM; ItemStack.getType()/getBlockType() lookups on a mocked stack
        // would otherwise fail. null is the safe entry for the BLOCK registry.
        if (BlockType.class.equals(type)) return null;
        // DataComponentType must be a real Valued+NonValued instance so the
        // DataComponentTypes static initializer's instanceof checks pass.
        if (DataComponentType.class.equals(type)) return FAKE_COMPONENT;
        return mock(type);
    }

    private static Map<RegistryKey<?>, Class<?>> keyTypes() {
        Map<RegistryKey<?>, Class<?>> map = new HashMap<>();
        map.put(RegistryKey.ATTRIBUTE, Attribute.class);
        map.put(RegistryKey.BLOCK, BlockType.class);
        map.put(RegistryKey.ENTITY_TYPE, EntityType.class);
        map.put(RegistryKey.ITEM, ItemType.class);
        map.put(RegistryKey.MOB_EFFECT, PotionEffectType.class);
        map.put(RegistryKey.PARTICLE_TYPE, org.bukkit.Particle.class);
        map.put(RegistryKey.POTION, PotionType.class);
        map.put(RegistryKey.MENU, org.bukkit.inventory.MenuType.class);
        map.put(RegistryKey.SOUND_EVENT, org.bukkit.Sound.class);
        map.put(RegistryKey.GAME_EVENT, org.bukkit.GameEvent.class);
        map.put(RegistryKey.DATA_COMPONENT_TYPE, io.papermc.paper.datacomponent.DataComponentType.class);
        map.put(RegistryKey.FLUID, org.bukkit.Fluid.class);
        map.put(RegistryKey.STRUCTURE_TYPE, org.bukkit.StructureType.class);
        map.put(RegistryKey.VILLAGER_PROFESSION, org.bukkit.entity.Villager.Profession.class);
        map.put(RegistryKey.VILLAGER_TYPE, org.bukkit.entity.Villager.Type.class);
        map.put(RegistryKey.MAP_DECORATION_TYPE, org.bukkit.map.MapCursor.Type.class);
        // The biome key is exercised by legacy Registry.BIOME consumers.
        map.put(RegistryKey.BIOME, org.bukkit.block.Biome.class);
        return Map.copyOf(map);
    }
}
