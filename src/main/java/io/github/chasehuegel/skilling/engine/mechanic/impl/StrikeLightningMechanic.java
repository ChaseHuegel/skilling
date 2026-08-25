package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Strikes the aimed entity with a vanilla lightning visual and a configured
 * amount of damage on a right-click cast.
 *
 * <p>The mage summons a thunderbolt that reads as vanilla lightning without
 * corrupting terrain: {@code strikeLightningEffect} plays the real lightning,
 * flash, and crack (no block fires, Pillar I safe unplug) while the configured
 * {@code damage} is dealt directly to the aimed target. Other players and the
 * caster are never strikable targets (PvP protection), matching the shared
 * {@link DamageTargetResolver}. Aiming at no acceptable target is a no-op that
 * spends nothing.
 *
 * <p><b>YAML key:</b> {@code core:strike_lightning}
 * <br>Params: {@code damage} (default 6.0, dealt on the aimed target)
 */
public final class StrikeLightningMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interactEvent)) return false;
        Action action = interactEvent.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;

        LivingEntity target = DamageTargetResolver.resolveTarget(player, event);
        if (target == null || target.isDead()) return false;

        double damage = ((Number) params.getOrDefault("damage", 6.0)).doubleValue();
        if (damage <= 0) return false;

        // Visual-only lightning (no fire / terrain damage) + controlled damage.
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.damage(damage, player);
        return true;
    }
}