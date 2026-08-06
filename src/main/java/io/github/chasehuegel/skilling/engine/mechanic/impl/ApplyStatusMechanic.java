package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;

/**
 * Applies a potion effect to the entity the triggering event points at.
 *
 * <p>The target is resolved exactly like the damage mechanics: the victim when
 * the player damages it, the right-clicked entity, or the entity the player is
 * looking at on a right-click. Other players and the caster are never affected
 * (PvP protection).
 *
 * <p><b>YAML key:</b> {@code core:apply_status}
 * <p><b>Required parameters:</b> {@code effect} (namespaced key, e.g. {@code minecraft:poison};
 * legacy numeric potion effect IDs still resolve but log a deprecation warning)
 * <p><b>Optional parameters:</b> {@code duration} (seconds, default 3), {@code amplifier} (default 0)
 */
public final class ApplyStatusMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        LivingEntity target = DamageTargetResolver.resolveTarget(player, event);
        if (target == null) return false;

        PotionEffectType type = PotionEffectResolver.resolve(params.get("effect"));

        int duration = ((Number) params.getOrDefault("duration", 3.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        target.addPotionEffect(new PotionEffect(type, duration, amplifier));
        return true;
    }
}
