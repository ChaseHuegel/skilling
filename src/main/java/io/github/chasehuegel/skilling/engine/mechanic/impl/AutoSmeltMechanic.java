package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Auto-smelts mined blocks on break (e.g. iron ore -> iron ingot).
 *
 * <p>The smelted results replace the vanilla drops: the real, tool-aware drops
 * (Fortune/Silk-Touch aware via {@code Block.getDrops(ItemStack)}) are captured
 * first, then each distinct <em>drop type</em> with a mapping in
 * {@link #SMELT_MAP} is smelted independently — counts are summed per product and
 * one smelted stack is dropped per product, while drop types without a mapping
 * are re-dropped unchanged, so a multi-type block never merges or loses drops.
 * Silk-Touch mining is left untouched (raw ore preserved) and nugget/quartz
 * drops (already the smelted product) pass through unchanged.
 *
 * <p>Reaching the chance roll counts as an activation attempt: the mechanic
 * returns {@code true} whether or not the roll succeeds, so the ability's cost
 * and cooldown are consumed exactly once per attempt and a failed roll cannot
 * be retried for free. {@code false} is only returned when the mechanic could
 * not act at all (wrong event type, no tool, silk-touch tool, no captured drops,
 * or nothing smeltable among the drops).
 *
 * <p>YAML key: {@code core:auto_smelt}
 * <br>Params: {@code chance} (0-100, percentage)
 */
public record AutoSmeltMechanic() implements SkillMechanic {

    /** Maps a captured <em>drop</em> material to its smelted product. */
    private static final Map<Material, Material> SMELT_MAP = Map.ofEntries(
        Map.entry(Material.RAW_IRON, Material.IRON_INGOT),
        Map.entry(Material.RAW_GOLD, Material.GOLD_INGOT),
        Map.entry(Material.RAW_COPPER, Material.COPPER_INGOT),
        Map.entry(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP),
        Map.entry(Material.COBBLESTONE, Material.STONE),
        Map.entry(Material.SAND, Material.GLASS),
        Map.entry(Material.RED_SAND, Material.GLASS),
        Map.entry(Material.CLAY, Material.TERRACOTTA)
    );

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to force a deterministic
     * roll; production always uses {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a percentage in [0, 100)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent be)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0)).doubleValue();
        if (chance <= 0) return false;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType() == Material.AIR) return false;
        // Silk-Touch mining yields the raw ore block; smelting it would destroy
        // the block, so leave those drops untouched.
        if (mainHand.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) return false;

        // Capture the real, tool-and-enchantment-aware drops (Fortune included)
        // before suppressing the vanilla drop pipeline.
        Collection<ItemStack> drops = be.getBlock().getDrops(mainHand);
        if (drops == null || drops.isEmpty()) return false;

        // Smelt each distinct drop type independently: sum counts per smelted
        // product and remember a sample stack to retype, keeping unmapped drop
        // types to re-drop unchanged so nothing is lost or merged across types.
        Map<Material, Integer> smeltedCounts = new HashMap<>();
        Map<Material, ItemStack> smeltedSamples = new HashMap<>();
        List<ItemStack> passthrough = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType() == Material.AIR) continue;
            Material product = SMELT_MAP.get(drop.getType());
            if (product == null) {
                passthrough.add(drop);
            } else {
                smeltedCounts.merge(product, drop.getAmount(), Integer::sum);
                smeltedSamples.putIfAbsent(product, drop);
            }
        }
        if (smeltedCounts.isEmpty()) return false;

        // A block with smeltable drops was broken: the ability attempted to act,
        // so a failed roll still counts as an activation (consume once).
        if (randomSource.getAsDouble() >= chance) return true;

        be.setDropItems(false);
        Location dropLocation = be.getBlock().getLocation().add(0.5, 0.5, 0.5);
        smeltedCounts.forEach((product, count) -> {
            ItemStack smelted = smeltedSamples.get(product);
            smelted.setType(product);
            smelted.setAmount(count);
            be.getBlock().getWorld().dropItemNaturally(dropLocation, smelted);
        });
        for (ItemStack drop : passthrough) {
            be.getBlock().getWorld().dropItemNaturally(dropLocation, drop);
        }
        return true;
    }
}
