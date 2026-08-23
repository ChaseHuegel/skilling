package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityBreedEvent;
import java.util.Map;
import java.util.UUID;

/**
 * Grants a level-scaled attribute bonus to a newborn animal on {@link EntityBreedEvent}.
 *
 * <p>The bred offspring ({@code EntityBreedEvent.getEntity()}) inherits an
 * additive attribute modifier (e.g. {@code minecraft:max_health} so a herd
 * gets tougher, or {@code minecraft:movement_speed} so it gets faster). The
 * modifier is <em>transient</em> ({@link AttributeInstance#addTransientModifier}),
 * never written to the animal's NBT, so removing the plugin leaves every bred
 * animal at its vanilla attributes — the clean-unplug pillar. The inherited
 * bonus persists while the animal stays loaded, and a re-bred generation carries
 * the current level's value.
 *
 * <p>Execution is idempotent per UUID: an existing modifier with the same
 * {@code uuid} and amount is a no-op, a different amount replaces it, and a
 * non-positive amount removes it. Because it is evaluated at the current skill
 * level, breeding repeatedly at a higher level produces stronger offspring.
 *
 * <p><b>YAML key:</b> {@code core:bred_attribute}
 * <br>Params: {@code attribute} (namespaced key), {@code amount} (level-scaled
 * additive modifier value), {@code uuid} (required stable modifier UUID so
 * replace-not-stack works across generations)
 */
public final class BredAttributeMechanic implements SkillMechanic {

    /** Namespaced-key value prefix for bred-attribute modifiers. */
    static final String MODIFIER_KEY_PREFIX = "bred";

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityBreedEvent breedEvent)) return false;
        if (!(breedEvent.getEntity() instanceof LivingEntity offspring)) return false;
        Attribute attribute = ModifyAttributeMechanic.resolveAttribute(params.get("attribute"));
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        if (amount <= 0) return false;
        UUID uuid = AttributeModifierHelper.resolveUuid(params.get("uuid"));
        return applyBredModifier(offspring, attribute, uuid, amount);
    }

    /**
     * Applies the additive transient modifier to the offspring's attribute,
     * replacing any prior modifier with the same UUID and leaving an identical
     * one untouched.
     *
     * @param offspring the newborn animal
     * @param attribute the attribute to modify
     * @param uuid      the stable modifier UUID
     * @param amount    the additive modifier value
     * @return true if the modifier state changed, false when the attribute is
     *         absent, the amount is non-positive, or the current modifier matches
     */
    static boolean applyBredModifier(LivingEntity offspring, Attribute attribute, UUID uuid, double amount) {
        AttributeInstance inst = offspring.getAttribute(attribute);
        if (inst == null) return false;
        NamespacedKey key = NamespacedKey.fromString(
                PersistentAttributeMechanic.MODIFIER_NAMESPACE + ":" + MODIFIER_KEY_PREFIX + "_" + uuid);
        AttributeModifier existing = inst.getModifier(key);
        if (existing != null) {
            if (amount == existing.getAmount()) return false;
            inst.removeModifier(key);
        }
        inst.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
        if (attribute == Attribute.MAX_HEALTH) {
            clampHealth(offspring);
        }
        return true;
    }

    /**
     * Clamps the offspring's current health to the new max-health value so
     * raising the cap never leaves health above it and a later re-breed at the
     * same UUID cannot underflow.
     *
     * @param offspring the newborn animal
     */
    private static void clampHealth(LivingEntity offspring) {
        AttributeInstance maxHealth = offspring.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && offspring.getHealth() > maxHealth.getValue()) {
            offspring.setHealth(maxHealth.getValue());
        }
    }
}