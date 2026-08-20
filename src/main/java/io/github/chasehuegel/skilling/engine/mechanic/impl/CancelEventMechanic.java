package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Cancels the triggering event, optionally gated by a percentage chance.
 *
 * <p>A generic suppression hammer for any cancellable event: pressure-plate and
 * tripwire steps, sculk sensor receives, and so on. It is bound to whatever
 * {@code target} / {@code state} filter scopes it, so one mechanic covers many
 * "do not trigger X while Y" abilities without a per-mechanic implementation.
 *
 * <p>The {@code chance} (0-100) is optional: absent means the event is always
 * cancelled. A landed cancel counts as an activation attempt, so the ability's
 * cost is consumed once per use and a failed roll cannot be retried for free.
 * A pass never changes already-consumed costs (the requirement check and
 * consumption happen once per activation attempt).
 *
 * <p><b>YAML key:</b> {@code core:cancel_event}
 * <br>Params: {@code chance} (optional, 0-100, percentage to cancel)
 */
public final class CancelEventMechanic implements ProcAwareMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    private boolean procced;

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
        if (!(event instanceof Cancellable cancellable)) return false;
        // Absent chance cancels unconditionally (roll always wins).
        double chance = ((Number) params.getOrDefault("chance", 101.0)).doubleValue();
        if (chance <= 0) return false;
        procced = randomSource.getAsDouble() <= Math.min(chance, 100.0);
        if (procced) {
            cancellable.setCancelled(true);
        }
        return true;
    }

    @Override
    public boolean didProc() {
        return procced;
    }
}
