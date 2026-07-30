package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTameEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ModifyTameChanceMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityTameEvent tameEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;
        if (ThreadLocalRandom.current().nextDouble() > 1.0 / multiplier) {
            tameEvent.setCancelled(true);
        }
        return true;
    }
}
