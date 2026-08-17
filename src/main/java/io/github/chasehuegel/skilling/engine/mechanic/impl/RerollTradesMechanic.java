package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.Map;

/**
 * Re-rolls a villager's trade offers by cycling its profession on a
 * right-click of the villager.
 *
 * <p>Setting the profession to {@code NONE} clears the villager's offers, and
 * restoring the original profession regenerates a fresh random set. This is the
 * vanilla trade refresh without breaking and replacing the workstation, so the
 * villager keeps its profession, type, and level. A villager with no profession
 * (already {@code NONE}) cannot re-roll and is left untouched.
 *
 * <p><b>YAML key:</b> {@code core:reroll_trades}
 * <p><b>Parameters:</b> none
 */
public final class RerollTradesMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEntityEvent interact)) return false;
        if (!(interact.getRightClicked() instanceof Villager villager)) return false;
        Villager.Profession profession = villager.getProfession();
        if (profession == Villager.Profession.NONE) return true;
        villager.setProfession(Villager.Profession.NONE);
        villager.setProfession(profession);
        return true;
    }
}
