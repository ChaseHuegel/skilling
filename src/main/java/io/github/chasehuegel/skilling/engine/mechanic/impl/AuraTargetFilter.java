package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;

/**
 * Resolves the {@code targets} parameter of aura mechanics
 * ({@code allies | hostiles | all}).
 *
 * <p>The default is {@code allies} so a beneficial aura (regen, strength) never
 * buffs hostile mobs unless the config explicitly opts into them; an unknown
 * value also falls back to {@code allies}, the safe default. {@code hostiles}
 * selects only {@link Enemy} entities (monsters and angered neutrals), and
 * {@code all} selects every living entity.
 */
final class AuraTargetFilter {

    private AuraTargetFilter() {}

    /**
     * Whether a living entity should receive the effect under the given filter.
     *
     * @param targets the configured {@code targets} value (unknown defaults to {@code allies})
     * @param entity  the candidate target
     * @return true if the entity should receive the effect
     */
    static boolean accepts(String targets, LivingEntity entity) {
        return switch (targets) {
            case "hostiles" -> entity instanceof Enemy;
            case "all" -> true;
            default -> !(entity instanceof Enemy);
        };
    }
}
