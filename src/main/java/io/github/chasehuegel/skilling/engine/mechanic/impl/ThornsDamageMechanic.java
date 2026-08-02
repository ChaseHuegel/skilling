package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Reflects damage back to the attacker.
 *
 * <p>YAML key: {@code core:thorns_damage}
 * <br>Params: {@code damage} (flat amount)
 */
public record ThornsDamageMechanic() implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        double damage = ((Number) params.getOrDefault("damage", 0)).doubleValue();
        if (damage <= 0) return false;
        LivingEntity attacker = EntityDamageResolver.resolveDamagerEntity(de);
        if (attacker == null) return false;
        attacker.damage(damage, player);
        return true;
    }
}
