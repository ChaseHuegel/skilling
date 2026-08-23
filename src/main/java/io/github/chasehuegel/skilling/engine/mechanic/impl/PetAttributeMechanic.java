package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;
import java.util.UUID;

/**
 * Buffs the player's tamed wolf(ves) near them: a scaling additive bonus to
 * {@code minecraft:attack_damage} (so the companion deals more damage) and a
 * {@code RESISTANCE} potion effect scaled from a reduction fraction (so it takes
 * less damage). Bound to a defensive trigger (e.g. {@code entity_damage_taken}),
 * so the buff refreshes while the player fights with their wolf.
 *
 * <p>Only tamed {@link Wolf} entities owned by the casting player within a radius
 * receive the buffs, matching the "combat companion" fantasy; horses are left
 * untouched. The attack-damage modifier is transient per UUID (replace-not-stack
 * via {@link AttributeModifierHelper}), and the resistance effect refreshes on
 * each activation. {@code false} is returned only when no owned wolf is in range,
 * so a call with no companion nearby is a no-op.
 *
 * <p><b>YAML key:</b> {@code core:pet_attribute}
 * <p><b>Optional parameters:</b> {@code damage} (additive attack-damage bonus,
 * default 0), {@code reduction} (damage-reduction fraction 0-0.5, mapped to a
 * resistance amplifier, default 0), {@code radius} (default 16, clamped to [0, 32]),
 * {@code duration} (seconds, default 10), {@code uuid} (stable modifier UUID for
 * the attack-damage bonus)
 */
public final class PetAttributeMechanic implements SkillMechanic {

    /** Matches Bukkit's entity-search radius cap. */
    private static final double MAX_RADIUS = 32.0;

    /** Namespaced prefix for the wolf attack-damage modifier key. */
    static final String MODIFIER_KEY_PREFIX = "pet";

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double damage = ((Number) params.getOrDefault("damage", 0.0)).doubleValue();
        double reduction = ((Number) params.getOrDefault("reduction", 0.0)).doubleValue();
        double radius = Math.max(0, Math.min(((Number) params.getOrDefault("radius", 16.0)).doubleValue(), MAX_RADIUS));
        int duration = ((Number) params.getOrDefault("duration", 10.0)).intValue();
        UUID uuid = AttributeModifierHelper.resolveUuid(params.get("uuid"));

        boolean any = false;
        for (LivingEntity entity : player.getLocation().getNearbyLivingEntities(radius)) {
            if (!(entity instanceof Wolf wolf)) continue;
            if (!wolf.isTamed() || !(wolf.getOwner() instanceof Player owner)
                    || !owner.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (damage > 0) {
                applyAttackDamage(wolf, damage, uuid);
            }
            if (reduction > 0 && duration > 0) {
                int amplifier = Math.min(3, (int) Math.floor(reduction * 5));
                wolf.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration * 20, amplifier));
            }
            any = true;
        }
        return any;
    }

    /**
     * Applies the additive attack-damage transient modifier to the wolf under the
     * stable UUID, replacing any prior one carrying the same UUID.
     *
     * @param wolf   the tamed wolf
     * @param damage the additive attack-damage amount
     * @param uuid   the stable modifier UUID
     */
    private static void applyAttackDamage(Wolf wolf, double damage, UUID uuid) {
        AttributeInstance inst = wolf.getAttribute(Attribute.ATTACK_DAMAGE);
        if (inst == null) return;
        NamespacedKey key = NamespacedKey.fromString(
                PersistentAttributeMechanic.MODIFIER_NAMESPACE + ":" + MODIFIER_KEY_PREFIX + "_" + uuid);
        AttributeModifier existing = inst.getModifier(key);
        if (existing != null) {
            inst.removeModifier(key);
        }
        inst.addTransientModifier(new AttributeModifier(key, damage, AttributeModifier.Operation.ADD_NUMBER));
    }
}