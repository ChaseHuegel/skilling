package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;
import java.util.UUID;

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
        var modifier = new AttributeModifier(UUID.randomUUID(), "skilling_jump", added, AttributeModifier.Operation.ADD_NUMBER);
        inst.addTransientModifier(modifier);
        player.getScheduler().runDelayed(
            io.github.chasehuegel.skilling.Skilling.getInstance(),
            t -> inst.removeModifier(modifier), null, duration * 20L
        );
        return true;
    }
}
