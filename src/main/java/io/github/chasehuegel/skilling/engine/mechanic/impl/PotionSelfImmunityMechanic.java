package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PotionSplashEvent;
import java.util.Map;

/**
 * Shields the thrower and allied players from the potion they threw.
 *
 * <p>On {@link PotionSplashEvent}, every affected living entity that is either
 * the throwing player or another player is zeroed out of the splash, so a
 * thrown splash/lingering potion — harmful or otherwise — never harms the
 * thrower or their own players. This is the vanilla+ friction-elimination
 * counterpart to brewing offensive potions: you can throw poison or harming
 * at a crowd without self-inflicting friendly fire on your group, while hostile
 * mobs are still hit normally.
 *
 * <p>The mechanic's {@code player} argument is the thrower (the dispatcher
 * routes {@code potion_splash} only to the player who threw it). Hostile mobs
 * are untouched. Returns {@code true} when at least one player was shielded
 * so the shared ability state is marked active; it is a passive with no cost.
 *
 * <p><b>YAML key:</b> {@code core:potion_self_immunity}
 * <br>Params: none
 */
public final class PotionSelfImmunityMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PotionSplashEvent splash)) return false;

        boolean shielded = false;
        for (LivingEntity entity : splash.getAffectedEntities()) {
            if (entity == null) continue;
            if (entity.equals(player) || entity instanceof Player) {
                splash.setIntensity(entity, 0.0);
                shielded = true;
            }
        }
        return shielded;
    }
}
