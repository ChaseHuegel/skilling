package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Shortens the hotbar cooldown a thrown trident places before the next throw.
 *
 * <p>Throwing a trident puts the trident slot on the vanilla item-use cooldown
 * for a moment before it can be thrown again; this mechanic sets that cooldown
 * to a level-scaled shorter window, so a thrower Volleys more often. It mutates
 * only transient player state (the native hotbar cooldown), so it is a safe
 * unplug and uses vanilla UI language. The vanilla throw cooldown is treated as
 * 20 ticks; the reduced value is floored so a runaway level-scaled reduction
 * cannot permit an instant, spam-cancelling rethrow.
 *
 * <p>{@code false} is returned when the mechanic could not act: the event is not
 * a player-launched trident, or the reduction is not positive.
 *
 * <p><b>YAML key:</b> {@code core:throw_haste}
 * <p><b>Required parameters:</b> {@code reduction} (0-100, percentage to shorten
 * the throw cooldown)
 */
public final class ThrowHasteMechanic implements SkillMechanic {

    /** The vanilla trident-throw cooldown in ticks (1 second). */
    private static final int VANILLA_THROW_COOLDOWN_TICKS = 20;

    /** Lower floor on the reduced cooldown so a rethrow always has a real cost. */
    private static final int MIN_COOLDOWN_TICKS = 4;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof ProjectileLaunchEvent launch)) return false;
        if (!(launch.getEntity().getShooter() instanceof Player shooter) || !shooter.equals(player)) return false;
        if (launch.getEntity().getType() != EntityType.TRIDENT) return false;

        double reduction = ((Number) params.getOrDefault("reduction", 0.0)).doubleValue();
        if (reduction <= 0) return false;

        int ticks = (int) Math.round(VANILLA_THROW_COOLDOWN_TICKS * (1.0 - reduction / 100.0));
        ticks = Math.max(MIN_COOLDOWN_TICKS, Math.min(ticks, VANILLA_THROW_COOLDOWN_TICKS));
        player.setCooldown(Material.TRIDENT, ticks);
        return true;
    }
}