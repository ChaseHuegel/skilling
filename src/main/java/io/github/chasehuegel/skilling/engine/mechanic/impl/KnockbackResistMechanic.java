package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Temporarily increases {@code GENERIC_KNOCKBACK_RESISTANCE} for the specified duration.
 *
 * <p>YAML key: {@code core:knockback_resist}
 * <br>Params: {@code amount} (0-1, resistance value), {@code duration} (optional, seconds, default 300),
 * {@code uuid} (optional, stable modifier UUID so repeated activations refresh instead of stacking)
 */
public final class KnockbackResistMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        if (amount <= 0) return false;
        int duration = ((Number) params.getOrDefault("duration", 300.0)).intValue();
        AttributeInstance inst = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (inst == null) return false;
        return AttributeModifierHelper.applyTransient(
                player, Attribute.KNOCKBACK_RESISTANCE, AttributeModifierHelper.resolveUuid(params.get("uuid")),
                "skilling_knockback", amount, duration);
    }
}
