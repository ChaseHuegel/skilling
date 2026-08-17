package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import java.util.Map;

/**
 * Grants the traded villager bonus experience on {@link PlayerTradeEvent}, so
 * its offers tier up faster than the vanilla trade loop alone.
 *
 * <p>The XP is added to the villager's current total
 * ({@link Villager#getVillagerExperience()} + amount), letting the vanilla
 * level thresholds advance naturally. Only {@link Villager} targets apply;
 * trading with a {@link org.bukkit.entity.WanderingTrader} (which has no
 * experience concept) is a no-op.
 *
 * <p><b>YAML key:</b> {@code core:villager_xp}
 * <p><b>Required parameters:</b> {@code amount} (bonus experience points)
 */
public final class VillagerXpMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerTradeEvent tradeEvent)) return false;
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        if (amount <= 0) return false;
        if (!(tradeEvent.getVillager() instanceof Villager villager)) return false;
        villager.setVillagerExperience(villager.getVillagerExperience() + (int) Math.round(amount));
        return true;
    }
}
