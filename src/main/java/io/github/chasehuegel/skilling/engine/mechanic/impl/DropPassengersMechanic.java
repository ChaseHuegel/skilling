package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.Map;

/**
 * Ejects every mob the player carries.
 *
 * <p>Each passenger is dismounted individually via {@code leaveVehicle()}, the
 * most reliable dismount for a living-entity vehicle, rather than a single
 * {@code eject()} call which can silently no-op on a player in some Paper
 * versions. The mechanic is bound to the {@code right_click_air} trigger by the
 * bundled Set Down ability; right-clicking a carried mob directly also sets it
 * down through {@code core:pick_up_mob}'s toggle.
 *
 * <p>Per the {@link SkillMechanic} return contract, {@code false} is returned
 * when there is nothing to set down, so a drop with no carried mobs spends no
 * cost or cooldown.
 *
 * <p><b>YAML key:</b> {@code core:drop_passengers}
 * <p><b>Parameters:</b> none
 */
public final class DropPassengersMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        boolean any = false;
        for (org.bukkit.entity.Entity passenger : java.util.List.copyOf(player.getPassengers())) {
            if (passenger.leaveVehicle()) any = true;
        }
        return any;
    }
}
