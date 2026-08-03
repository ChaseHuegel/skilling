package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;

/**
 * Launches a snowball projectile from the player with configurable speed and damage on {@link PlayerInteractEvent}.
 * Damage is applied via a {@code ProjectileHitEvent} handler using PersistentDataContainer.
 *
 * <p>Only a right-click (air or block) launches the projectile; a left-click is a
 * no-op so it never consumes the ability cost.
 *
 * <p><b>YAML key:</b> {@code projectile}
 * <p><b>Optional parameters:</b> {@code speed} (default 1.5), {@code damage} (default 4.0)
 */
public final class ProjectileMechanic implements SkillMechanic {

    /** Key used to store intended damage on snowball entities. */
    public static final NamespacedKey DAMAGE_KEY = NamespacedKey.fromString("skilling:projectile_damage");

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interactEvent)) return false;
        org.bukkit.event.block.Action action = interactEvent.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return false;
        double speed = ((Number) params.getOrDefault("speed", 1.5)).doubleValue();
        double damage = ((Number) params.getOrDefault("damage", 4.0)).doubleValue();

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().multiply(speed));
        projectile.getPersistentDataContainer().set(DAMAGE_KEY, PersistentDataType.DOUBLE, damage);
        return true;
    }
}
