package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * Deals damage based on the off-hand weapon's base attack damage to the strike
 * target, consuming 1 off-hand durability.
 *
 * <p>The target is the entity the triggering event points at: the right-clicked
 * entity, the victim of the player's {@code entity_damage}/{@code left_click_entity}
 * hit, or the entity the player is looking at on a {@code player_interact}
 * right-click. A plain right-click on air or a block (no entity in reach) is a
 * no-op. Entity clicks arrive as {@code PlayerInteractEntityEvent}, so a
 * {@code right_click_entity}-triggered ability strikes the clicked entity
 * directly instead of requiring a raycast.
 *
 * <p>Only right-click-with-item actions fire the strike; left-clicks, plain block
 * interactions, and empty-hand actions never trigger it. The durability decrement
 * is written back to the off-hand slot, and an air/unbreakable off-hand is a no-op.
 *
 * <p>Strikes never hit another player or the caster (PvP protection). The
 * {@code targets} filter is honored for the remaining living entities; an unknown
 * {@code targets} value falls back to this mechanic's {@code hostiles} default
 * rather than {@link AuraTargetFilter}'s {@code allies} fallback, so a typo
 * cannot flip the strike onto friendly mobs. The {@code reach} and {@code multiplier}
 * params are clamped at execution time so level-scaled evaluator outputs stay
 * bounded too.
 *
 * <p>YAML key: {@code core:offhand_strike}
 * <br>Params:
 * <ul>
 *   <li>{@code multiplier} (double, optional, default 1.0) — scales the off-hand
 *       base damage (clamped to [0, 4])</li>
 *   <li>{@code reach} (double, optional, default 4) — maximum raycast distance in
 *       blocks for the air/block-click fallback (clamped to [0, 4.5])</li>
 *   <li>{@code targets} (string, optional, default {@code hostiles}) — which
 *       living entities may be struck; other players are never struck</li>
 * </ul>
 *
 * <p>Acts on {@link PlayerInteractEvent}, {@link PlayerInteractEntityEvent}, and
 * {@link EntityDamageByEntityEvent}. The {@link #BASE_DAMAGE} table maps vanilla
 * weapon materials to their base attack damage; unarmed off-hand defaults to 1.0.
 */
public final class OffhandStrikeMechanic implements SkillMechanic {

    /** Vanilla survival attack reach, so the raycast cannot hit across a room. */
    static final double MAX_REACH = 4.5;

    /** Sane damage-multiplier cap so a level-scaled multiplier cannot one-shot. */
    static final double MAX_MULTIPLIER = 4.0;

    /**
     * Vanilla weapon base attack damage per material.
     *
     * <p>Immutable (design decision, ISSUE-301): this mirrors Minecraft's fixed
     * weapon damage values, so it is a library-mechanic fact rather than
     * author-facing content and is deliberately not exposed in YAML.
     */
    static final Map<Material, Double> BASE_DAMAGE = Map.ofEntries(
            Map.entry(Material.WOODEN_SWORD, 4.0),
            Map.entry(Material.STONE_SWORD, 5.0),
            Map.entry(Material.GOLDEN_SWORD, 4.0),
            Map.entry(Material.IRON_SWORD, 6.0),
            Map.entry(Material.DIAMOND_SWORD, 7.0),
            Map.entry(Material.NETHERITE_SWORD, 8.0),
            Map.entry(Material.WOODEN_AXE, 3.0),
            Map.entry(Material.STONE_AXE, 4.0),
            Map.entry(Material.GOLDEN_AXE, 3.0),
            Map.entry(Material.IRON_AXE, 5.0),
            Map.entry(Material.DIAMOND_AXE, 6.0),
            Map.entry(Material.NETHERITE_AXE, 7.0),
            Map.entry(Material.TRIDENT, 8.0),
            Map.entry(Material.MACE, 6.0)
    );

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        // Only right-click-with-item interactions may trigger the strike; left-clicks
        // and plain block interactions must not fire (or give free damage). Entity
        // clicks arrive as PlayerInteractEntityEvent and carry no such guards.
        if (event instanceof PlayerInteractEvent interactEvent) {
            org.bukkit.event.block.Action action = interactEvent.getAction();
            if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                    && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return false;
            if (interactEvent.useItemInHand() == org.bukkit.event.Event.Result.DENY) return false;
        }

        double multiplier = clampMultiplier(((Number) params.getOrDefault("multiplier", 1.0)).doubleValue());
        if (multiplier <= 0) return false;
        double reach = clampReach(((Number) params.getOrDefault("reach", 4.0)).doubleValue());
        String targets = normalizeTargets(String.valueOf(params.getOrDefault("targets", "hostiles")));

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType() == Material.AIR) return false;
        if (!(offhand.getItemMeta() instanceof Damageable damageable)) return false;
        if (damageable.isUnbreakable()) return false;

        // PvP protection, the dead check, and the targets filter live in the
        // shared resolver; never strike another player or the caster.
        LivingEntity livingTarget = DamageTargetResolver.resolveTarget(player, event, reach);
        if (livingTarget == null) return false;
        if (!AuraTargetFilter.accepts(targets, livingTarget)) return false;

        double dmg = baseDamage(offhand.getType()) * multiplier;
        livingTarget.damage(dmg, player);

        // Consume off-hand durability through the cancellable
        // PlayerItemDamageEvent so Unbreaking rolls and other plugins can veto,
        // and the item breaks at max durability instead of resting in an
        // invalid damage state.
        ToolDurability.damageOnce(player, offhand, org.bukkit.inventory.EquipmentSlot.OFF_HAND);
        return true;
    }

    static double clampReach(double reach) {
        return Math.max(0.0, Math.min(reach, MAX_REACH));
    }

    static double clampMultiplier(double multiplier) {
        return Math.max(0.0, Math.min(multiplier, MAX_MULTIPLIER));
    }

    /**
     * Coerces the {@code targets} parameter to a known filter value, falling back
     * to {@code hostiles} on anything unknown so a typo cannot silently select
     * friendly mobs via {@link AuraTargetFilter}'s {@code allies} default.
     *
     * @param targets the configured {@code targets} value
     * @return the normalized filter value
     */
    static String normalizeTargets(String targets) {
        return switch (targets) {
            case "hostiles", "allies", "all" -> targets;
            default -> "hostiles";
        };
    }

    /**
     * Returns the vanilla base attack damage for the given material, defaulting to 1.0
     * (unarmed) when the material has no weapon entry.
     *
     * @param material the off-hand item material
     * @return the base attack damage
     */
    public static double baseDamage(Material material) {
        return BASE_DAMAGE.getOrDefault(material, 1.0);
    }
}
