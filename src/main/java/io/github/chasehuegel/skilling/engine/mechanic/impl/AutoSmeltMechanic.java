package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Auto-smelts mined blocks on break (e.g. iron ore -> iron ingot).
 *
 * <p>The smelted result replaces the vanilla drops: the real, tool-aware drops
 * (Fortune/Silk-Touch aware via {@code Block.getDrops(ItemStack)}) are captured
 * first, their stack sizes are summed, and the matching smelted product is
 * dropped once. Silk-Touch mining is left untouched (raw ore preserved) and
 * nugget-producing ores stay nuggets.
 *
 * <p>Reaching the chance roll counts as an activation attempt: the mechanic
 * returns {@code true} whether or not the roll succeeds, so the ability's cost
 * and cooldown are consumed exactly once per attempt and a failed roll cannot
 * be retried for free. {@code false} is only returned when the mechanic could
 * not act at all (wrong event type, unsmeltable block, silk-touch tool, or no
 * captured drops).
 *
 * <p>YAML key: {@code core:auto_smelt}
 * <br>Params: {@code chance} (0-100, percentage)
 */
public record AutoSmeltMechanic() implements SkillMechanic {
    private static final Map<Material, Material> SMELT_MAP = Map.ofEntries(
        Map.entry(Material.IRON_ORE, Material.IRON_INGOT),
        Map.entry(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT),
        Map.entry(Material.GOLD_ORE, Material.GOLD_INGOT),
        Map.entry(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT),
        Map.entry(Material.COPPER_ORE, Material.COPPER_INGOT),
        Map.entry(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT),
        Map.entry(Material.NETHER_GOLD_ORE, Material.GOLD_NUGGET),
        Map.entry(Material.NETHER_QUARTZ_ORE, Material.QUARTZ),
        Map.entry(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP),
        Map.entry(Material.COBBLESTONE, Material.STONE),
        Map.entry(Material.SAND, Material.GLASS),
        Map.entry(Material.RED_SAND, Material.GLASS),
        Map.entry(Material.CLAY, Material.TERRACOTTA)
    );

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    /**
     * Test-only seam to force a deterministic roll; production always uses
     * {@link ThreadLocalRandom}.
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
        Material source = be.getBlock().getType();
        Material result = SMELT_MAP.get(source);
        if (result == null) return false;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        // Silk-Touch mining yields the raw ore block; smelting it would destroy
        // the block, so leave those drops untouched.
        if (mainHand.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) return false;

        // Capture the real, tool-and-enchantment-aware drops (Fortune included)
        // before suppressing the vanilla drop pipeline. Sum counts across every
        // stack so no drop is lost.
        Collection<ItemStack> drops = be.getBlock().getDrops(mainHand);
        ItemStack smelted = null;
        int count = 0;
        for (ItemStack drop : drops) {
            if (drop == null) continue;
            count += drop.getAmount();
            if (smelted == null) smelted = drop;
        }
        if (count <= 0 || smelted == null) return false;

        // A smeltable, non-silk-touch block was broken: the ability attempted to
        // act, so a failed roll still counts as an activation (consume once).
        if (randomSource.getAsDouble() >= chance) return true;

        be.setDropItems(false);
        smelted.setType(result);
        smelted.setAmount(count);
        be.getBlock().getWorld().dropItemNaturally(be.getBlock().getLocation().add(0.5, 0.5, 0.5), smelted);
        return true;
    }
}
