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
 * <p>YAML key: {@code core:offhand_strike}
 * <br>Params:
 * <ul>
 *   <li>{@code multiplier} (double, optional, default 1.0) — scales the off-hand base damage</li>
 *   <li>{@code reach} (double, optional, default 4) — maximum raycast distance in blocks</li>
 * </ul>
 *
 * <p>Requires {@link PlayerInteractEvent}. The {@link #BASE_DAMAGE} table maps vanilla
 * weapon materials to their base attack damage; unarmed off-hand defaults to 1.0.
 */
public final class OffhandStrikeMechanic implements SkillMechanic {

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
        if (!(event instanceof PlayerInteractEvent)) return false;

        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;
        double reach = ((Number) params.getOrDefault("reach", 4.0)).doubleValue();

        Entity target = player.getTargetEntity((int) reach);
        if (!(target instanceof LivingEntity livingTarget)) return false;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        double dmg = baseDamage(offhand.getType()) * multiplier;
        livingTarget.damage(dmg, player);

        if (offhand.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage(damageable.getDamage() + 1);
            offhand.setItemMeta((org.bukkit.inventory.meta.ItemMeta) damageable);
        }
        return true;
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
