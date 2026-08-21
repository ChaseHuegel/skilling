package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Raises each chosen enchantment on {@link EnchantItemEvent} by one level, capped
 * at the enchant's own max, with an independent percentage chance per enchant.
 *
 * <p>Reaching the roll step counts as an activation attempt (cost/cooldown
 * consumed) whether or not any enchant actually upgrades, matching the
 * chance-mechanic contract.
 *
 * <p><b>YAML key:</b> {@code core:enchant_level_up}
 * <br>Params: {@code chance} (0-100, percentage for each enchant to gain +1 level)
 */
public final class EnchantLevelUpMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    /** Test seam to force a deterministic roll (0-100). */
    static void setRandomSource(DoubleSupplier source) { randomSource = source; }

    /** Restores the production random source. */
    static void reset() { randomSource = () -> ThreadLocalRandom.current().nextDouble(100); }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EnchantItemEvent enchantEvent)) return false;
        if (!enchantEvent.getEnchanter().equals(player)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        Map<Enchantment, Integer> enchants = enchantEvent.getEnchantsToAdd();
        // Collect the upgrades first, then apply them, so the source map is not
        // mutated while iterated (and a defensive unmodifiable view is preserved).
        java.util.List<Map.Entry<Enchantment, Integer>> upgrades = new java.util.ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            if (entry.getValue() < enchant.getMaxLevel() && randomSource.getAsDouble() < chance) {
                upgrades.add(Map.entry(enchant, entry.getValue() + 1));
            }
        }
        for (Map.Entry<Enchantment, Integer> upgrade : upgrades) {
            enchants.put(upgrade.getKey(), upgrade.getValue());
        }
        return true;
    }
}
