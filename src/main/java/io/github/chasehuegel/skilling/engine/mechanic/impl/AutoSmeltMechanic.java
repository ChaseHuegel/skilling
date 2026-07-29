package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Auto-smelts mined blocks on break (e.g. iron ore -> iron ingot).
 *
 * <p>YAML key: {@code core:auto_smelt}
 * <br>Params: {@code chance} (0-100, percentage)
 */
public record AutoSmeltMechanic() implements SkillMechanic {
    private static final Map<Material, Material> SMELT_MAP = new HashMap<>();
    static {
        SMELT_MAP.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.NETHER_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.NETHER_QUARTZ_ORE, Material.QUARTZ);
        SMELT_MAP.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SMELT_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELT_MAP.put(Material.SAND, Material.GLASS);
        SMELT_MAP.put(Material.RED_SAND, Material.GLASS);
        SMELT_MAP.put(Material.CLAY, Material.TERRACOTTA);
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent be)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0)).doubleValue();
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble(100) >= chance) return false;
        Material source = be.getBlock().getType();
        Material result = SMELT_MAP.get(source);
        if (result == null) return false;
        be.setDropItems(false);
        int count = 1;
        for (ItemStack drop : be.getBlock().getDrops()) {
            count = Math.max(count, drop.getAmount());
        }
        ItemStack smelted = new ItemStack(result, count);
        be.getBlock().getWorld().dropItemNaturally(be.getBlock().getLocation().add(0.5, 0.5, 0.5), smelted);
        return true;
    }
}
