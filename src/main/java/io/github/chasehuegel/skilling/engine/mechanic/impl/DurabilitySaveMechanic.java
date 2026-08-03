package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemDamageEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Gives a percentage chance to negate item durability damage on {@link PlayerItemDamageEvent}.
 *
 * <p>Reaching the chance roll counts as an activation attempt: the mechanic
 * returns {@code true} whether or not the roll succeeds, so the ability's cost
 * and cooldown are consumed exactly once per attempt and a failed roll cannot
 * be retried for free. {@code false} is only returned when the mechanic could
 * not act at all (wrong event type, damage not on the player, or no chance).
 *
 * <p><b>YAML key:</b> {@code core:durability_save}
 * <p><b>Required parameters:</b> {@code chance} (0-100, percentage to negate damage)
 */
public final class DurabilitySaveMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    /**
     * Test-only seam to force a deterministic roll; production always uses
     * {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a percentage in [0, 100)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerItemDamageEvent damageEvent)) return false;
        if (!damageEvent.getPlayer().equals(player)) return false;

        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        if (randomSource.getAsDouble() < chance) {
            damageEvent.setCancelled(true);
        }
        return true;
    }
}
