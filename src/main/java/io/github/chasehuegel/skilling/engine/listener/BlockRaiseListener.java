package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.engine.combat.BlockHistory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Records when a player raises a shield so the {@code timed_block} state filter can
 * measure a well-timed block.
 *
 * <p>Bukkit exposes blocking only as a boolean and offers no public "block started"
 * event, so this listener watches the vanilla shield-raise gesture: a right-click
 * with a shield in either hand. Covering the offhand is required for the tandem
 * stance (weapon in the main hand, shield in the offhand), where right-click raises
 * the offhand shield even though the interacted slot is the main hand.
 */
public final class BlockRaiseListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (holdsShield(player)) {
            BlockHistory.record(player);
        }
    }

    /**
     * Whether the player can raise a shield: a shield in either hand.
     *
     * @param player the player holding the potential shield
     * @return true if a shield is equipped in the main or off hand
     */
    private static boolean holdsShield(Player player) {
        return isShield(player.getInventory().getItem(EquipmentSlot.HAND))
                || isShield(player.getInventory().getItem(EquipmentSlot.OFF_HAND));
    }

    private static boolean isShield(org.bukkit.inventory.ItemStack item) {
        return item != null && item.getType() == Material.SHIELD;
    }
}