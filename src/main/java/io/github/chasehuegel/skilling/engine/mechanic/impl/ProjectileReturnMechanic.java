package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ProjectileReturnMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof ProjectileHitEvent hitEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        if (ThreadLocalRandom.current().nextDouble(100) > chance) return false;
        var projectile = hitEvent.getEntity();
        if (!(projectile.getShooter() instanceof Player shooter) || !shooter.equals(player)) return false;
        var type = projectile.getType();
        Material material = switch (type) {
            case TRIDENT -> Material.TRIDENT;
            case SNOWBALL -> Material.SNOWBALL;
            case EGG -> Material.EGG;
            default -> null;
        };
        if (material == null) return false;
        player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(material));
        return true;
    }
}
