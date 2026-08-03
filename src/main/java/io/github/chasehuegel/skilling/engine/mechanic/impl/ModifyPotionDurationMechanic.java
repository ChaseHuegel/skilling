package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import java.util.Map;

/**
 * Multiplies the duration of all custom potion effects in a brewing stand's contents on {@link BrewEvent}.
 *
 * <p><b>YAML key:</b> {@code core:modify_potion_duration}
 * <p><b>Optional parameters:</b> {@code multiplier} (default 1.0; values &gt; 1 lengthen, &lt; 1 shorten, minimum 1 tick)
 */
public final class ModifyPotionDurationMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BrewEvent brewEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;

        boolean modified = false;
        for (ItemStack item : brewEvent.getContents().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            if (item.getItemMeta() instanceof PotionMeta meta) {
                if (meta.hasCustomEffects()) {
                    for (PotionEffect effect : meta.getCustomEffects()) {
                        int newDuration = (int) Math.round(effect.getDuration() * multiplier);
                        if (newDuration < 1) newDuration = 1;
                        meta.addCustomEffect(new PotionEffect(
                                effect.getType(), newDuration, effect.getAmplifier(),
                                effect.isAmbient(), effect.hasParticles(), effect.hasIcon()
                        ), true);
                    }
                    item.setItemMeta(meta);
                    modified = true;
                }
            }
        }
        return modified;
    }
}
