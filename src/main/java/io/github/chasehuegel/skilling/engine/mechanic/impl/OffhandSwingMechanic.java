package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;

/**
 * Plays the off-hand swing animation for the activating player.
 *
 * <p>The mechanic does nothing other than call {@link Player#swingOffHand()}; it
 * deals no damage and changes no state. Use it purely for animation feedback,
 * typically alongside another mechanic such as {@code core:offhand_strike}.
 *
 * <p>YAML key: {@code core:offhand_swing}
 * <br>Parameters: none
 */
public final class OffhandSwingMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        player.swingOffHand();
        return true;
    }
}
