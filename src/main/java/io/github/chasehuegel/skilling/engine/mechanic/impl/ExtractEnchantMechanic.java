package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Strips the highest-level enchantment off the held item and writes it into an
 * enchanted book on a sneak + right-click of a grindstone.
 *
 * <p>The vanilla grindstone GUI is suppressed (the interact is cancelled) so the
 * act reads as "sneak + interact = extract" while a normal interact keeps the
 * vanilla GUI. Only non-book enchanted items are valid sources: enchanted books
 * are excluded so a single book cannot be looped into two. The item keeps its
 * remaining enchantments; the extracted book is added to the inventory (dropped
 * if full). Returns a no-op (false) without spending the ability's cost when the
 * held item is unenchanted or a book, as when the interact target is not a
 * grindstone.
 *
 * <p>Item durability cost is handled separately by the {@code core:durability}
 * requirement cost, keeping this mechanic to a single responsibility.
 *
 * <p><b>YAML key:</b> {@code core:extract_enchant}
 * <br>Params: none
 */
public final class ExtractEnchantMechanic implements SkillMechanic {

    private static volatile Supplier<ItemStack> bookFactory =
            () -> new ItemStack(Material.ENCHANTED_BOOK);

    /** Test seam to supply a book without a live server registry. */
    static void setBookFactory(Supplier<ItemStack> factory) { bookFactory = factory; }

    /** Restores the production book factory. */
    static void reset() { bookFactory = () -> new ItemStack(Material.ENCHANTED_BOOK); }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        // The grindstone target filter already gated this mechanic; suppress the
        // vanilla GUI so the sneak-interact act is the extraction.
        interact.setCancelled(true);

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) return false;
        // Enchanted books cannot seed a duplication loop: they are never a source.
        if (held.getType() == Material.ENCHANTED_BOOK) return false;

        Map<Enchantment, Integer> enchants = held.getEnchantments();
        Map.Entry<Enchantment, Integer> best = null;
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            if (best == null || entry.getValue() > best.getValue()) best = entry;
        }
        if (best == null) return false;

        held.removeEnchantment(best.getKey());
        player.getInventory().setItemInMainHand(held);

        ItemStack book = bookFactory.get();
        if (book.getItemMeta() instanceof EnchantmentStorageMeta storage) {
            storage.addStoredEnchant(best.getKey(), best.getValue(), true);
            book.setItemMeta(storage);
        }
        player.getInventory().addItem(book).values().forEach(
                leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
        return true;
    }
}
