package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import java.util.Map;

/**
 * Injects additional saturation to the player on {@link PlayerItemConsumeEvent}.
 *
 * <p><b>YAML key:</b> {@code core:saturation_inject}
 * <p><b>Required parameters:</b> {@code saturation} (positive float value to add)
 */
public final class SaturationInjectMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerItemConsumeEvent)) return false;
        double saturation = ((Number) params.getOrDefault("saturation", 0.0)).doubleValue();
        if (saturation <= 0) return false;
        // Clamp to the vanilla saturation cap (20, matching the hunger bar).
        float capped = Math.min(player.getSaturation() + (float) saturation, 20.0f);
        player.setSaturation(capped);
        return true;
    }
}
