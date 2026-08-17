package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Summons a wandering trader at the player's location on a right-click
 * interaction.
 *
 * <p>The summoned trader keeps vanilla despawn behavior
 * ({@code WanderingTrader#setDespawnDelay} is untouched), so it is a transient
 * entity that leaves no persistent state on plugin removal. Right-click actions
 * only.
 *
 * <p><b>YAML key:</b> {@code core:summon_wandering_trader}
 * <p><b>Parameters:</b> none
 */
public final class SummonWanderingTraderMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_AIR
                && interact.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        player.getWorld().spawn(player.getLocation(), WanderingTrader.class,
                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM, false, null);
        return true;
    }
}
