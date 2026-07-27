package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;

/**
 * Launches a snowball projectile from the player with configurable speed and damage on {@link PlayerInteractEvent}.
 * Damage is applied via a {@code ProjectileHitEvent} handler using metadata.
 *
 * <p><b>YAML key:</b> {@code projectile}
 * <p><b>Optional parameters:</b> {@code speed} (default 1.5), {@code damage} (default 4.0)
 */
public final class ProjectileMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent)) return false;
        double speed = ((Number) params.getOrDefault("speed", 1.5)).doubleValue();
        double damage = ((Number) params.getOrDefault("damage", 4.0)).doubleValue();

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().multiply(speed));
        projectile.setMetadata("skilling_damage",
                new FixedMetadataValue(JavaPlugin.getPlugin(
                        io.github.chasehuegel.skilling.Skilling.class
                ), damage));
        return true;
    }
}
