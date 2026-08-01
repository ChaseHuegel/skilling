package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Triggers the vanilla shield raise-lockout cooldown on a target player.
 *
 * <p>YAML key: {@code core:shield_disable}
 * <br>Params: {@code ticks} (double) — cooldown duration in ticks, must be &gt; 0 to act.
 *
 * <p>On {@link EntityDamageByEntityEvent} the cooldown is applied to the damaged player;
 * on {@link PlayerInteractEvent} the activating player disables their own shield.
 */
public final class ShieldDisableMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double ticks = ((Number) params.getOrDefault("ticks", 0.0)).doubleValue();
        if (ticks <= 0) return false;

        if (event instanceof EntityDamageByEntityEvent de && de.getEntity() instanceof Player victim) {
            victim.setCooldown(Material.SHIELD, (int) ticks);
            return true;
        }

        if (event instanceof PlayerInteractEvent) {
            player.setCooldown(Material.SHIELD, (int) ticks);
            return true;
        }

        return false;
    }
}
