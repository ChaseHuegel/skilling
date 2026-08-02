package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Shared resolution of the effective actor behind an {@link EntityDamageByEntityEvent}.
 *
 * <p>Abilities fire for the player resolved by the event dispatcher, which already
 * resolves a projectile's shooter. These helpers let mechanics apply the same rule
 * so bow/snowball attacks count as the owning player's, and a mob projectile's
 * shooter is the reflected attacker.
 */
public final class EntityDamageResolver {

    private EntityDamageResolver() {}

    /**
     * Returns the player responsible for the damage: a direct player attacker, or
     * a projectile's shooter when it is a player.
     *
     * @param event the damage event
     * @return the owning player, or null if the damage did not originate from a player
     */
    public static Player resolveDamagerPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    /**
     * Returns the living entity responsible for the damage: a direct living
     * attacker, or a projectile's shooter when it is living.
     *
     * @param event the damage event
     * @return the attacking living entity, or null
     */
    public static LivingEntity resolveDamagerEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity living) return living;
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof LivingEntity shooter) return shooter;
        return null;
    }
}
