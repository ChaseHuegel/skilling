package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class CancelDamageMechanic implements SkillMechanic {

    @Override
    public void execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent damageEvent)) return;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return;
        if (ThreadLocalRandom.current().nextDouble(100) < chance) {
            damageEvent.setCancelled(true);
        }
    }
}
