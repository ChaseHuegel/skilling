package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.BaseDamageCancelMechanic;
import java.util.Map;

/**
 * Cancels incoming damage with a percentage chance.
 *
 * <p><b>YAML key:</b> {@code cancel_damage}
 * <p><b>Required parameters:</b> {@code chance} (0-100, percentage chance to negate damage)
 */
public final class CancelDamageMechanic extends BaseDamageCancelMechanic {

    @Override
    protected double getChance(Map<String, Object> params) {
        return ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
    }
}
