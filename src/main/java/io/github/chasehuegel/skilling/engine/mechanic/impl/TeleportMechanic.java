package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

public final class TeleportMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent)) return false;
        double range = ((Number) params.getOrDefault("range", 10.0)).doubleValue();
        if (range <= 0) return false;

        Location target;
        Block targetBlock = player.getTargetBlockExact((int) range);
        if (targetBlock != null) {
            target = targetBlock.getLocation();
        } else {
            target = player.getLocation().add(
                    player.getLocation().getDirection().multiply(range)
            );
        }
        Location safeTarget = findSafeLocation(target);
        if (safeTarget != null) {
            player.teleport(safeTarget);
        }
        return true;
    }

    private Location findSafeLocation(Location loc) {
        Location safe = loc.clone();
        safe.setY(safe.getY() + 1);
        if (safe.getBlock().isEmpty() && safe.add(0, 1, 0).getBlock().isEmpty()) {
            return safe.subtract(0, 1, 0);
        }
        for (int y = 0; y > -3; y--) {
            Location check = loc.clone().add(0, y, 0);
            if (check.getBlock().isEmpty() && check.add(0, 1, 0).getBlock().isEmpty()) {
                return check;
            }
        }
        return null;
    }
}
