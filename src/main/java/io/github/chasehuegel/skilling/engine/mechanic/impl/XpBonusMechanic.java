package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Applies a multiplicative XP bonus for the player's session when activated.
 * The multiplier is stored in a static map and consumed by the XP grant pipeline.
 *
 * <p>YAML key: {@code core:xp_bonus}
 * <br>Params: {@code multiplier} (multiplicative factor, not a percentage increase;
 * 1.0 = no bonus, 1.5 = +50%, 2.0 = double)
 */
public record XpBonusMechanic() implements SkillMechanic {

    private static final Map<UUID, Double> multipliers = new ConcurrentHashMap<>();

    /**
     * Returns the active XP multiplier for the given player, defaulting to 1.0.
     */
    public static double getMultiplier(UUID playerId) {
        return multipliers.getOrDefault(playerId, 1.0);
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double mult = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (mult <= 0) return false;
        multipliers.put(player.getUniqueId(), mult);
        return true;
    }
}
