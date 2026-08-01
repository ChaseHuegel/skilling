package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Triggers the vanilla item-stack cooldown animation on the activating player.
 *
 * <p>YAML key: {@code core:set_cooldown}
 * <br>Params:
 * <ul>
 *   <li>{@code material} (String) — namespaced material key, e.g. {@code minecraft:shield}</li>
 *   <li>{@code ticks} (double) — cooldown duration in ticks, must be &gt; 0 to act</li>
 * </ul>
 *
 * <p>Throws {@link IllegalArgumentException} if the material key does not resolve
 * (fail-fast on bad config).
 */
public final class SetCooldownMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        Object rawMat = params.get("material");
        if (rawMat == null) return false;

        Material mat = Material.matchMaterial(rawMat.toString());
        if (mat == null) {
            throw new IllegalArgumentException("Unknown material in set_cooldown: " + rawMat);
        }

        double ticks = ((Number) params.getOrDefault("ticks", 0.0)).doubleValue();
        if (ticks <= 0) return false;

        player.setCooldown(mat, (int) ticks);
        return true;
    }
}
