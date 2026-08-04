package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * Deals damage based on the off-hand weapon's base attack damage to the entity the
 * player is looking at, consuming 1 off-hand durability.
 *
 * <p>Only fires on a right-click with an item in hand; left-clicks, plain block
 * interactions, and empty-hand actions never trigger it. The durability decrement
 * is written back to the off-hand slot, and an air/unbreakable off-hand is a no-op.
 *
 * <p>The raycast is gated by the {@code targets} filter and never strikes another
 * player or the caster, so a cheap right-click cannot damage players across a
 * room. The {@code reach} and {@code multiplier} params are clamped at execution
 * time so level-scaled evaluator outputs stay bounded too.
 *
 * <p>YAML key: {@code core:offhand_strike}
 * <br>Params:
 * <ul>
 *   <li>{@code multiplier} (double, optional, default 1.0) — scales the off-hand
 *       base damage (clamped to [0, 4])</li>
 *   <li>{@code reach} (double, optional, default 4) — maximum raycast distance in
 *       blocks (clamped to [0, 4.5])</li>
 *   <li>{@code targets} (string, optional, default {@code hostiles}) — which
 *       living entities may be struck; other players are never struck</li>
 * </ul>
 *
 * <p>Requires {@link PlayerInteractEvent}. The {@link #BASE_DAMAGE} table maps vanilla
 * weapon materials to their base attack damage; unarmed off-hand defaults to 1.0.
 */
public final class OffhandStrikeMechanic implements SkillMechanic {

    /** Vanilla survival attack reach, so the raycast cannot hit across a room. */
    static final double MAX_REACH = 4.5;

    /** Sane damage-multiplier cap so a level-scaled multiplier cannot one-shot. */
    static final double MAX_MULTIPLIER = 4.0;

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
        if (!(event instanceof PlayerInteractEvent interactEvent)) return false;

        // Only right-click-with-item actions may trigger the strike; left-clicks
        // and plain block interactions must not fire (or give free damage).
        org.bukkit.event.block.Action action = interactEvent.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return false;
        if (interactEvent.useItemInHand() == org.bukkit.event.Event.Result.DENY) return false;

        double multiplier = clampMultiplier(((Number) params.getOrDefault("multiplier", 1.0)).doubleValue());
        if (multiplier <= 0) return false;
        double reach = clampReach(((Number) params.getOrDefault("reach", 4.0)).doubleValue());
        String targets = String.valueOf(params.getOrDefault("targets", "hostiles"));

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType() == Material.AIR) return false;
        if (!(offhand.getItemMeta() instanceof Damageable damageable)) return false;
        if (damageable.isUnbreakable()) return false;

        Entity target = player.getTargetEntity((int) reach);
        if (!(target instanceof LivingEntity livingTarget)) return false;
        // PvP protection: never strike another player, and honor the targets filter.
        if (livingTarget instanceof Player) return false;
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
