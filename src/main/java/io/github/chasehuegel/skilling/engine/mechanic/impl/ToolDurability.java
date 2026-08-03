package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/**
 * Shared tool-durability cost for chain/harvest mechanics.
 *
 * <p>Each additional block broken costs one durability point, mirroring the
 * vanilla cost (the originating break already consumed one). The cost fires a
 * cancellable {@link PlayerItemDamageEvent} so other plugins can veto, rolls
 * the {@link Enchantment#UNBREAKING Unbreaking} enchantment (each durability
 * point has a 1/(level+1) chance of being consumed), and breaks the tool when
 * it reaches max durability instead of leaving it in an invalid damage state.
 *
 * <p>The random source and the Unbreaking-level reader are injectable so the
 * logic is deterministic under test without a live registry; production always
 * rolls via {@link ThreadLocalRandom} and reads the tool's real enchantments.
 */
final class ToolDurability {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble();
    private static volatile Function<ItemStack, Integer> unbreakingReader = ToolDurability::readUnbreaking;

    private ToolDurability() {}

    /**
     * Test-only seam to force a deterministic Unbreaking roll.
     *
     * @param source the roll source returning a value in [0, 1)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    /**
     * Test-only seam to supply a deterministic Unbreaking level.
     *
     * @param reader reads the Unbreaking level from the tool
     */
    static void setUnbreakingReader(Function<ItemStack, Integer> reader) {
        unbreakingReader = reader;
    }

    /** Restores the production random source and Unbreaking reader. */
    static void reset() {
        randomSource = () -> ThreadLocalRandom.current().nextDouble();
        unbreakingReader = ToolDurability::readUnbreaking;
    }

    /**
     * Costs one durability point on the tool for an additional block broken.
     *
     * @param player the breaking player
     * @param tool   the held tool
     */
    static void damageOnce(Player player, ItemStack tool) {
        if (tool == null || tool.getType() == Material.AIR) return;
        int unbreaking = unbreakingReader.apply(tool);
        // Vanilla Unbreaking: each durability point is consumed with probability
        // 1/(level+1); the roll happens per damage event.
        if (unbreaking > 0 && randomSource.getAsDouble() * (unbreaking + 1) >= 1.0) {
            return;
        }
        PlayerItemDamageEvent damageEvent = new PlayerItemDamageEvent(player, tool, 1);
        Bukkit.getPluginManager().callEvent(damageEvent);
        if (damageEvent.isCancelled()) return;
        applyDamage(player, tool, Math.max(1, damageEvent.getDamage()));
    }

    /**
     * Reads the Unbreaking level from the tool's enchantment map by key, so no
     * {@link Enchantment} constant (and thus no live registry) is required.
     */
    private static int readUnbreaking(ItemStack tool) {
        for (Map.Entry<Enchantment, Integer> entry : tool.getEnchantments().entrySet()) {
            if ("unbreaking".equals(entry.getKey().getKey().getKey())) {
                return entry.getValue();
            }
        }
        return 0;
    }

    private static void applyDamage(Player player, ItemStack tool, int damage) {
        if (tool.getItemMeta() instanceof Damageable damageable) {
            int maxDurability = tool.getType().getMaxDurability();
            int newDamage = damageable.getDamage() + damage;
            if (maxDurability > 0 && newDamage >= maxDurability) {
                // The tool reaches max durability and breaks like a vanilla break
                // rather than resting in an invalid damage state.
                tool.setAmount(0);
                player.getInventory().setItemInMainHand(tool);
            } else {
                damageable.setDamage(newDamage);
                tool.setItemMeta((org.bukkit.inventory.meta.ItemMeta) damageable);
                player.getInventory().setItemInMainHand(tool);
            }
        }
    }
}
