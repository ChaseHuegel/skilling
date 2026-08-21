package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Refunds one crafting ingredient back to the player on a percentage roll, the
 * "no-waste craftsman" perk: the careful worker never spoils material.
 *
 * <p>The refunded ingredient is identified <em>programmatically</em> from the
 * placed grid ({@link CraftingInventory#getMatrix}), so the mechanic needs no
 * per-recipe configuration and works for any craft, shaped or shapeless, vanilla
 * or data-pack. A randomly selected non-empty used-ingredient slot is restored by
 * one item, granted to the player's inventory (dropped on the ground when full).
 * The crafted result is never touched and the matrix is not mutated, so the roll
 * cannot interfere with the card's own consumption.
 *
 * <p>The optional {@code ingredient} reference (a material or {@code #...} tag)
 * narrows which used ingredients may be refunded; when absent, any used
 * ingredient of the craft is eligible. The trigger's own filter (for example a
 * {@code #c:wooden_products} craft filter) is what scopes the application.
 *
 * <p><b>YAML key:</b> {@code core:ingredient_refund}
 * <br>Params: {@code chance} (0-100), optional {@code ingredient} (material or tag)
 */
public final class IngredientRefundMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);
    private static volatile Function<Material, ItemStack> itemFactory = ItemStack::new;

    /**
     * Test-only seam to force a deterministic roll; production always uses
     * {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a percentage in [0, 100)
     */
    public static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    /** Restores the production random source. */
    public static void reset() {
        randomSource = () -> ThreadLocalRandom.current().nextDouble(100);
    }

    /**
     * Test-only seam so the refund item can be produced without a live item
     * registry (which is unavailable in a plain-JUnit JVM).
     *
     * @param factory builds the refund ItemStack for a given ingredient material
     */
    public static void setItemFactory(Function<Material, ItemStack> factory) {
        itemFactory = factory;
    }

    /** Restores the production item factory. */
    public static void resetItemFactory() {
        itemFactory = ItemStack::new;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof CraftItemEvent craftEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        List<Material> candidates = eligibleIngredients(craftEvent, params);
        if (candidates.isEmpty()) return false;
        if (randomSource.getAsDouble() > chance) return true;

        int pick = ThreadLocalRandom.current().nextInt(candidates.size());
        Material refunded = candidates.get(pick);
        for (ItemStack leftover : player.getInventory().addItem(itemFactory.apply(refunded)).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        return true;
    }

    /**
     * Collects the eligible refund candidates: the distinct non-empty ingredient
     * materials in the placed grid, optionally narrowed by the {@code ingredient}
     * reference.
     *
     * @param craftEvent the crafting event whose grid is read
     * @param params     the evaluated mechanic parameters
     * @return the distinct eligible ingredient materials (empty if none)
     */
    private static List<Material> eligibleIngredients(CraftItemEvent craftEvent, Map<String, Object> params) {
        if (!(craftEvent.getInventory() instanceof CraftingInventory crafting)) return List.of();
        ItemStack[] matrix = crafting.getMatrix();
        if (matrix == null) return List.of();

        Set<Material> allowed = resolveAllowed(params);
        List<Material> candidates = new ArrayList<>();
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;
            Material type = item.getType();
            if (allowed != null && !allowed.contains(type)) continue;
            if (!candidates.contains(type)) {
                candidates.add(type);
            }
        }
        return candidates;
    }

    /**
     * Resolves the optional {@code ingredient} reference into a material set, or
     * {@code null} when absent so every used ingredient is eligible.
     *
     * @param params the evaluated mechanic parameters
     * @return the allowed refund materials, or null for "any"
     */
    private static Set<Material> resolveAllowed(Map<String, Object> params) {
        Object raw = params.get("ingredient");
        if (raw == null) return null;
        String reference = String.valueOf(raw);
        if (reference.isBlank()) return null;
        if (reference.startsWith("#")) {
            TagResolver resolver = Skilling.getInstance().getTagResolver();
            if (resolver == null) {
                throw new IllegalStateException("Cannot resolve ingredient_refund 'ingredient' without a live TagResolver");
            }
            return resolver.resolve(reference);
        }
        Material material = Material.matchMaterial(reference);
        return material == null ? null : Set.of(material);
    }
}
