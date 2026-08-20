package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Refunds a placed block back into the builder's inventory on a percentage roll.
 *
 * <p>The block is granted as one item of the placed material on {@link
 * BlockPlaceEvent}. Because the dispatch runs before the server consumes the
 * held stack (the consumption happens after the event passes), a successful roll
 * nets the player zero material cost for that placement — a materials-conservation
 * scalar that rewards steady building without ever creating free blocks. A full
 * inventory drops the refund item on the ground so nothing is lost.
 *
 * <p>Which blocks qualify is data-driven: the ability's own filter (e.g. a
 * {@code #c:construction_blocks} tag) gates execution, so this mechanic only ever
 * refunds the configured material set.
 *
 * <p><b>YAML key:</b> {@code core:block_refund}
 * <br>Params: {@code chance} (0-100, the percentage chance to refund each placement)
 */
public final class BlockRefundMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);
    private static volatile Function<Material, ItemStack> itemFactory = BlockRefundMechanic::defaultItem;

    /**
     * Test-only seam to force a deterministic roll; production always uses
     * {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a percentage in [0, 100)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    /** Restores the production random source. */
    static void reset() {
        randomSource = () -> ThreadLocalRandom.current().nextDouble(100);
    }

    /**
     * Test-only seam so the refund item can be produced without a live item
     * registry (which is unavailable in a plain-JUnit JVM).
     *
     * @param factory builds the refund ItemStack for a given placed material
     */
    static void setItemFactory(Function<Material, ItemStack> factory) {
        itemFactory = factory;
    }

    /** Restores the production item factory. */
    static void resetItemFactory() {
        itemFactory = BlockRefundMechanic::defaultItem;
    }

    private static ItemStack defaultItem(Material material) {
        return new ItemStack(material, 1);
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockPlaceEvent placeEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        if (randomSource.getAsDouble() > chance) return true;
        refund(player, placeEvent);
        return true;
    }

    /**
     * Grants one item of the placed material back to the player, dropping it on
     * the ground when the inventory is full.
     *
     * @param player the placing player
     * @param event  the placement event whose block is refunded
     */
    private static void refund(Player player, BlockPlaceEvent event) {
        Material placed = event.getBlockPlaced().getType();
        ItemStack refund = itemFactory.apply(placed);
        for (ItemStack leftover : player.getInventory().addItem(refund).values()) {
            player.getWorld().dropItemNaturally(event.getBlockPlaced().getLocation(), leftover);
        }
    }
}
