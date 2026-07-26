package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class YieldMultiplierMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockBreakEvent breakEvent)) return false;
        double chance = ((Number) params.getOrDefault("yield_chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        if (ThreadLocalRandom.current().nextDouble(100) < chance) {
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
