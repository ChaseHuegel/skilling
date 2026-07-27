package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cancels incoming damage with a percentage chance on {@link EntityDamageEvent}.
 *
 * <p><b>YAML key:</b> {@code cancel_damage}
 * <p><b>Required parameters:</b> {@code chance} (0-100, percentage chance to negate damage)
 */
public final class CancelDamageMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent damageEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        if (ThreadLocalRandom.current().nextDouble(100) < chance) {
            damageEvent.setCancelled(true);
        }
        return true;
    }
}
