package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Ejects every mob the player carries.
 *
 * <p><b>YAML key:</b> {@code core:drop_passengers}
 * <p><b>Parameters:</b> none
 */
public final class DropPassengersMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (player.getPassengers().isEmpty()) return false;
        return player.eject();
    }
}
