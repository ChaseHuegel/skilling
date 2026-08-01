package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;

/**
 * Temporarily increases {@code GENERIC_JUMP_STRENGTH} for the specified duration.
 *
 * <p>YAML key: {@code core:modify_jump}
 * <br>Params: {@code multiplier} (e.g. 1.5 = 50% higher jumps), {@code duration} (optional, seconds, default 300),
 * {@code uuid} (optional, stable modifier UUID so repeated activations refresh instead of stacking)
 */
public final class ModifyJumpMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;
        int duration = ((Number) params.getOrDefault("duration", 300.0)).intValue();
        AttributeInstance inst = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (inst == null) return false;
        double base = inst.getBaseValue();
        double added = base * (multiplier - 1.0);
        if (added <= 0) return false;
        return AttributeModifierHelper.applyTransient(
                player, Attribute.JUMP_STRENGTH, AttributeModifierHelper.resolveUuid(params.get("uuid")),
                "skilling_jump", added, duration);
    }
}
