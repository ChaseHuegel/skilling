package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Enhances fishing loot by applying a multiplier to caught item stacks
 * on {@link PlayerFishEvent}. Always activates on CAUGHT_FISH state.
 *
 * <p><b>YAML key:</b> {@code core:fishing_loot}
 * <p><b>Required parameters:</b> {@code multiplier} (stack multiplier, e.g. 2.0 = double)
 */
public final class FishingLootMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerFishEvent fishEvent)) return false;
        if (fishEvent.getState() != PlayerFishEvent.State.CAUGHT_FISH) return false;

        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 1.0) return false;

        if (!(fishEvent.getCaught() instanceof Item caught)) return false;

        ItemStack stack = caught.getItemStack();
        int newAmount = (int) Math.round(stack.getAmount() * multiplier);
        if (newAmount > stack.getAmount()) {
            int extra = newAmount - stack.getAmount();
            ItemStack bonus = stack.clone();
            bonus.setAmount(extra);
            player.getWorld().dropItemNaturally(caught.getLocation(), bonus);
        }
        return true;
    }
}
