package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Temporarily increases {@code GENERIC_ARMOR} and {@code GENERIC_ARMOR_TOUGHNESS} for the specified duration.
 *
 * <p>YAML key: {@code core:armor_bonus}
 * <br>Params: {@code amount} (armor points to add), {@code duration} (optional, seconds, default 300),
 * {@code uuid} (optional, stable modifier UUID so repeated activations refresh instead of stacking)
 */
public final class ArmorBonusMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        if (amount == 0) return false;
        int duration = ((Number) params.getOrDefault("duration", 300.0)).intValue();
        var uuid = AttributeModifierHelper.resolveUuid(params.get("uuid"));
        boolean applied = false;
        for (Attribute attr : new Attribute[]{Attribute.ARMOR, Attribute.ARMOR_TOUGHNESS}) {
            if (AttributeModifierHelper.applyTransient(
                    player, attr, uuid, "skilling_armor_bonus", amount, duration)) {
                applied = true;
            }
        }
        return applied;
    }
}
