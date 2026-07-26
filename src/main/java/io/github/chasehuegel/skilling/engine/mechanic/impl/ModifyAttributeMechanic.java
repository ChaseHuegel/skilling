package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;
import java.util.UUID;

public final class ModifyAttributeMechanic implements SkillMechanic {

    @Override
    public void execute(Player player, Map<String, Object> params, Event event) {
        String attrName = (String) params.getOrDefault("attribute", "");
        if (attrName.isBlank()) return;
        Attribute attribute;
        try {
            attribute = Attribute.valueOf(attrName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        int duration = ((Number) params.getOrDefault("duration", 5.0)).intValue();
        if (amount == 0) return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        var modifier = new AttributeModifier(
                UUID.randomUUID(),
                "skilling_modifier",
                amount,
                AttributeModifier.Operation.ADD_NUMBER
        );
        instance.addTransientModifier(modifier);
        player.getScheduler().runDelayed(
                org.bukkit.plugin.java.JavaPlugin.getPlugin(
                        io.github.chasehuegel.skilling.Skilling.class
                ),
                task -> instance.removeModifier(modifier),
                null,
                duration * 20L
        );
    }
}
