package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Rolls a percentage chance to cancel incoming {@link EntityDamageEvent} damage.
 *
 * <p>This is the single implementation behind three registered flavor aliases —
 * {@code core:dodge}, {@code core:block_damage}, and {@code core:cancel_damage} —
 * which behave identically and exist purely so skill configs read as a dodge,
 * a shield block, or an evade.
 *
 * <p>Reaching the chance roll counts as an activation attempt: the mechanic
 * returns {@code true} whether or not the roll succeeds, so the ability's cost
 * and cooldown are consumed exactly once per attempt and a failed roll cannot
 * be retried for free. {@code false} is only returned when the mechanic could
 * not act at all (wrong event type, damage not on the player, no chance).
 *
 * <p><b>YAML keys:</b> {@code core:dodge}, {@code core:block_damage},
 * {@code core:cancel_damage}
 * <br>Params: {@code chance} (0-100, percentage chance to negate damage)
 *
 * <p><b>Proc reporting:</b> implements {@link ProcAwareMechanic} so the engine
 * can gate {@code feedback.success_only} cues to the roll actually landing —
 * a dodge that whiffs stays silent, a dodge that saves you flashes feedback.
 */
public final class DamageCancelMechanic implements ProcAwareMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    private boolean procced;

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to force a deterministic
     * roll; production always uses {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a percentage in [0, 100)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0)).doubleValue();
        if (chance <= 0) return false;
        // Record whether the roll lands so the engine can gate success-only feedback.
        procced = randomSource.getAsDouble() <= chance;
        if (procced) {
            de.setCancelled(true);
        }
        return true;
    }

    @Override
    public boolean didProc() {
        return procced;
    }
}
