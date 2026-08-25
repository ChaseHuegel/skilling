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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
 * <p>A second, potion-aware mode is enabled when {@code source_potion} and
 * {@code product_potion} are both present. In this mode the swap matches the
 * held potion's base {@link PotionType} instead of its material (Thick and
 * Healing potions share the {@code minecraft:potion} material and differ only
 * in potion type), so the clicked reaction block converts the held potion's
 * effect without consuming a source batch. The held potion keeps its item form
 * (drinkable, splash, or lingering) and any custom effects are cleared, leaving
 * a plain potion of the product type. This is the skill-crafted route of the
 * piety "Holy Water" ability: turning a hard-to-farm Thick potion (glowstone
 * dust) into a basic Healing potion.
 *
 * <p><b>YAML key:</b> {@code core:transmute}
 * <br>Params: {@code source} (required material), {@code product} (required
 * material), {@code block} (optional material or tag restricting the reaction
 * block), {@code source_count} (default 1, consumed per activation),
 * {@code product_count} (default 1, granted per activation),
 * {@code bonus_product_chance} (default 0-100, chance of one extra unit of
 * {@code product} on a successful swap), {@code source_potion} (optional, must
 * pair with {@code product_potion}), {@code product_potion} (optional
 * {@code PotionType} the swap converts to)
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

        if (!matchesStation(interact.getClickedBlock(), params.get("block"))) return false;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null) return false;

        // Potion-aware mode: both potion params present.
        if (params.containsKey("source_potion") || params.containsKey("product_potion")) {
            return swapPotion(held, player, interact, resolvePotionType(params.get("source_potion")), resolvePotionType(params.get("product_potion")));
        }

        Material source = Material.matchMaterial(String.valueOf(params.getOrDefault("source", "")));
        if (source == null) return false;
        Material product = Material.matchMaterial(String.valueOf(params.getOrDefault("product", "")));
        if (product == null) return false;

        int sourceCount = count(params.get("source_count"), 1);
        int productCount = count(params.get("product_count"), 1);
        if (sourceCount < 1 || productCount < 1) return false;

        if (held.getType() != source) return false;

        // Consume the source batch and cancel the vanilla reaction-block interaction.
        held.setAmount(held.getAmount() - sourceCount);
        player.getInventory().setItemInMainHand(held);
        interact.setCancelled(true);

        // A bonus-product roll may grant one extra unit of the product, so a
        // high-level transmuter (Prismatic Rituals) occasionally out-yields the
        // configured ratio without ever costing more source material.
        double bonusChance = ((Number) params.getOrDefault("bonus_product_chance", 0)).doubleValue();
        if (bonusChance > 0 && ThreadLocalRandom.current().nextDouble(100) <= bonusChance) {
            productCount += 1;
        }

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
     * Resolves a {@link PotionType} from its namespaced key (e.g.
     * {@code minecraft:thick}, {@code minecraft:healing}), matching a raw enum
     * name as a fallback. Enumerates the enum rather than consulting the live
     * {@code Registry} so a plain-JUnit JVM (which cannot initialize Bukkit
     * registries) can still parse a skill file.
     *
     * @param raw the {@code source_potion}/{@code product_potion} parameter value
     * @return the resolved potion type, or null when absent or unknown
     */
    static PotionType resolvePotionType(Object raw) {
        if (raw == null) return null;
        String reference = String.valueOf(raw);
        if (reference.isBlank()) return null;
        for (PotionType type : PotionType.values()) {
            if (type.getKey() == null) continue;
            if (type.getKey().toString().equals(reference)
                    || type.getKey().getKey().equals(reference)
                    || type.name().equals(reference)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Whether the material is a potion item (drinkable, splash, or lingering)
     * carrying a {@link PotionMeta}.
     *
     * @param material the held material
     * @return true for any potion item form
     */
    private static boolean isPotionMaterial(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    /**
     * Swaps the held potion's base type from {@code sourceType} to
     * {@code productType} in place, clearing custom effects so the result is a
     * plain potion of the product type. The whole held stack is converted (never
     * a single bottle), the vanilla reaction-block interaction is cancelled, and
     * no source batch is consumed.
     *
     * @param held        the held item
     * @param player      the activating player
     * @param interact    the triggering interaction event
     * @param sourceType  the required base potion type to match
     * @param productType the base potion type to convert to
     * @return true when a potion was actually converted
     */
    private static boolean swapPotion(ItemStack held, Player player, PlayerInteractEvent interact,
                                      PotionType sourceType, PotionType productType) {
        if (sourceType == null || productType == null) return false;
        if (!isPotionMaterial(held.getType())) return false;
        if (held.getItemMeta() == null || !(held.getItemMeta() instanceof PotionMeta meta)) return false;
        if (!meta.hasBasePotionType() || meta.getBasePotionType() != sourceType) return false;

        meta.setBasePotionType(productType);
        meta.clearCustomEffects();
        held.setItemMeta(meta);
        player.getInventory().setItemInMainHand(held);
        interact.setCancelled(true);
        return true;
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
