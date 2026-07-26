package io.github.chasehuegel.skilling.engine.mechanic;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;

/**
 * A single executable action that runs when an ability is activated.
 *
 * <p>Mechanics are prototype-scoped: a new instance is created via the
 * {@link io.github.chasehuegel.skilling.engine.registry.MechanicRegistry}
 * each time the ability fires. The {@link #execute} method receives the
 * player, pre-evaluated parameters, and the original triggering event.
 *
 * <p>Parameters are a map of String to Object. Numeric values from YAML
 * evaluators arrive as {@link Double}; string values arrive as {@link String}.
 *
 * <p>YAML key: {@code type} field in a mechanic entry (e.g., {@code core:yield_multiplier})
 */
@FunctionalInterface
public interface SkillMechanic {

    void execute(Player player, Map<String, Object> params, Event event);
}
