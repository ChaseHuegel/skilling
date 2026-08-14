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
 * Multiplies the duration of every potion effect in a brewing stand's contents on {@link BrewEvent}.
 *
 * <p>The scale applies to the base potion type's effects (a vanilla-brewed
 * potion such as Swiftness stores its effect as the base type, not as a custom
 * effect) and to any existing custom effects. Same-type effects merge with the
 * longest duration winning at consumption, so keeping the base type preserves
 * the potion's identity while the scaled custom effect wins.
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
                if (scalePotionMeta(meta, multiplier)) {
                    item.setItemMeta(meta);
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Scales the duration of every effect on the potion, including the base
     * potion type's effects. The override flag replaces any pre-existing custom
     * effect of the same type, so re-scaling a batch never stacks duplicates.
     *
     * @param meta       the potion meta to scale
     * @param multiplier the duration multiplier
     * @return true if at least one effect was scaled
     */
    private static boolean scalePotionMeta(PotionMeta meta, double multiplier) {
        boolean modified = false;
        if (meta.hasBasePotionType()) {
            for (PotionEffect effect : meta.getBasePotionType().getPotionEffects()) {
                meta.addCustomEffect(scaled(effect, multiplier), true);
                modified = true;
            }
        }
        if (meta.hasCustomEffects()) {
            for (PotionEffect effect : meta.getCustomEffects()) {
                meta.addCustomEffect(scaled(effect, multiplier), true);
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Computes the scaled copy of a potion effect, flooring the duration at one
     * tick so a very short or instant effect never becomes zero-duration.
     *
     * @param effect     the effect to scale
     * @param multiplier the duration multiplier
     * @return the scaled effect copy
     */
    private static PotionEffect scaled(PotionEffect effect, double multiplier) {
        int newDuration = (int) Math.round(effect.getDuration() * multiplier);
        if (newDuration < 1) newDuration = 1;
        return new PotionEffect(
                effect.getType(), newDuration, effect.getAmplifier(),
                effect.isAmbient(), effect.hasParticles(), effect.hasIcon()
        );
    }
}
