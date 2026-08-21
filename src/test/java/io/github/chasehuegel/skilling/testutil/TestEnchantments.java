package io.github.chasehuegel.skilling.testutil;

import io.papermc.paper.enchantments.EnchantmentRarity;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test-only helpers for {@link Enchantment}: Mockito cannot instrument
 * {@code Enchantment} in a plain-JUnit JVM, so tests that need enchantment values
 * use a real anonymous subclass and stub {@link RegistryAccess} to satisfy the
 * enchantment registry during class initialization.
 */
public final class TestEnchantments {

    private TestEnchantments() {}

    /**
     * Opens a mocked {@link RegistryAccess} that delegates to the shared
     * {@link FakeRegistryAccess}, answering the enchantment registry with one that
     * hands back anonymous fakes so {@code Enchantment}'s static initializer (and
     * any {@code Registry.ENCHANTMENT} access) succeeds in plain JUnit.
     *
     * @return the open static mock; caller closes it (ideally via try-with-resources)
     */
    public static MockedStatic<RegistryAccess> mockAccess() {
        MockedStatic<RegistryAccess> registry = mockStatic(RegistryAccess.class);
        RegistryAccess access = mock(RegistryAccess.class);
        registry.when(RegistryAccess::registryAccess).thenReturn(access);
        when(access.getRegistry(any(Class.class))).thenAnswer(inv -> {
            Class<?> requested = inv.getArgument(0);
            return Enchantment.class.equals(requested)
                    ? enchantmentRegistry()
                    : FakeRegistryAccess.registryFor(requested);
        });
        when(access.getRegistry(any(RegistryKey.class))).thenAnswer(inv -> {
            RegistryKey<?> requested = inv.getArgument(0);
            return RegistryKey.ENCHANTMENT.equals(requested)
                    ? enchantmentRegistry()
                    : FakeRegistryAccess.registryFor(requested);
        });
        return registry;
    }

    /**
     * Builds a distinct anonymous {@link Enchantment} fake with the given behavior.
     *
     * @param id         namespaced key / identity (also its translation key)
     * @param maxLevel   the enchant's max level
     * @param canEnchant whether {@code canEnchantItem} returns true
     * @param conflicts  whether {@code conflictsWith} returns true for any other enchant
     * @return a fresh anonymous enchantment
     */
    public static Enchantment fake(String id, int maxLevel, boolean canEnchant, boolean conflicts) {
        NamespacedKey key = NamespacedKey.minecraft(id);
        return new Enchantment() {
            @Override public String getName() { return id; }
            @Override public int getMaxLevel() { return maxLevel; }
            @Override public int getStartLevel() { return 1; }
            @Override public EnchantmentTarget getItemTarget() { return EnchantmentTarget.ALL; }
            @Override public boolean isTreasure() { return false; }
            @Override public boolean isCursed() { return false; }
            @Override public boolean conflictsWith(Enchantment other) { return conflicts; }
            @Override public boolean canEnchantItem(ItemStack item) { return canEnchant; }
            @Override public net.kyori.adventure.text.Component displayName(int level) {
                return net.kyori.adventure.text.Component.text(id);
            }
            @Override public boolean isTradeable() { return false; }
            @Override public boolean isDiscoverable() { return false; }
            @Override public int getMinModifiedCost(int level) { return 0; }
            @Override public int getMaxModifiedCost(int level) { return 0; }
            @Override public int getAnvilCost() { return 0; }
            @Override public EnchantmentRarity getRarity() { return EnchantmentRarity.COMMON; }
            @Override public float getDamageIncrease(int level, EntityCategory category) { return 0; }
            @Override public float getDamageIncrease(int level, EntityType type) { return 0; }
            @Override public Set<EquipmentSlotGroup> getActiveSlotGroups() { return Set.of(); }
            @Override public net.kyori.adventure.text.Component description() {
                return net.kyori.adventure.text.Component.text(id);
            }
            @Override public io.papermc.paper.registry.set.RegistryKeySet<org.bukkit.inventory.ItemType> getSupportedItems() { return null; }
            @Override public io.papermc.paper.registry.set.RegistryKeySet<org.bukkit.inventory.ItemType> getPrimaryItems() { return null; }
            @Override public int getWeight() { return 0; }
            @Override public io.papermc.paper.registry.set.RegistryKeySet<Enchantment> getExclusiveWith() { return null; }
            @Override public String translationKey() { return "enchantment." + id; }
            @Override public String getTranslationKey() { return "enchantment." + id; }
            @Override public NamespacedKey getKey() { return key; }
        };
    }

    @SuppressWarnings("unchecked")
    private static Registry<Enchantment> enchantmentRegistry() {
        return new Registry<Enchantment>() {
            @Override public Enchantment get(NamespacedKey key) { return fake("default", 3, true, false); }
            @Override public NamespacedKey getKey(Enchantment entry) { return entry.getKey(); }
            @Override public boolean hasTag(TagKey<Enchantment> key) { return false; }
            @Override public Tag<Enchantment> getTag(TagKey<Enchantment> key) { return null; }
            @Override public Collection<Tag<Enchantment>> getTags() { return List.of(); }
            @Override public Stream<Enchantment> stream() { return Stream.empty(); }
            @Override public Stream<NamespacedKey> keyStream() { return Stream.empty(); }
            @Override public int size() { return 0; }
            @Override public Iterator<Enchantment> iterator() { return stream().iterator(); }
        };
    }
}
