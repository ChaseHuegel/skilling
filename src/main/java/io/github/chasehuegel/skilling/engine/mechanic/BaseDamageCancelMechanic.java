package io.github.chasehuegel.skilling.engine.mechanic;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract base for damage-cancelling mechanics that roll a percentage chance
 * to negate {@link EntityDamageEvent} damage.
 *
 * <p>Subclasses only need to provide {@link #getChance(Map)} to supply the
 * percentage chance from the ability's parameters.
 */
public abstract class BaseDamageCancelMechanic implements SkillMechanic {

    protected abstract double getChance(Map<String, Object> params);

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        double chance = getChance(params);
        if (chance <= 0) return false;
        if (ThreadLocalRandom.current().nextDouble(100) <= chance) {
            de.setCancelled(true);
            return true;
        }
        return false;
    }
}
