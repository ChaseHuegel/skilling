package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.view.AnvilView;
import java.util.Map;

/**
 * Removes the anvil's "Too Expensive" wall by raising its maximum repair cost
 * and lifting the enchantment-level restriction.
 *
 * <p>On every {@link PrepareAnvilEvent} the open {@link AnvilView}'s maximum
 * repair cost is raised to the level-scaled {@code cap} (never below the
 * vanilla 40) and {@link AnvilView#bypassEnchantmentLevelRestriction} is
 * enabled, so repeatedly repaired gear stays repairable as long as the player
 * can pay the XP. Bind it to the ungated {@code anvil_prepare} trigger so the
 * cap is raised before the player commits, including when the anvil would
 * otherwise resolve to a null "Too Expensive" result.
 *
 * <p><b>YAML key:</b> {@code core:uncap_repair}
 * <br>Params: {@code cap} (level-scaled, at least 40; the raised maximum repair cost)
 */
public final class UncapRepairMechanic implements SkillMechanic {

    private static final int VANILLA_CAP = 40;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PrepareAnvilEvent prepare)) return false;

        AnvilView view = prepare.getView();
        if (view == null) return false;

        int cap = ((Number) params.getOrDefault("cap", VANILLA_CAP)).intValue();
        if (cap < VANILLA_CAP) cap = VANILLA_CAP;

        view.setMaximumRepairCost(cap);
        view.bypassEnchantmentLevelRestriction(true);
        return true;
    }
}