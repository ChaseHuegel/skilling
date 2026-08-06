package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;

/**
 * Deals damage to the target that ignores damage mitigations.
 *
 * <p>The damage is applied directly to the target's health via
 * {@link LivingEntity#setHealth(double)}, bypassing armor, protection
 * enchantments, potion effects, absorption, and damage-cancel mechanics. The
 * target must not be invulnerable (creative players, protected entities) — that
 * hard flag is still honored.
 *
 * <p>The target is the entity the triggering event points at: the victim when
 * the player damages it, the right-clicked entity, or the entity the player is
 * looking at on a right-click. Other players and the caster are never damaged.
 *
 * <p>YAML key: {@code core:true_damage}
 * <br>Params:
 * <ul>
 *   <li>{@code damage} (double, optional, default 0) — flat damage in engine
 *       points (half-hearts, matching the rest of the engine; an iron sword
 *       deals 6.0)</li>
 *   <li>{@code percent} (double, optional, default 0) — bonus damage as a
 *       fraction of the target's max health (e.g. {@code 0.1} = 10% of max
 *       health)</li>
 * </ul>
 *
 * <p>Acts only when the total damage is positive; a no-op otherwise so a zeroed
 * parameter set never consumes the ability cost.
 */
public record TrueDamageMechanic() implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        LivingEntity target = DamageTargetResolver.resolveTarget(player, event);
        if (target == null) return false;
        double amount = DamageTargetResolver.resolveAmount(target, params);
        if (amount <= 0) return false;
        if (target.isInvulnerable()) return false;
        target.setHealth(Math.max(0.0, target.getHealth() - amount));
        return true;
    }
}
