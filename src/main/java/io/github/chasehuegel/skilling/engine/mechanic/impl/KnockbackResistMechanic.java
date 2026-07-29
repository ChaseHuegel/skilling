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
 * Increases {@code GENERIC_KNOCKBACK_RESISTANCE}.
 *
 * <p>YAML key: {@code core:knockback_resist}
 * <br>Params: {@code amount} (0-1, resistance value)
 */
public final class KnockbackResistMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        if (amount <= 0) return false;
        AttributeInstance inst = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (inst == null) return false;
        var modifier = new AttributeModifier(UUID.randomUUID(), "skilling_knockback", amount, AttributeModifier.Operation.ADD_NUMBER);
        inst.addTransientModifier(modifier);
        player.getScheduler().runDelayed(
            org.bukkit.plugin.java.JavaPlugin.getPlugin(io.github.chasehuegel.skilling.Skilling.class),
            t -> inst.removeModifier(modifier), null, 6000L
        );
        return true;
    }
}
