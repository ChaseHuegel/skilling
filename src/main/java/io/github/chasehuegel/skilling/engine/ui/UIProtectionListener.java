package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;

/**
 * Global inventory security listener that enforces UI protection rules
 * and handles pagination navigation for the skills chest GUI.
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
 *
 * <p><b>Navigation:</b> In paginated mode ({@link SkillInventoryHolder#getPageOrder()}
 * is non-null), clicks on slot 45 open the previous page and clicks on slot
 * 53 open the next page. The inventory is swapped entirely; arrow items are
 * not consumed.
 */
public final class UIProtectionListener implements Listener {

    private static final int SLOT_PREV = 45;
    private static final int SLOT_NEXT = 53;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;

        // Check if this is our custom UI (SkillInventoryHolder indicates Skilling UI)
        if (!(top.getHolder() instanceof SkillInventoryHolder holder)) return;

        // Cancel all interactions in custom UI slots
        event.setCancelled(true);

        // Explicitly deny dangerous actions
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getClick().isShiftClick()
                || event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
        }

        // Handle pagination navigation
        handleNavigation(event, player, holder);
    }

    private void handleNavigation(InventoryClickEvent event, Player player, SkillInventoryHolder holder) {
        List<String> pageOrder = holder.getPageOrder();
        if (pageOrder == null) return;

        int slot = event.getSlot();
        int pageIndex = holder.getPageIndex();
        int pageCount = holder.getPageCount();

        int targetPage = -1;
        if (slot == SLOT_PREV && pageIndex > 0) {
            targetPage = pageIndex - 1;
        } else if (slot == SLOT_NEXT && pageIndex < pageCount - 1) {
            targetPage = pageIndex + 1;
        }

        if (targetPage < 0) return;

        PlayerProfile profile = Skilling.getInstance().getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        Map<Integer, Inventory> cached = profile.getCachedPageInventories();
        if (cached == null) return;

        Inventory targetInventory = cached.get(targetPage);
        if (targetInventory == null) return;

        player.openInventory(targetInventory);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;

        if (top.getHolder() instanceof SkillInventoryHolder) {
            event.setCancelled(true);
        }
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
