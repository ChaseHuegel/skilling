package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Instantly kills the target if below a health threshold.
 *
 * <p>The kill is delivered as a guaranteed-lethal blow through the damage
 * pipeline so it stays attributed to the player (killer credit, vanilla XP),
 * and the mechanic reports success only if the target actually died — a target
 * that survives (armor/absorption, cancelled damage, invulnerability) does not
 * consume the ability's cost/cooldown.
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
            // Already-dead targets and unkillable players (creative/spectator)
            // are no-ops: there is nothing to execute.
            if (target.isDead()) return false;
            if (target instanceof Player p
                    && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)) {
                return false;
            }
            double maxHp = target.getAttribute(Attribute.MAX_HEALTH) != null
                ? target.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
            if (target.getHealth() / maxHp * 100 <= threshold) {
                // A huge damage amount survives any armor/absorption reduction, so
                // the threshold execution cannot fail to the target's defenses.
                target.damage(Double.MAX_VALUE, player);
                // Only consume the ability when the kill actually landed.
                return target.isDead();
            }
        }
        return false;
    }
}
