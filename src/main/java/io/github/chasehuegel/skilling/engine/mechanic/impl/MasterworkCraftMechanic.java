package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Adds a single random, compatible enchantment to a freshly crafted item on a
 * level-scaled chance roll. Powers the Smithing skill's mastery capstone: a
 * master smith forges "masterwork" gear that comes out naturally enchanted.
 *
 * <p>On {@link CraftItemEvent} a candidate enchant is chosen from the item's
 * full compatible pool, skipping enchants already present and those that
 * conflict, and the bonus level is a random {@code 1..max}. The roll happens on
 * the crafted result, so the gear is enchanted at the anvil-free moment of
 * crafting. Reaching the roll counts as an activation attempt (cost/cooldown
 * consumed) whether or not it succeeds, matching the chance-mechanic contract.
 *
 * <p><b>YAML key:</b> {@code core:masterwork_craft}
 * <br>Params: {@code chance} (0-100, percentage to add a bonus enchant)
 */
public final class MasterworkCraftMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);
    private static volatile DoubleSupplier levelRandom = ThreadLocalRandom.current()::nextDouble;
    private static volatile Supplier<Collection<Enchantment>> enchantSource =
            () -> Registry.ENCHANTMENT.stream().toList();

    /** Test seam to force a deterministic chance roll (0-100). */
    static void setRandomSource(DoubleSupplier source) { randomSource = source; }

    /** Test seam to force a deterministic bonus level roll (0.0-1.0). */
    static void setLevelRandom(DoubleSupplier source) { levelRandom = source; }

    /** Test seam to supply the candidate enchantment pool without a live registry. */
    static void setEnchantSource(Supplier<Collection<Enchantment>> source) { enchantSource = source; }

    /** Restores the production sources. */
    static void reset() {
        randomSource = () -> ThreadLocalRandom.current().nextDouble(100);
        levelRandom = ThreadLocalRandom.current()::nextDouble;
        enchantSource = () -> Registry.ENCHANTMENT.stream().toList();
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof CraftItemEvent craftEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        if (randomSource.getAsDouble() >= chance) return true;

        ItemStack result = craftEvent.getCurrentItem();
        if (result == null || result.isEmpty()) return true;

        Enchantment bonus = pickBonus(result);
        if (bonus != null) {
            int maxLevel = Math.max(1, bonus.getMaxLevel());
            int level = 1 + (int) (levelRandom.getAsDouble() * maxLevel);
            result.addEnchantment(bonus, Math.min(level, maxLevel));
            craftEvent.setCurrentItem(result);
        }
        return true;
    }

    private Enchantment pickBonus(ItemStack item) {
        List<Enchantment> occupied = new ArrayList<>(item.getEnchantments().keySet());
        List<Enchantment> candidates = new ArrayList<>();
        for (Enchantment e : enchantSource.get()) {
            if (e == null || occupied.contains(e)) continue;
            if (!e.canEnchantItem(item)) continue;
            if (conflictsWithAny(e, occupied)) continue;
            candidates.add(e);
        }
        if (candidates.isEmpty()) return null;
        int index = (int) (randomSource.getAsDouble() / 100.0 * candidates.size());
        if (index >= candidates.size()) index = candidates.size() - 1;
        return candidates.get(Math.max(0, index));
    }

    private static boolean conflictsWithAny(Enchantment enchant, List<Enchantment> occupied) {
        for (Enchantment other : occupied) {
            if (enchant.conflictsWith(other)) return true;
        }
        return false;
    }
}