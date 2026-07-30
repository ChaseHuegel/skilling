package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Multiplies fishing loot when a player catches a fish or treasure on {@link PlayerFishEvent}.
 * Works by doubling the caught item entity's stack when the player reels in.
 *
 * <p><b>YAML key:</b> {@code core:fishing_yield}
 * <p><b>Required parameters:</b> {@code yield_chance} (0-100, percentage chance to double catch)
 */
public final class FishingYieldMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerFishEvent fishEvent)) return false;
        if (fishEvent.getState() != PlayerFishEvent.State.CAUGHT_FISH) return false;

        double chance = ((Number) params.getOrDefault("yield_chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        if (!(fishEvent.getCaught() instanceof Item caught)) return false;
        if (ThreadLocalRandom.current().nextDouble(100) >= chance) return false;

        ItemStack stack = caught.getItemStack().clone();
        stack.setAmount(stack.getAmount() * 2);
        player.getWorld().dropItemNaturally(caught.getLocation(), stack);
        return true;
    }
}
