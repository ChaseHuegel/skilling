package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;

/**
 * Applies a potion effect to the activating player only. This is the self-only
 * counterpart of {@code core:apply_status} (which targets a victim) and the
 * auras (which target nearby allies): it powers a solo-buffing cleric's
 * milestones, such as a totem-resurrection burst or a feast-time absorption,
 * without ever touching allies or hostile mobs.
 *
 * <p><b>YAML key:</b> {@code core:self_effect}
 * <br>Params: {@code effect} (namespaced key, e.g. {@code minecraft:absorption}),
 * {@code duration} (seconds, default 3), {@code amplifier} (default 0)
 */
public final class SelfEffectMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        PotionEffectType type = PotionEffectResolver.resolve(params.get("effect"));
        int duration = ((Number) params.getOrDefault("duration", 3.0)).intValue() * 20;
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        player.addPotionEffect(new PotionEffect(type, duration, amplifier));
        return true;
    }
}