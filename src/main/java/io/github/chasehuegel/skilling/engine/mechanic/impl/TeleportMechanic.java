package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Teleports the player to the targeted block or in the look direction up to a maximum range on {@link PlayerInteractEvent}.
 * Includes safe-location fallback and bounds checking.
 *
 * <p><b>YAML key:</b> {@code teleport}
 * <p><b>Optional parameters:</b> {@code range} (default 10.0, maximum teleport distance in blocks)
 */
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
        if (safeTarget == null) return false;
        return player.teleport(safeTarget);
    }

    private Location findSafeLocation(Location loc) {
        Location safe = loc.clone();
        safe.setY(safe.getY() + 1);
        Location above = safe.clone().add(0, 1, 0);
        if (safe.getBlock().isEmpty() && above.getBlock().isEmpty()) {
            return safe;
        }
        for (int y = 0; y > -3; y--) {
            Location check = loc.clone().add(0, y, 0);
            Location aboveCheck = check.clone().add(0, 1, 0);
            if (check.getBlock().isEmpty() && aboveCheck.getBlock().isEmpty()) {
                return check;
            }
        }
        return null;
    }
}
