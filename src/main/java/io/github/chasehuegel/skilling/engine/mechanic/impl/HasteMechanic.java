package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;

/**
 * Applies the HASTE potion effect to the player for a configurable duration.
 * Provides a mining speed increase that affects block-breaking speed rather than
 * movement speed (unlike {@link SpeedBonusMechanic}).
 *
 * <p><b>YAML key:</b> {@code core:haste_effect}
 * <p><b>Required parameters:</b> {@code amplifier} (0 = 33% speed, 1 = 66%, etc.)
 * <p><b>Optional parameters:</b> {@code duration} (seconds, default 300)
 */
public final class HasteMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        int amplifier = ((Number) params.getOrDefault("amplifier", 0.0)).intValue();
        if (amplifier < 0) return false;
        int duration = ((Number) params.getOrDefault("duration", 300.0)).intValue() * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, amplifier));
        return true;
    }
}
