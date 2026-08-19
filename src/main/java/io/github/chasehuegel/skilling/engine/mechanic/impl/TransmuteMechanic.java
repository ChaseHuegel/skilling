package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Converts a held stack of one material into another on a right-click of a
 * cauldron (the alchemy transmutation topic).
 *
 * <p>Each {@code core:transmute} mechanic entry carries a single source-&gt;product
 * swap; the ability's "transmute table" is expressed as several of these
 * mechanics so it stays within the scalar evaluator schema. The mechanic reads
 * the player's main-hand item: when it matches {@code source} with at least
 * {@code source_count} items, {@code source_count} are consumed and
 * {@code product_count} of {@code product} are granted (inventory, dropped on
 * the ground when it is full). The vanilla click is cancelled so the cauldron
 * interaction does not also run.
 *
 * <p>Acting only on a main-hand item that exactly matches this entry keeps the
 * remaining swap-mechanics in the same ability a no-op (per the engine's
 * no-op-does-not-consume rule): one activation performs exactly the one swap
 * whose {@code source} the player is holding, and consumes the shared
 * requirements (catalyst, hunger, cooldown) once.
 *
 * <p><b>YAML key:</b> {@code core:transmute}
 * <br>Params: {@code source} (required material), {@code product} (required
 * material), {@code source_count} (default 1, consumed per activation),
 * {@code product_count} (default 1, granted per activation)
 */
public final class TransmuteMechanic implements SkillMechanic {

    private static volatile BiFunction<Material, Integer, ItemStack> stackFactory =
            (material, amount) -> new ItemStack(material, amount);

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to supply the granted
     * product stack without touching the live item registry, which a plain-JUnit
     * JVM cannot initialize. Production always creates a real {@link ItemStack}.
     *
     * @param factory the product-stack factory
     */
    static void setStackFactory(BiFunction<Material, Integer, ItemStack> factory) {
        stackFactory = factory;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_BLOCK) return false;

        Material source = Material.matchMaterial(String.valueOf(params.getOrDefault("source", "")));
        if (source == null) return false;
        Material product = Material.matchMaterial(String.valueOf(params.getOrDefault("product", "")));
        if (product == null) return false;

        int sourceCount = count(params.get("source_count"), 1);
        int productCount = count(params.get("product_count"), 1);
        if (sourceCount < 1 || productCount < 1) return false;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != source) return false;
        if (held.getAmount() < sourceCount) return false;

        // Consume the source batch and cancel the vanilla cauldron interaction.
        held.setAmount(held.getAmount() - sourceCount);
        player.getInventory().setItemInMainHand(held);
        interact.setCancelled(true);

        ItemStack productStack = stackFactory.apply(product, productCount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(productStack);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        return true;
    }

    private static int count(Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        return fallback;
    }
}
