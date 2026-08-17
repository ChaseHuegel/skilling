package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Sets down every mob the player carries on an empty-hand right-click of air.
 *
 * <p>On {@link PlayerInteractEvent} with {@link Action#RIGHT_CLICK_AIR}, all
 * passengers riding the player are ejected via {@code eject()}, the companion
 * gesture to {@code core:pick_up_mob}. The empty main hand makes the drop
 * deliberate, so it never fires while the player right-clicks air with a tool,
 * food, or throwable in hand.
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
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_AIR) return false;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return false;
        if (player.getPassengers().isEmpty()) return false;
        return player.eject();
    }
}
