package io.github.chasehuegel.skilling.engine.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Global inventory security listener that enforces UI protection rules.
 *
 * <p>Two layers of defense:
 * <ol>
 *   <li><b>Strict Routing:</b> All click/drag events within custom holders
 *       are cancelled, with explicit denial of shift-clicks, number-key
 *       swaps, and off-hand swaps.</li>
 *   <li><b>Poison Pill Vaporization:</b> Any {@link ItemStack} tagged with
 *       the {@link PoisonPillTag} found outside a controlled UI context
 *       is deleted.</li>
 * </ol>
 */
public final class UIProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null || top.getHolder() != null) return;

        // Check if this is our custom UI (null holder indicates Skilling UI)
        // If the clicked inventory has a null holder, it's our custom UI
        if (top.getHolder() != null) return;

        // Cancel all interactions in custom UI slots
        event.setCancelled(true);

        // Explicitly deny dangerous actions
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getClick().isShiftClick()
                || event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null || top.getHolder() != null) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        // Vaporize tagged items that escaped the UI
        ItemStack item = event.getItem().getItemStack();
        if (item.hasItemMeta() && PoisonPillTag.isTagged(item.getItemMeta())) {
            event.getItem().remove();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(org.bukkit.event.entity.ItemSpawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        if (item.hasItemMeta() && PoisonPillTag.isTagged(item.getItemMeta())) {
            event.setCancelled(true);
        }
    }
}