package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
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

    /** Ceiling on the cooldown in ticks (5 minutes), so a runaway level-scaled value cannot freeze an item for hours. */
    private static final int MAX_TICKS = 20 * 60 * 5;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        Object rawMat = params.get("material");
        if (rawMat == null) return false;

        // Route through the cached TagResolver so the material name is parsed
        // once per unique name instead of calling matchMaterial per activation.
        // Without a live plugin instance (e.g. unit tests) fall back to a direct
        // parse, which is not cached but still correct.
        io.github.chasehuegel.skilling.Skilling instance = io.github.chasehuegel.skilling.Skilling.getInstance();
        TagResolver resolver = instance != null ? instance.getTagResolver() : null;
        Material mat = resolver != null
                ? resolver.material(rawMat.toString())
                : Material.matchMaterial(rawMat.toString());
        if (mat == null) {
            throw new IllegalArgumentException("Unknown material in set_cooldown: " + rawMat);
        }

        double ticks = ((Number) params.getOrDefault("ticks", 0.0)).doubleValue();
        // Round (not truncate) a level-scaled fractional value, then clamp to a
        // sane range so the animation never exceeds the ceiling.
        int tickCount = (int) Math.round(ticks);
        if (tickCount <= 0) return false;
        tickCount = Math.min(tickCount, MAX_TICKS);

        player.setCooldown(mat, tickCount);
        return true;
    }
}
