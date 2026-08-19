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
 * Adds a flat amplifier to every potion effect in a brewing stand's contents on
 * {@link BrewEvent}.
 *
 * <p>The added amplifier raises each effect's potency (Speed I becomes Speed II,
 * and so on) while leaving its duration untouched. It applies to the base potion
 * type's effects (a vanilla-brewed potion stores its effect as the base type)
 * and to any existing custom effects. Crucially, unlike a duration multiplier,
 * it also strengthens instant potions (Instant Health, Instant Harming), whose
 * duration is effectively fixed.
 *
 * <p>An amplifier of 0 is a no-op return {@code false}, so a level-scaled
 * evaluator (e.g. {@code milestones { 50: 1 }}) that has not yet crossed its
 * threshold spends nothing.
 *
 * <p><b>YAML key:</b> {@code core:modify_potion_amplifier}
 * <br>Params: {@code amplifier} (default 0, flat add to each effect's level)
 */
public final class ModifyPotionAmplifierMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BrewEvent brewEvent)) return false;
        int added = ((Number) params.getOrDefault("amplifier", 0)).intValue();
        if (added == 0) return false;

        boolean modified = false;
        for (ItemStack item : brewEvent.getContents().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            if (item.getItemMeta() instanceof PotionMeta meta) {
                if (boostPotionMeta(meta, added)) {
                    item.setItemMeta(meta);
                    modified = true;
                }
            }
        }
        return modified;
    }

    /**
     * Adds {@code added} to the amplifier of every effect on the potion, using
     * the override flag so re-boosting a batch never stacks duplicate effects.
     *
     * @param meta  the potion meta to boost
     * @param added the amplifier to add (must be positive)
     * @return true if at least one effect was boosted
     */
    private static boolean boostPotionMeta(PotionMeta meta, int added) {
        boolean modified = false;
        if (meta.hasBasePotionType()) {
            for (PotionEffect effect : meta.getBasePotionType().getPotionEffects()) {
                meta.addCustomEffect(boosted(effect, added), true);
                modified = true;
            }
        }
        if (meta.hasCustomEffects()) {
            for (PotionEffect effect : meta.getCustomEffects()) {
                meta.addCustomEffect(boosted(effect, added), true);
                modified = true;
            }
        }
        return modified;
    }

    private static PotionEffect boosted(PotionEffect effect, int added) {
        int amplifier = effect.getAmplifier() + added;
        if (amplifier < 0) amplifier = 0;
        return new PotionEffect(
                effect.getType(), effect.getDuration(), amplifier,
                effect.isAmbient(), effect.hasParticles(), effect.hasIcon()
        );
    }
}
