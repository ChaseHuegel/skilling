package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Gives a percentage chance to keep the entire inventory on {@link PlayerDeathEvent}.
 *
 * <p>A single roll per death is made. On success the death is marked to keep
 * inventory and the drop list is cleared, so nothing spawns and the inventory is
 * preserved intact into respawn. On failure the normal death (full drop) proceeds.
 * The mechanic no-ops harmlessly when the server already keeps inventory (the drop
 * list is already empty). Reaching the roll counts as an activation attempt
 * whether or not it succeeds, matching the chance-mechanic contract.
 *
 * <p><b>YAML key:</b> {@code core:keep_on_death}
 * <br>Params: {@code chance} (0-100, percentage to keep the whole inventory)
 */
public final class KeepOnDeathMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    /** Test seam to force a deterministic roll (0-100). */
    static void setRandomSource(DoubleSupplier source) { randomSource = source; }

    /** Restores the production random source. */
    static void reset() { randomSource = () -> ThreadLocalRandom.current().nextDouble(100); }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerDeathEvent deathEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        if (randomSource.getAsDouble() < chance) {
            deathEvent.setKeepInventory(true);
            deathEvent.getDrops().clear();
        }
        return true;
    }
}
