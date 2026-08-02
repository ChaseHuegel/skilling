package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Instantly kills the target if below a health threshold.
 *
 * <p>YAML key: {@code core:execute}
 * <br>Params: {@code threshold} (0-100, % HP)
 */
public record ExecuteMechanic() implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!player.equals(EntityDamageResolver.resolveDamagerPlayer(de))) return false;
        double threshold = ((Number) params.getOrDefault("threshold", 0)).doubleValue();
        if (threshold <= 0) return false;
        if (de.getEntity() instanceof LivingEntity target) {
            double maxHp = target.getAttribute(Attribute.MAX_HEALTH) != null
                ? target.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
            if (target.getHealth() / maxHp * 100 <= threshold) {
                target.setHealth(0);
                return true;
            }
        }
        return false;
    }
}
