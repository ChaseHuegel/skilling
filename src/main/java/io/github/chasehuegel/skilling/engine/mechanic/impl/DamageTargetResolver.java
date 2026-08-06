package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Shared target resolution for the damage mechanics ({@code core:damage} and
 * {@code core:true_damage}).
 *
 * <p>The target is the entity the event points at: the victim when the player
 * damages it, the right-clicked entity, or the entity the player is looking at
 * on a right-click. Other players and the caster are never resolvable targets
 * (PvP protection), matching {@code core:knockback} and {@code core:offhand_strike}.
 *
 * <p>The damage amount combines the flat {@code damage} value with the
 * {@code percent} fraction of the target's max health, so both parameters can be
 * declared together and the result is their sum.
 */
final class DamageTargetResolver {

    private DamageTargetResolver() {}

    /**
     * Resolves the damage target carried by the event, or null when no acceptable
     * living target exists (air click, no target in reach, a player, the caster,
     * or a dead entity).
     *
     * @param player the player activating the ability
     * @param event the triggering event
     * @return the target to damage, or null
     */
    static LivingEntity resolveTarget(Player player, Event event) {
        LivingEntity target = null;
        if (event instanceof EntityDamageByEntityEvent de
                && player.equals(EntityDamageResolver.resolveDamagerPlayer(de))
                && de.getEntity() instanceof LivingEntity victim) {
            target = victim;
        } else if (event instanceof PlayerInteractEntityEvent ie
                && ie.getRightClicked() instanceof LivingEntity clicked) {
            target = clicked;
        } else if (event instanceof PlayerInteractEvent ie) {
            org.bukkit.event.block.Action action = ie.getAction();
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                    || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                Entity hit = player.getTargetEntity((int) OffhandStrikeMechanic.MAX_REACH);
                if (hit instanceof LivingEntity living) target = living;
            }
        }
        if (target == null || target.isDead()) return null;
        // PvP protection: never damage another player or the caster.
        if (target instanceof Player || target.equals(player)) return null;
        return target;
    }

    /**
     * Computes the damage amount from the {@code damage} flat value and the
     * {@code percent} fraction of the target's max health.
     *
     * @param target the target to damage
     * @param params the mechanic parameters
     * @return the total damage amount in engine points (half-hearts)
     */
    static double resolveAmount(LivingEntity target, Map<String, Object> params) {
        double damage = ((Number) params.getOrDefault("damage", 0.0)).doubleValue();
        double percent = ((Number) params.getOrDefault("percent", 0.0)).doubleValue();
        double maxHp = target.getAttribute(Attribute.MAX_HEALTH) != null
                ? target.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        return damage + maxHp * percent;
    }
}
