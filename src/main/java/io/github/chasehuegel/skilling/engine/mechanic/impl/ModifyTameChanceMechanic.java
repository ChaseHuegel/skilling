package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTameEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Scales the chance of a successful tame on {@link EntityTameEvent}.
 *
 * <p>{@link EntityTameEvent} fires only <em>after</em> a tame has succeeded, so a
 * multiplier of {@code 1.0} leaves the vanilla outcome untouched, a multiplier
 * greater than {@code 1.0} preserves the success, and a multiplier below
 * {@code 1.0} cancels an otherwise-successful tame with probability
 * {@code 1 - multiplier} (e.g. 0.5 → half of tames are undone). A higher
 * multiplier is never worse than a lower one.
 *
 * <p><b>YAML key:</b> {@code core:modify_tame_chance}
 * <br>Params: {@code multiplier} (multiplicative tame-chance factor, 1.0 = vanilla)
 */
public final class ModifyTameChanceMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble();

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to force a deterministic
     * tame roll; production always uses {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a value in [0, 1)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityTameEvent tameEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;

        if (multiplier < 1.0 && randomSource.getAsDouble() > multiplier) {
            tameEvent.setCancelled(true);
        }
        return true;
    }
}
