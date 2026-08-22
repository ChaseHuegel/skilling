package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import java.util.Map;

/**
 * Shortens the time between a cast and the fish bite on {@link PlayerFishEvent}.
 *
 * <p>Fires on the cast (the {@code FISHING} state, routed by the
 * {@code fishing_cast} trigger) and shrinks the hook's remaining wait time and
 * its re-roll bounds by a percentage. Only the single in-flight hook's timing is
 * mutated: the mechanic never re-casts and never reels, so a cast cannot yield a
 * second fish (it only waits less). {@code false} is returned when the mechanic
 * could not act (wrong event type, no reduction, or a hook with no wait).
 *
 * <p><b>YAML key:</b> {@code core:fishing_speed}
 * <p><b>Required parameters:</b> {@code reduction} (0-100, percentage to shorten the catch wait)
 */
public final class FishingSpeedMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerFishEvent fishEvent)) return false;

        double reduction = ((Number) params.getOrDefault("reduction", 0.0)).doubleValue();
        if (reduction <= 0) return false;

        FishHook hook = fishEvent.getHook();
        if (hook == null) return false;

        double factor = 1.0 - reduction / 100.0;
        int remaining = Math.max(1, (int) Math.round(hook.getWaitTime() * factor));
        hook.setWaitTime(remaining);
        hook.setMinWaitTime(Math.max(1, (int) Math.round(hook.getMinWaitTime() * factor)));
        hook.setMaxWaitTime(Math.max(1, (int) Math.round(hook.getMaxWaitTime() * factor)));
        return true;
    }
}