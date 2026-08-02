package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Returns a thrown projectile's item to the player on {@link ProjectileHitEvent},
 * with a percentage chance.
 *
 * <p>For retrievable projectiles that persist in the world (tridents, arrows —
 * including spectral and tipped arrows), the returned item is the projectile's
 * own stack — preserving enchantments, durability, names, lore, and potion
 * effects — and the projectile entity is removed so it cannot also be picked up
 * (no duplication). Non-retrievable projectiles (snowballs, eggs) get a fresh
 * drop since they vanish on impact anyway.
 *
 * <p><b>YAML key:</b> {@code core:projectile_return}
 * <br>Params: {@code chance} (0-100, percentage chance to return the projectile)
 */
public final class ProjectileReturnMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof ProjectileHitEvent hitEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        if (ThreadLocalRandom.current().nextDouble(100) > chance) return false;
        Projectile projectile = hitEvent.getEntity();
        if (!(projectile.getShooter() instanceof Player shooter) || !shooter.equals(player)) return false;

        ItemStack returnItem = returnItemFor(projectile);
        if (returnItem == null) return false;

        // Tridents and arrows persist and are pickable in vanilla; remove the
        // entity so the returned item is received exactly once (no 2-for-1).
        if (isPickableItem(projectile)) {
            projectile.remove();
        }

        player.getWorld().dropItemNaturally(player.getLocation(), returnItem);
        return true;
    }

    private static ItemStack returnItemFor(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow) {
            ItemStack item = arrow.getItemStack();
            if (item != null && !item.isEmpty()) return item.clone();
            Material fallback = switch (projectile.getType()) {
                case TRIDENT -> Material.TRIDENT;
                case SPECTRAL_ARROW -> Material.SPECTRAL_ARROW;
                default -> Material.ARROW;
            };
            return new ItemStack(fallback);
        }
        Material fresh = freshMaterial(projectile);
        return fresh == null ? null : new ItemStack(fresh);
    }

    /**
     * Whether the projectile persists in the world and can be picked up in vanilla
     * (tridents and all arrow types), requiring the entity to be removed to avoid
     * a duplicate pickup.
     */
    static boolean isPickableItem(Projectile projectile) {
        return projectile instanceof AbstractArrow;
    }

    /**
     * Fresh material for non-retrievable projectiles that vanish on impact.
     */
    static Material freshMaterial(Projectile projectile) {
        if (projectile.getType() == EntityType.SNOWBALL) return Material.SNOWBALL;
        if (projectile.getType() == EntityType.EGG) return Material.EGG;
        return null;
    }
}
