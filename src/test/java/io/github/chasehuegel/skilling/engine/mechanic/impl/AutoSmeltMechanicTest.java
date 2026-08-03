package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.BukkitMock;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the chance-roll semantics of {@link AutoSmeltMechanic}: reaching the
 * roll counts as an activation attempt, so a failed roll cannot be retried for
 * free.
 */
class AutoSmeltMechanicTest {

    @AfterEach
    void tearDown() {
        AutoSmeltMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    /**
     * Enchantment static init needs a RegistryAccess in plain JUnit; provide a
     * registry that hands back a real (anonymous) Enchantment for any key lookup.
     * Both the class and key overloads are stubbed because {@code Registry}'s
     * static initializer resolves legacy registries through {@code getRegistry(Class)}.
     */
    private static MockedStatic<RegistryAccess> mockRegistryAccess() {
        MockedStatic<RegistryAccess> registry = mockStatic(RegistryAccess.class);
        RegistryAccess access = mock(RegistryAccess.class);
        registry.when(RegistryAccess::registryAccess).thenReturn(access);
        when(access.getRegistry(any(Class.class))).thenAnswer(inv -> enchantmentRegistry());
        when(access.getRegistry(any(RegistryKey.class))).thenAnswer(inv -> enchantmentRegistry());
        return registry;
    }

    @SuppressWarnings("unchecked")
    private static Registry<Enchantment> enchantmentRegistry() {
        return new Registry<Enchantment>() {
            @Override
            public Enchantment get(NamespacedKey key) {
                return fakeEnchantment();
            }

            @Override
            public NamespacedKey getKey(Enchantment entry) {
                return entry.getKey();
            }

            @Override
            public boolean hasTag(TagKey<Enchantment> key) {
                return false;
            }

            @Override
            public Tag<Enchantment> getTag(TagKey<Enchantment> key) {
                return null;
            }

            @Override
            public Collection<Tag<Enchantment>> getTags() {
                return List.of();
            }

            @Override
            public Stream<Enchantment> stream() {
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
            public Iterator<Enchantment> iterator() {
                return stream().iterator();
            }
        };
    }

    private static Enchantment fakeEnchantment() {
        return new Enchantment() {
            @Override
            public String getName() {
                return "silk_touch";
            }

            @Override
            public int getMaxLevel() {
                return 1;
            }

            @Override
            public int getStartLevel() {
                return 1;
            }

            @Override
            public EnchantmentTarget getItemTarget() {
                return EnchantmentTarget.ALL;
            }

            @Override
            public boolean isTreasure() {
                return false;
            }

            @Override
            public boolean isCursed() {
                return false;
            }

            @Override
            public boolean conflictsWith(Enchantment other) {
                return false;
            }

            @Override
            public boolean canEnchantItem(ItemStack item) {
                return false;
            }

            @Override
            public net.kyori.adventure.text.Component displayName(int level) {
                return net.kyori.adventure.text.Component.text("Silk Touch");
            }

            @Override
            public boolean isTradeable() {
                return false;
            }

            @Override
            public boolean isDiscoverable() {
                return false;
            }

            @Override
            public int getMinModifiedCost(int level) {
                return 0;
            }

            @Override
            public int getMaxModifiedCost(int level) {
                return 0;
            }

            @Override
            public int getAnvilCost() {
                return 0;
            }

            @Override
            public io.papermc.paper.enchantments.EnchantmentRarity getRarity() {
                return io.papermc.paper.enchantments.EnchantmentRarity.COMMON;
            }

            @Override
            public float getDamageIncrease(int level, org.bukkit.entity.EntityCategory entityCategory) {
                return 0;
            }

            @Override
            public float getDamageIncrease(int level, org.bukkit.entity.EntityType entityType) {
                return 0;
            }

            @Override
            public java.util.Set<org.bukkit.inventory.EquipmentSlotGroup> getActiveSlotGroups() {
                return java.util.Set.of();
            }

            @Override
            public net.kyori.adventure.text.Component description() {
                return net.kyori.adventure.text.Component.text("Silk Touch");
            }

            @Override
            public io.papermc.paper.registry.set.RegistryKeySet<org.bukkit.inventory.ItemType> getSupportedItems() {
                return null;
            }

            @Override
            public io.papermc.paper.registry.set.RegistryKeySet<org.bukkit.inventory.ItemType> getPrimaryItems() {
                return null;
            }

            @Override
            public int getWeight() {
                return 0;
            }

            @Override
            public io.papermc.paper.registry.set.RegistryKeySet<Enchantment> getExclusiveWith() {
                return null;
            }

            @Override
            public String translationKey() {
                return "enchantment.silk_touch";
            }

            @Override
            public NamespacedKey getKey() {
                return NamespacedKey.minecraft("silk_touch");
            }

            @Override
            public String getTranslationKey() {
                return "enchantment.silk_touch";
            }
        };
    }

    private static BlockBreakEvent smeltableBreak(ItemStack hand, List<ItemStack> drops) {
        var event = mock(BlockBreakEvent.class);
        var block = mock(Block.class);
        when(event.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.IRON_ORE);
        when(block.getDrops(any(ItemStack.class))).thenReturn(drops);
        var world = mock(World.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        return event;
    }

    @Test
    void failedRollStillCountsAsActivationAttempt() {
        AutoSmeltMechanic.setRandomSource(() -> 99.0);
        try (MockedStatic<RegistryAccess> registry = mockRegistryAccess()) {
            var mechanic = new AutoSmeltMechanic();
            var player = BukkitMock.mockPlayer();
            var hand = mock(ItemStack.class);
            when(player.getInventory().getItemInMainHand()).thenReturn(hand);
            var raw = mock(ItemStack.class);
            when(raw.getType()).thenReturn(Material.RAW_IRON);
            when(raw.getAmount()).thenReturn(2);
            var event = smeltableBreak(hand, List.of(raw));

            assertTrue(mechanic.execute(player, Map.of("chance", 50.0), event));
            verify(event, never()).setDropItems(false);
            verify(event.getBlock().getWorld(), never())
                    .dropItemNaturally(any(Location.class), any(ItemStack.class));
        }
    }
}
