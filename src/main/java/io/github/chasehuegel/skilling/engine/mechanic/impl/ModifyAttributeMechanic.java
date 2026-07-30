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
 * <p><b>Required parameters:</b> {@code attribute} (legacy numeric attribute ID, e.g. {@code 4} for Movement Speed)
 * <p><b>Optional parameters:</b> {@code amount} (modifier value), {@code duration} (default 5s)
 *
 * <p>Numeric IDs: 1=MAX_HEALTH, 2=FOLLOW_RANGE, 3=KNOCKBACK_RESISTANCE, 4=MOVEMENT_SPEED,
 * 5=FLYING_SPEED, 6=ARMOR, 7=ARMOR_TOUGHNESS, 8=ATTACK_DAMAGE, 9=ATTACK_SPEED, 10=LUCK
 */
public final class ModifyAttributeMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        Object rawAttr = params.get("attribute");
        if (rawAttr == null) return false;
        int id;
        if (rawAttr instanceof Number n) {
            id = n.intValue();
        } else {
            try {
                id = Integer.parseInt(rawAttr.toString());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        Attribute attribute = switch (id) {
            case 1 -> Attribute.MAX_HEALTH;
            case 2 -> Attribute.FOLLOW_RANGE;
            case 3 -> Attribute.KNOCKBACK_RESISTANCE;
            case 4 -> Attribute.MOVEMENT_SPEED;
            case 5 -> Attribute.FLYING_SPEED;
            case 6 -> Attribute.ARMOR;
            case 7 -> Attribute.ARMOR_TOUGHNESS;
            case 8 -> Attribute.ATTACK_DAMAGE;
            case 9 -> Attribute.ATTACK_SPEED;
            case 10 -> Attribute.LUCK;
            default -> null;
        };
        if (attribute == null) return false;
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
