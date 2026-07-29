package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Chance to completely negate incoming damage (evasion).
 *
 * <p>YAML key: {@code core:dodge}
 * <br>Params: {@code chance} (0-100, percentage)
 */
public record DodgeMechanic() implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0)).doubleValue();
        if (chance <= 0) return false;
        if (ThreadLocalRandom.current().nextDouble(100) <= chance) {
            de.setCancelled(true);
            return true;
        }
        return false;
    }
}
