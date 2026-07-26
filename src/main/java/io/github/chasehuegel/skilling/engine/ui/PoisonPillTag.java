package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Constants and utilities for the anti-dupe poison pill tagging system.
 *
 * <p>Every UI {@link ItemStack} is tagged with a hidden byte via Paper's
 * {@link org.bukkit.persistence.PersistentDataContainer}. The
 * {@link UIProtectionListener} intercepts any tagged item found outside
 * a controlled UI context and removes it.
 *
 * <p>The key is namespaced under {@code skilling:ui_item}.
 */
public final class PoisonPillTag {

    /** The namespaced key used to tag UI items. */
    public static final NamespacedKey KEY = new NamespacedKey(Skilling.getInstance(), "ui_item");

    private PoisonPillTag() {}

    /**
     * Applies the poison pill tag to an item's metadata.
     *
     * @param meta the item meta to tag
     */
    public static void apply(ItemMeta meta) {
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * Checks whether an item's metadata has the poison pill tag.
     *
     * @param meta the item meta to check
     * @return true if the item is a tagged UI item
     */
    public static boolean isTagged(ItemMeta meta) {
        return meta != null && meta.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }
}