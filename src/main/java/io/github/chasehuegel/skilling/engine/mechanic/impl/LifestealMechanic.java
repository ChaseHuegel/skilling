package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Heals the player for a percentage of damage dealt.
 *
 * <p>YAML key: {@code core:lifesteal}
 * <br>Params: {@code percentage} (0-100, percentage of damage to heal)
 */
public record LifestealMechanic() implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!de.getDamager().equals(player)) return false;
        double percentage = ((Number) params.getOrDefault("percentage", 0)).doubleValue();
        if (percentage <= 0) return false;
        double healAmount = de.getFinalDamage() * (percentage / 100.0);
        if (healAmount <= 0) return false;
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        double newHealth = Math.min(player.getHealth() + healAmount, maxHealth);
        player.setHealth(newHealth);
        return true;
    }
}
