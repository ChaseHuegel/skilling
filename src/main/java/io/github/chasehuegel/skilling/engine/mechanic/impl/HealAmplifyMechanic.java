package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import java.util.Map;

/**
 * Multiplies the health a player regains from any source (food, the
 * regeneration effect, instant-health potions/golden apples, and healing from
 * other plugins). It rewrites the amount on {@link EntityRegainHealthEvent}, so
 * the same {@code multiplier} the player's level evaluates to is applied to
 * every recovery in real time — a continuous, always-on scalar rather than a
 * one-shot buff.
 *
 * <p>Flat {@code setHealth} healing (for example the engine's own
 * {@code core:lifesteal}) does not fire a regain-health event and is therefore
 * not scaled; the mechanic only amplifies event-driven recovery.
 *
 * <p><b>YAML key:</b> {@code core:heal_amplify}
 * <br>Params: {@code multiplier} (default 1.0; values &gt; 1 amplify recovery.
 * A value of 1.0 (or below) is a no-op)
 */
public final class HealAmplifyMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityRegainHealthEvent heal)) return false;
        if (heal.isCancelled() || !heal.getEntity().equals(player)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 1.0) return false;
        heal.setAmount(heal.getAmount() * multiplier);
        return true;
    }
}