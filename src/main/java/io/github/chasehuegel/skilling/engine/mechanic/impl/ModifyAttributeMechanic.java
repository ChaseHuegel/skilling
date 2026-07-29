package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;
import java.util.UUID;

/**
 * Temporarily modifies a player's attribute (e.g. {@code GENERIC_MAX_HEALTH}, {@code GENERIC_MOVEMENT_SPEED})
 * for a specified duration using a transient {@link AttributeModifier}.
 *
 * <p><b>YAML key:</b> {@code modify_attribute}
 * <p><b>Required parameters:</b> {@code attribute} (attribute enum name), {@code amount} (modifier value)
 * <p><b>Optional parameters:</b> {@code duration} (default 5s)
 */
public final class ModifyAttributeMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        String attrName = (String) params.getOrDefault("attribute", "");
        if (attrName.isBlank()) return false;
        Attribute attribute;
        try {
            attribute = Attribute.valueOf(attrName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        int duration = ((Number) params.getOrDefault("duration", 5.0)).intValue();
        if (amount == 0) return false;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return false;

        var modifier = new AttributeModifier(
                UUID.randomUUID(),
                "skilling_modifier",
                amount,
                AttributeModifier.Operation.ADD_NUMBER
        );
        instance.addTransientModifier(modifier);
        player.getScheduler().runDelayed(
                io.github.chasehuegel.skilling.Skilling.getInstance(),
                task -> instance.removeModifier(modifier),
                null,
                duration * 20L
        );
        return true;
    }
}
