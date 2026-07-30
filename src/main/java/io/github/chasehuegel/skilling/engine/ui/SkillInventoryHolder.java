package io.github.chasehuegel.skilling.engine.ui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.List;

/**
 * Inventory holder for Skilling UI inventories.
 *
 * <p>When the GUI is paginated (via {@code gui.yml}), carries the current
 * page index, ordered page IDs, and total page count so the
 * {@link UIProtectionListener} can handle navigation clicks.
 *
 * <p>In legacy flat layout mode, {@code pageIndex} is -1 and
 * {@code pageOrder} is null.
 */
public final class SkillInventoryHolder implements InventoryHolder {

    private final Player player;
    private final int pageIndex;
    private final List<String> pageOrder;
    private final int pageCount;

    /**
     * Creates a holder for the legacy flat layout (non-paginated).
     *
     * @param player the player viewing the inventory
     */
    public SkillInventoryHolder(Player player) {
        this(player, -1, null, 0);
    }

    /**
     * Creates a holder for a paginated page inventory.
     *
     * @param player    the player viewing the inventory
     * @param pageIndex zero-based index of the current page
     * @param pageOrder ordered list of page IDs
     * @param pageCount total number of pages
     */
    public SkillInventoryHolder(Player player, int pageIndex, List<String> pageOrder, int pageCount) {
        this.player = player;
        this.pageIndex = pageIndex;
        this.pageOrder = pageOrder;
        this.pageCount = pageCount;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the current page index, or -1 in legacy flat layout mode.
     *
     * @return the page index
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * Returns the ordered list of page IDs, or null in legacy mode.
     *
     * @return the page order
     */
    public List<String> getPageOrder() {
        return pageOrder;
    }

    /**
     * Returns the total number of pages, or 0 in legacy mode.
     *
     * @return the page count
     */
    public int getPageCount() {
        return pageCount;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
