package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.BaseDamageCancelMechanic;
import java.util.Map;

/**
 * Chance to block incoming damage entirely (shield-like).
 *
 * <p>YAML key: {@code core:block_damage}
 * <br>Params: {@code chance} (0-100, percentage)
 */
public final class BlockDamageMechanic extends BaseDamageCancelMechanic {

    @Override
    protected double getChance(Map<String, Object> params) {
        return ((Number) params.getOrDefault("chance", 0)).doubleValue();
    }
}
