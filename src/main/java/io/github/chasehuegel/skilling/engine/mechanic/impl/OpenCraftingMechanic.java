package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Opens a temporary, full-screen 3x3 crafting window for the player without
 * needing a crafting table block, so a "mobile workshop" ability can be used
 * anywhere.
 *
 * <p>The window is a standard vanilla workbench GUI ({@link Player#openWorkbench}),
 * so it stores nothing and modifying it in play cannot persist anything when
 * the plugin is removed. It must be bound to a right-click trigger
 * ({@code right_click_air} or {@code right_click_block}); a left-click is a
 * no-op so it never steals a swing. Gating (held item, hunger, cooldown) lives
 * in the ability's {@code requirements:} block, not in this mechanic.
 *
 * <p><b>YAML key:</b> {@code core:open_crafting}
 * <p><b>Parameters:</b> none
 */
public final class OpenCraftingMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_AIR
                && interact.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        // null location + force=true opens a full-screen crafting table window
        // even when no physical table exists, the standard "craft anywhere" window.
        player.openWorkbench(null, true);
        return true;
    }
}
