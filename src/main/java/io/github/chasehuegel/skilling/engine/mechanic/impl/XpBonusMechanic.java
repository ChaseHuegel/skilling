package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Placeholder: XP bonus multiplier would hook into XP gain pipeline.
 *
 * <p>YAML key: {@code core:xp_bonus}
 * <br>Params: {@code multiplier}
 */
public record XpBonusMechanic() implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double mult = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        return mult > 0;
    }
}
