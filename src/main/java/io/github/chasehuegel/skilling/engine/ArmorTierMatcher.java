package io.github.chasehuegel.skilling.engine;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Classifies a player's worn armor into a tier (light/medium/heavy/none).
 *
 * <p>Used by the {@code state:equipped} filter to gate armor-skill XP and
 * abilities to the correct gear type. All four armor slots must match the
 * requested tier for the filter to pass; {@code none} requires every slot to
 * be empty.
 */
public final class ArmorTierMatcher {

    private ArmorTierMatcher() {}

    /**
     * Returns whether all armor slots match the requested tier.
     *
     * <p>Slot values of {@code null} or {@link Material#AIR} count as empty.
     *
     * @param armor the four armor contents (helmet, chestplate, leggings, boots)
     * @param tier  the requested tier: {@code light}, {@code medium}, {@code heavy}, or {@code none}
     * @return true if every slot matches the tier, false otherwise
     */
    public static boolean matchesArmorTier(ItemStack[] armor, String tier) {
        if (tier.equals("none")) {
            for (ItemStack slot : armor) {
                if (slot != null && slot.getType() != Material.AIR) return false;
            }
            return true;
        }
        for (ItemStack slot : armor) {
            if (slot == null || slot.getType() == Material.AIR) return false;
            if (!matchesTier(slot.getType(), tier)) return false;
        }
        return true;
    }

    private static boolean matchesTier(Material mat, String tier) {
        return switch (tier) {
            case "light" -> mat.name().contains("LEATHER");
            case "medium" -> mat.name().startsWith("CHAINMAIL_") || mat.name().startsWith("IRON_")
                    || mat.name().startsWith("GOLDEN_") || mat == Material.TURTLE_HELMET;
            case "heavy" -> mat.name().startsWith("DIAMOND_") || mat.name().startsWith("NETHERITE_");
            default -> false;
        };
    }
}
