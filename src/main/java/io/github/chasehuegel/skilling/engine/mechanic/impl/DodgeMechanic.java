package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.BaseDamageCancelMechanic;
import java.util.Map;

/**
 * Chance to completely negate incoming damage (evasion).
 *
 * <p>YAML key: {@code core:dodge}
 * <br>Params: {@code chance} (0-100, percentage)
 */
public final class DodgeMechanic extends BaseDamageCancelMechanic {

    @Override
    protected double getChance(Map<String, Object> params) {
        return ((Number) params.getOrDefault("chance", 0)).doubleValue();
    }
}
