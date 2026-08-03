package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Duplicates block drops with a percentage chance on {@link BlockBreakEvent}. When triggered, the
 * block's natural drops are doubled and dropped once at the block location, replacing the vanilla
 * drops (the vanilla drop pipeline is suppressed so a break never yields original + doubled = 3x).
 *
 * <p><b>YAML key:</b> {@code core:yield_multiplier}
 * <p><b>Required parameters:</b> {@code yield_chance} (0-100, percentage chance to double drops)
 */
public final class YieldMultiplierMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;
        double chance = ((Number) params.getOrDefault("yield_chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        if (ThreadLocalRandom.current().nextDouble(100) < chance) {
            // Suppress the vanilla drops so the doubled set below is the only loot.
            breakEvent.setDropItems(false);
            Collection<ItemStack> drops = breakEvent.getBlock().getDrops(player.getInventory().getItemInMainHand());
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    drop.setAmount(drop.getAmount() * 2);
                    breakEvent.getBlock().getWorld().dropItemNaturally(
                            breakEvent.getBlock().getLocation().add(0.5, 0.5, 0.5),
                            drop
                    );
                }
            }
        }

        return true;
    }
}
