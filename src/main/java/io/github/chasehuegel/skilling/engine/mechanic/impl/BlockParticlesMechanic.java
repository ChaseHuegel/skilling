package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Spawns a configured particle burst at the event's clicked/broken/placed block.
 *
 * <p>YAML key: {@code core:block_particles}
 * <br>Params: {@code particle} (Particle enum name), {@code count} (default 1),
 * {@code speed} (default 0).
 *
 * <p>Returns true only when the event carries a block location (a right-click on a
 * block, a block break, or a block place), so it can serve as the executable action
 * that triggers the requirement {@code consume} step for item costs. An absent or
 * blank {@code particle} makes the mechanic a no-op (used purely as a consume
 * trigger); a present-but-unknown particle is rejected at skill load by the
 * registry validator and throws here only if a real bug or an unvalidated addon
 * mechanic passes one.
 */
public final class BlockParticlesMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        Location blockLocation = resolveBlockLocation(event);
        if (blockLocation == null || blockLocation.getWorld() == null) return false;

        String type = String.valueOf(params.getOrDefault("particle", ""));
        int count = ((Number) params.getOrDefault("count", 1)).intValue();
        double speed = ((Number) params.getOrDefault("speed", 0.0)).doubleValue();
        if (type.isBlank()) return false;

        Particle particle = Particle.valueOf(type.toUpperCase());
        blockLocation.getWorld().spawnParticle(particle, blockLocation, count, 0, 0, 0, speed);
        return true;
    }

    /**
     * Resolves the block location carried by a block interaction event, or null.
     *
     * @param event the triggering event
     * @return the block location, or null if the event does not target a block
     */
    public static Location resolveBlockLocation(Event event) {
        if (event instanceof PlayerInteractEvent ie
                && ie.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                && ie.getClickedBlock() != null) {
            return ie.getClickedBlock().getLocation();
        }
        if (event instanceof BlockBreakEvent be) {
            return be.getBlock().getLocation();
        }
        if (event instanceof BlockPlaceEvent pe) {
            return pe.getBlockPlaced().getLocation();
        }
        return null;
    }
}
