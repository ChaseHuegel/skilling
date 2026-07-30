package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemDamageEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gives a percentage chance to negate item durability damage on {@link PlayerItemDamageEvent}.
 *
 * <p><b>YAML key:</b> {@code core:durability_save}
 * <p><b>Required parameters:</b> {@code chance} (0-100, percentage to negate damage)
 */
public final class DurabilitySaveMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerItemDamageEvent damageEvent)) return false;
        if (!damageEvent.getPlayer().equals(player)) return false;

        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        if (ThreadLocalRandom.current().nextDouble(100) < chance) {
            damageEvent.setCancelled(true);
            return true;
        }
        return false;
    }
}
