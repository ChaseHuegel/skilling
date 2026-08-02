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
 * Doubles fishing loot with a percentage chance when a player catches a fish or
 * treasure on {@link PlayerFishEvent}. The net multiplier is applied to the catch:
 * only the <em>extra</em> amount (original × 1) is dropped as a bonus, so the
 * player collects exactly 2× the original — never the original plus a full clone.
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

        ItemStack stack = caught.getItemStack();
        // Drop only the difference (original × (multiplier − 1) with multiplier 2),
        // matching FishingLootMechanic, so total collected is exactly 2×, not 3×.
        ItemStack bonus = stack.clone();
        bonus.setAmount(stack.getAmount());
        player.getWorld().dropItemNaturally(caught.getLocation(), bonus);
        return true;
    }
}
