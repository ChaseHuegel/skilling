package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Permanently increases {@code GENERIC_ARMOR} and {@code GENERIC_ARMOR_TOUGHNESS}.
 *
 * <p>YAML key: {@code core:armor_bonus}
 * <br>Params: {@code amount} (armor points to add)
 */
public final class ArmorBonusMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        if (amount == 0) return false;
        boolean applied = false;
        for (Attribute attr : new Attribute[]{Attribute.ARMOR, Attribute.ARMOR_TOUGHNESS}) {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst != null) {
                var modifier = new AttributeModifier(UUID.randomUUID(), "skilling_armor_bonus", amount, AttributeModifier.Operation.ADD_NUMBER);
                inst.addTransientModifier(modifier);
                player.getScheduler().runDelayed(
                    org.bukkit.plugin.java.JavaPlugin.getPlugin(io.github.chasehuegel.skilling.Skilling.class),
                    t -> inst.removeModifier(modifier), null, 6000L
                );
                applied = true;
            }
        }
        return applied;
    }
}
