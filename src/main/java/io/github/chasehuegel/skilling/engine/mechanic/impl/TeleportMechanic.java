package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Teleports the player to the targeted block or in the look direction up to a maximum range on {@link PlayerInteractEvent}.
 * Includes safe-location fallback and bounds checking.
 *
 * <p>Only a right-click (air or block) teleports; a left-click is a no-op so it
 * never consumes the ability cost.
 *
 * <p>The destination is clamped to a survivable landing: a passable feet/head
 * column standing on a solid floor within a fall-safe drop distance. If no such
 * spot exists (e.g. looking over the void or beyond the world floor) the
 * teleport is refused, so the player can never be dropped into the void or an
 * unsurvivable fall.
 *
 * <p><b>YAML key:</b> {@code core:teleport}
 * <p><b>Optional parameters:</b> {@code range} (default 10.0, maximum teleport distance in blocks)
 */
public final class TeleportMechanic implements SkillMechanic {

    /** Maximum blocks below the target that a survivable landing may fall. */
    static final int FALL_SAFE_DISTANCE = 8;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interactEvent)) return false;
        org.bukkit.event.block.Action action = interactEvent.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return false;
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

    /**
     * Finds a survivable landing spot near the given location: a passable
     * feet/head column standing on a solid floor, searched from one block above
     * the target down to {@link #FALL_SAFE_DISTANCE} blocks below. Returns null
     * when no such spot exists (including when the search reaches the world's
     * void floor), so the caller refuses the teleport rather than dropping the
     * player into the air or the void.
     *
     * @param loc the requested destination
     * @return a safe landing location, or null
     */
    static Location findSafeLocation(Location loc) {
        World world = loc.getWorld();
        int minY = world.getMinHeight();
        double feetY = loc.getY() + 1;
        for (int drop = 0; drop <= FALL_SAFE_DISTANCE; drop++) {
            Location feet = loc.clone();
            feet.setY(feetY);
            Location below = feet.clone().subtract(0, 1, 0);
            // The floor search has reached the world's void floor; nothing below is solid.
            if (below.getBlockY() < minY) break;
            if (feet.getBlock().isEmpty() && feet.clone().add(0, 1, 0).getBlock().isEmpty()
                    && below.getBlock().isSolid()) {
                return feet;
            }
            feetY -= 1;
        }
        return null;
    }
}
