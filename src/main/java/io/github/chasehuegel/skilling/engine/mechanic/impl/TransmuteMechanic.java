package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Converts a held stack of one material into another on a right-click of a
 * reaction block (a cauldron for the alchemy transmutation topic, a furnace for
 * a kiln-firing topic).
 *
 * <p>Each {@code core:transmute} mechanic entry carries a single source-&gt;product
 * swap; the ability's "transmute table" is expressed as several of these
 * mechanics so it stays within the scalar evaluator schema. The mechanic reads
 * the player's main-hand item: when it matches {@code source} with at least
 * {@code source_count} items, {@code source_count} are consumed and
 * {@code product_count} of {@code product} are granted (inventory, dropped on
 * the ground when it is full). The vanilla click is cancelled so the reaction
 * block's interaction does not also run.
 *
 * <p>The optional {@code block} parameter names the reaction block the swap must
 * run on (a material or tag). When present it is verified against the clicked
 * block before anything is consumed; a mismatched block is a no-op that spends
 * nothing. The ability's own {@code target} filter already scopes the station in
 * most skills; {@code block} is a self-contained guard so a single mechanic
 * entry never fires against the wrong reaction vessel regardless of the filter.
 *
 * <p>Acting only on a main-hand item that exactly matches this entry keeps the
 * remaining swap-mechanics in the same ability a no-op (per the engine's
 * no-op-does-not-consume rule): one activation performs exactly the one swap
 * whose {@code source} the player is holding, and consumes the shared
 * requirements (catalyst, hunger, cooldown) once.
 *
 * <p><b>YAML key:</b> {@code core:transmute}
 * <br>Params: {@code source} (required material), {@code product} (required
 * material), {@code block} (optional material or tag restricting the reaction
 * block), {@code source_count} (default 1, consumed per activation),
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

        if (!matchesStation(interact.getClickedBlock(), params.get("block"))) return false;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != source) return false;
        if (held.getAmount() < sourceCount) return false;

        // Consume the source batch and cancel the vanilla reaction-block interaction.
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

    /**
     * Whether the clicked block is the reaction station the {@code block}
     * parameter requires. A blank/absent parameter imposes no station check; a
     * material reference matches the clicked material directly and a {@code #...}
     * reference resolves through the live {@link TagResolver}. A null or air
     * block is rejected.
     *
     * @param clicked the clicked block, or null
     * @param raw     the {@code block} parameter value (material or tag), or null
     * @return true when the block is acceptable
     */
    private static boolean matchesStation(Block clicked, Object raw) {
        if (raw == null) return true;
        String reference = String.valueOf(raw);
        if (reference.isBlank()) return true;
        if (clicked == null || isAir(clicked.getType())) return false;

        if (reference.startsWith("#")) {
            TagResolver resolver = Skilling.getInstance().getTagResolver();
            if (resolver == null) {
                throw new IllegalStateException("Cannot resolve transmute 'block' tag without a live TagResolver");
            }
            Set<Material> set = resolver.resolve(reference);
            return set.contains(clicked.getType());
        }
        Material station = Material.matchMaterial(reference);
        return station != null && clicked.getType() == station;
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }
}
