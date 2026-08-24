package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Temporarily increases the movement speed of the vehicle the player is riding.
 *
 * <p><b>Why this targets the mount, not the rider:</b> {@code core:speed_bonus}
 * modifies the rider's {@code GENERIC_MOVEMENT_SPEED}, but a ridden entity is
 * moved by its own speed attribute, so a rider-side buff does nothing while
 * mounted. This mechanic applies the modifier to the ridden vehicle instead, so
 * a horse (or strider, camel, pig) actually travels faster. Boats and minecarts
 * have no speed attribute and are a no-op ({@code false}).
 *
 * <p>Repeated activations refresh the buff rather than stack: the modifier is
 * replaced under its resolved UUID and the prior scheduled removal is cancelled.
 * Removal runs on the mount's entity scheduler, which Paper retires when the
 * mount despawns (e.g. on dismount death), dropping the tracked entry.
 *
 * <p><b>YAML key:</b> {@code core:mounted_speed}
 * <br>Params: {@code multiplier} (e.g. 1.1 = 10% faster mount), {@code duration}
 * (optional, seconds, default 60), {@code uuid} (optional, stable modifier UUID)
 */
public final class MountedSpeedMechanic implements SkillMechanic {

    private static final String MODIFIER_NAME = "skilling_mounted_speed";

    /** Tracks scheduled speed-buff removals keyed by the mount id and modifier UUID. */
    private static final ConcurrentMap<String, ScheduledTask> PENDING_REMOVALS = new ConcurrentHashMap<>();

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(player.getVehicle() instanceof LivingEntity mount)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;
        int duration = ((Number) params.getOrDefault("duration", 60.0)).intValue();
        if (duration <= 0) return false;

        AttributeInstance inst = mount.getAttribute(Attribute.MOVEMENT_SPEED);
        if (inst == null) return false;
        double added = inst.getBaseValue() * (multiplier - 1.0);
        if (added <= 0) return false;

        UUID uuid = AttributeModifierHelper.resolveUuid(params.get("uuid"));
        return apply(mount, inst, uuid, added, duration);
    }

    /**
     * Applies the transient speed modifier to the mount under the given UUID,
     * replacing any existing modifier with that UUID and cancelling its prior
     * scheduled removal so a refresh cuts nothing short.
     *
     * @param mount    the ridden living mount
     * @param inst     the mount's movement-speed attribute
     * @param uuid     the modifier UUID (stable per ability when configured)
     * @param added    the additive modifier amount
     * @param duration the modifier lifetime in seconds
     * @return true if a modifier was applied
     */
    private static boolean apply(LivingEntity mount, AttributeInstance inst, UUID uuid,
                                 double added, int duration) {
        String mountId = mount.getUniqueId().toString();
        String key = mountId + ":" + uuid;
        ScheduledTask previous = PENDING_REMOVALS.remove(key);
        if (previous != null && !previous.isCancelled()) {
            previous.cancel();
        }

        AttributeModifier existing = inst.getModifier(uuid);
        if (existing != null) {
            inst.removeModifier(existing);
        }

        String name = MODIFIER_NAME;
        var modifier = new AttributeModifier(uuid, name, added, AttributeModifier.Operation.ADD_NUMBER);
        inst.addTransientModifier(modifier);

        ScheduledTask[] removal = { null };
        ScheduledTask task = mount.getScheduler().runDelayed(
                Skilling.getInstance(),
                t -> {
                    PENDING_REMOVALS.remove(key, t);
                    inst.removeModifier(uuid);
                },
                () -> PENDING_REMOVALS.remove(key, removal[0]),
                duration * 20L
        );
        removal[0] = task;
        if (task != null) {
            PENDING_REMOVALS.put(key, task);
        }
        return true;
    }

    /**
     * Cancels every tracked removal task (and strips nothing — the mounts may be
     * gone), then clears the tracker. Called on plugin disable and reload so the
     * static map cannot grow and retired entity-scheduler tasks are cleaned up.
     */
    public static void clearAll() {
        for (ScheduledTask task : PENDING_REMOVALS.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        PENDING_REMOVALS.clear();
    }

    /**
     * Test-only seam: number of tracked pending mount-speed removals.
     *
     * @return the size of the pending-removal tracker
     */
    static int pendingRemovalsSize() {
        return PENDING_REMOVALS.size();
    }
}