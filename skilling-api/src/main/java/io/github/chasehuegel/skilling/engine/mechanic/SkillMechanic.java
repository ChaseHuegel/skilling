package io.github.chasehuegel.skilling.engine.mechanic;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;

/**
 * A single executable action that runs when an ability is activated.
 *
 * <p>Mechanics are prototype-scoped: a new instance is created via the
 * MechanicRegistry each time the ability fires. The {@link #execute}
 * method receives the player, pre-evaluated parameters, and the original
 * triggering event.
 *
 * <p>Parameters are a map of String to Object. Numeric values from YAML
 * evaluators arrive as {@link Double}; string values arrive as {@link String}.
 *
 * <p>YAML key: {@code type} field in a mechanic entry (e.g., {@code core:yield_multiplier})
 */
@FunctionalInterface
public interface SkillMechanic {

    /**
     * Executes the mechanic action.
     *
     * <p><b>Return-value contract (consumption rule):</b> returning {@code true}
     * signals an <em>activation attempt</em> — the mechanic reached its
     * action/condition step — and the engine consumes the ability's cost and
     * applies its cooldown exactly once per attempt. Chance-based mechanics
     * therefore return {@code true} whether or not their RNG roll succeeds, so a
     * failed roll cannot be retried for free. Return {@code false} only when the
     * mechanic could not act at all (wrong event type, missing target,
     * inapplicable state); such a no-op must not spend the ability's cost or
     * cooldown. When an ability carries multiple mechanics, consumption happens
     * once if <em>any</em> mechanic returns {@code true}.
     *
     * @param player the player activating the ability
     * @param params pre-evaluated parameters
     * @param event the original triggering event
     * @return true if the mechanic performed an activation attempt; false if it was a no-op
     */
    boolean execute(Player player, Map<String, Object> params, Event event);
}
