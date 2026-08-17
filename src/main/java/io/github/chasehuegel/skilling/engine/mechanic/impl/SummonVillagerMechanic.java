package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Summons a jobless (profession {@code NONE}) villager at the player's location
 * on a right-click interaction.
 *
 * <p>The summoned villager is a normal vanilla entity with no persistent
 * Skilling state: removing the plugin leaves it as an ordinary villager that
 * despawns or wanders like any other. Right-click actions only, so a
 * left-click can never summon accidentally.
 *
 * <p><b>YAML key:</b> {@code core:summon_villager}
 * <p><b>Parameters:</b> none
 */
public final class SummonVillagerMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_AIR
                && interact.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        player.getWorld().spawn(player.getLocation(), Villager.class,
                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM, false,
                villager -> villager.setProfession(Villager.Profession.NONE));
        return true;
    }
}
