package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import java.util.Map;

public final class SaturationInjectMechanic implements SkillMechanic {

    @Override
    public void execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerItemConsumeEvent)) return;
        double saturation = ((Number) params.getOrDefault("saturation", 0.0)).doubleValue();
        if (saturation <= 0) return;
        player.setSaturation(player.getSaturation() + (float) saturation);
    }
}
