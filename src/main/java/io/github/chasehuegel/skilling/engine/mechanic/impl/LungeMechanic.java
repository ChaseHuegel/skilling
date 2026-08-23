package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

/**
 * Propels the player forward in their look direction on a right-click (air or
 * block), the mobility "lunge" that closes distance into a fight. A left-click
 * is a no-op so it never consumes the ability cost.
 *
 * <p>Only the player's own velocity is changed and the impulse is clamped, so
 * the dash is transient player state: no world geometry is written and removing
 * the plugin returns the player to vanilla physics (safe unplug). The optional
 * {@code knockback_resist} seconds apply full knockback resistance, keeping the
 * player glued to their path while mid-lunge so a shove cannot cancel the dash.
 *
 * <p>YAML key: {@code core:lunge}
 * <br>Params:
 * <ul>
 *   <li>{@code force} (double, &gt; 0 to act) — horizontal impulse strength in the
 *       look direction (clamped to [0, 3])</li>
 *   <li>{@code vertical} (double, optional, default 0.3) — upward component added
 *       to the impulse (clamped to [0, 1.5])</li>
 *   <li>{@code knockback_resist} (double, optional, default 0) — seconds of full
 *       knockback resistance granted as the dash is applied (0 = none)</li>
 * </ul>
 */
public final class LungeMechanic implements SkillMechanic {

    /** Sane impulse cap so a level-scaled force cannot rocket the player across a map. */
    static final double MAX_FORCE = 3.0;

    /** Sane upward cap so a level-scaled vertical cannot launch the player skyward. */
    static final double MAX_VERTICAL = 1.5;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interactEvent)) return false;
        Action action = interactEvent.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;

        double force = clampForce(((Number) params.getOrDefault("force", 0.0)).doubleValue());
        if (force <= 0) return false;
        double vertical = clampVertical(((Number) params.getOrDefault("vertical", 0.3)).doubleValue());

        Vector impulse = player.getLocation().getDirection().multiply(force);
        impulse.setY(impulse.getY() + vertical);
        player.setVelocity(impulse);

        double resist = ((Number) params.getOrDefault("knockback_resist", 0.0)).doubleValue();
        if (resist > 0) {
            AttributeModifierHelper.applyTransient(
                    player, Attribute.KNOCKBACK_RESISTANCE,
                    AttributeModifierHelper.resolveUuid(params.get("uuid")),
                    "skilling_lunge_resist", 1.0, (int) resist);
        }
        return true;
    }

    static double clampForce(double force) {
        return Math.max(0.0, Math.min(force, MAX_FORCE));
    }

    static double clampVertical(double vertical) {
        return Math.max(0.0, Math.min(vertical, MAX_VERTICAL));
    }
}