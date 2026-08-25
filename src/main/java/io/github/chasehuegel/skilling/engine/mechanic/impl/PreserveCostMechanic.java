package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Rolls a percentage chance to preserve (not consume) the triggering ability's
 * catalyst cost items on a successful activation.
 *
 * <p>This is a <em>rider</em> mechanic: it is listed alongside the ability's real
 * action mechanic (a cast, a ritual transmute) so a thrifty wizard occasionally
 * keeps the catalyst that a successful activation would otherwise spend. The
 * mechanic always counts as an activation attempt — it returns {@code true}
 * whenever a chance is configured, whether or not the roll lands — and reports
 * whether the roll succeeded via {@code didProc()}. The engine reads that report
 * after execution and, for the same abilities' {@code cost} item deductions,
 * skips the consumption when it proc'd. Cooldown, exhaustion (hunger), and
 * durability costs are still consumed normally; only the material catalyst is
 * preserved.
 *
 * <p>Because the preserve signal must reach the engine's post-execution consume
 * step, it is engine-integrated rather than a self-contained action: the engine's
 * dispatch loop recognizes any {@link PreserveCostMechanic} instance and carries
 * its {@code didProc()} result into the {@code RequirementEngine.consume} call.
 *
 * <p><b>YAML key:</b> {@code core:preserve_cost}
 * <br>Params: {@code chance} (0-100, percentage chance to keep the catalyst)
 */
public final class PreserveCostMechanic implements ProcAwareMechanic {

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
        double chance = ((Number) params.getOrDefault("chance", 0)).doubleValue();
        if (chance <= 0) return false;
        procced = randomSource.getAsDouble() <= chance;
        // Always counts as an activation attempt: the catalyst is either spent
        // (roll missed) or preserved (roll landed), both reached this step.
        return true;
    }

    @Override
    public boolean didProc() {
        return procced;
    }
}
