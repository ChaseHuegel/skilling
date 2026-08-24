package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.Map;

/**
 * Reduces the damage dealt to the vehicle the player is riding by a flat
 * percentage.
 *
 * <p>The activating player must be a passenger of the damaged vehicle; otherwise
 * the mechanic is a no-op. Damage is multiplied by {@code (1 - reduction / 100)}
 * so a reduction of {@code 25} blunts every qualifying blow to three-quarters of
 * its value. This complements {@code core:reduce_damage}, which guards the rider
 * themselves, by guarding the mount that absorbs hits while mounted.
 *
 * <p><b>YAML key:</b> {@code core:mounted_ward}
 * <br>Params: {@code reduction} (0-100, percentage of mount damage removed)
 */
public final class MountedWardMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent de)) return false;
        Entity damaged = de.getEntity();
        if (!(damaged instanceof Vehicle vehicle)) return false;
        if (!isRiding(player, vehicle)) return false;
        double reduction = ((Number) params.getOrDefault("reduction", 0)).doubleValue();
        if (reduction <= 0) return false;
        double reduced = de.getDamage() * (1 - Math.min(100, reduction) / 100);
        de.setDamage(Math.max(0, reduced));
        return true;
    }

    /**
     * Whether the activating player is a passenger of the given vehicle.
     *
     * @param player  the activating player
     * @param vehicle the damaged vehicle
     * @return true when the player is mounted on it
     */
    private static boolean isRiding(Player player, Vehicle vehicle) {
        if (vehicle instanceof Entity entity) {
            for (Entity passenger : entity.getPassengers()) {
                if (player.equals(passenger)) return true;
            }
        }
        return false;
    }
}