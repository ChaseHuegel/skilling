package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.Map;

/**
 * Automatically replants crops when harvested on {@link BlockBreakEvent}.
 * Only activates on mature crops (age == maximum age). Replants by setting
 * the crop block back to age 0 after harvest drops are handled.
 *
 * <p><b>YAML key:</b> {@code core:auto_replant}
 * <p><b>Parameters:</b> none
 */
public final class AutoReplantMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;

        Block block = breakEvent.getBlock();
        Material type = block.getType();
        if (!isCrop(type)) return false;

        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) return false;
        if (ageable.getAge() < ageable.getMaximumAge()) return false;

        // Schedule replant on next tick so drops happen first
        org.bukkit.Bukkit.getScheduler().runTask(
            io.github.chasehuegel.skilling.Skilling.getInstance(),
            () -> {
                block.setType(type);
                BlockData newData = block.getBlockData();
                if (newData instanceof Ageable newAgeable) {
                    newAgeable.setAge(0);
                    block.setBlockData(newAgeable, false);
                }
            }
        );
        return true;
    }

    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART -> true;
            default -> false;
        };
    }
}
