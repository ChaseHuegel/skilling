package io.github.chasehuegel.skilling.engine;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code state:equipped} armor-tier classification in
 * {@link ArmorTierMatcher}.
 */
class StateFilterEquippedTest {

    private static ItemStack item(Material mat) {
        var stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(mat);
        return stack;
    }

    private static ItemStack[] fullSet(Material mat) {
        return new ItemStack[]{item(mat), item(mat), item(mat), item(mat)};
    }

    private static ItemStack[] mixed(ItemStack... slots) {
        return slots;
    }

    @Test
    void fullLeatherSetMatchesLightOnly() {
        var set = fullSet(Material.LEATHER_HELMET);
        assertTrue(ArmorTierMatcher.matchesArmorTier(set, "light"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "heavy"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "none"));
    }

    @Test
    void fullDiamondSetMatchesHeavyOnly() {
        var set = fullSet(Material.DIAMOND_BOOTS);
        assertTrue(ArmorTierMatcher.matchesArmorTier(set, "heavy"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "light"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "none"));
    }

    @Test
    void mixedSetFailsAllTiers() {
        var set = mixed(item(Material.LEATHER_HELMET), item(Material.LEATHER_CHESTPLATE),
                item(Material.LEATHER_LEGGINGS), item(Material.DIAMOND_BOOTS));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "light"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "heavy"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "none"));
    }

    @Test
    void emptySlotsMatchNoneOnly() {
        var set = new ItemStack[4];
        assertTrue(ArmorTierMatcher.matchesArmorTier(set, "none"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "light"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "heavy"));
    }

    @Test
    void airItemsCountAsEmpty() {
        var set = mixed(item(Material.AIR), item(Material.AIR), item(Material.AIR), item(Material.AIR));
        assertTrue(ArmorTierMatcher.matchesArmorTier(set, "none"));
    }

    @Test
    void fullChainmailSetMatchesMedium() {
        var set = fullSet(Material.CHAINMAIL_LEGGINGS);
        assertTrue(ArmorTierMatcher.matchesArmorTier(set, "medium"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "light"));
    }

    @Test
    void partialSetFailsNonNoneTiers() {
        var set = mixed(item(Material.IRON_HELMET), null, item(Material.IRON_LEGGINGS), item(Material.IRON_BOOTS));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "medium"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "light"));
        assertFalse(ArmorTierMatcher.matchesArmorTier(set, "heavy"));
    }
}
