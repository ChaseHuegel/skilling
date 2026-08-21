package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Teleports the player back to the position a held compass is bound to — the
 * vanilla {@code lodestone_tracker} the compass carries. The late-game
 * counterpart to {@code core:locate}: even where you planted a genuine lodestone,
 * a compass bound to it returns you there (recall to your own camp).
 *
 * <p>Fires only on a main-hand right-click with a {@code minecraft:compass}
 * (a normal or lodestone compass). A compass with no tracker component bound is a
 * no-op, so it never recalls a player to nothing. On a successful recall it also
 * applies a vanilla item cooldown to the compass's slot UI (see
 * {@code cooldown_ticks}); while that cooldown is active the recall is a no-op,
 * so the slot UI both shows readiness and paces the ability. Cross-dimension
 * recall works by teleporting to the bound location directly, which the Paper API
 * loads.
 *
 * <p><b>YAML key:</b> {@code core:teleport_lodestone}
 * <br>Params: {@code cooldown_ticks} (optional, vanilla item-cooldown ticks
 * applied to the compass on a successful recall)
 */
public record TeleportLodestoneMechanic() implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent ie)) return false;
        if (ie.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return false;
        Action action = ie.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.COMPASS) return false;
        if (!held.hasData(DataComponentTypes.LODESTONE_TRACKER)) return false;
        LodestoneTracker tracker = held.getData(DataComponentTypes.LODESTONE_TRACKER);
        if (tracker == null) return false;
        Location target = tracker.location();
        if (target == null || target.getWorld() == null) return false;

        double ticks = ((Number) params.getOrDefault("cooldown_ticks", 0.0)).doubleValue();
        // The vanilla item cooldown is the real gate: while it is active the
        // recall is a no-op (spending nothing), so the slot UI controls pacing.
        if (ticks > 0 && player.hasCooldown(Material.COMPASS)) return false;

        player.teleport(target.clone().add(0, 1, 0));
        if (ticks > 0) {
            player.setCooldown(Material.COMPASS, (int) ticks);
        }
        return true;
    }
}