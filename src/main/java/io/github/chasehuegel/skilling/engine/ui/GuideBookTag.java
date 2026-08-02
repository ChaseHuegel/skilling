package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Distinct UI tag for the Skills Guide book.
 *
 * <p>The book is a legitimately held item, so it must never carry the
 * {@link PoisonPillTag} used by the vaporization net on menu items; this
 * tag keeps it safe from that net while still marking the item.
 */
public final class GuideBookTag {

    private static volatile NamespacedKey key;

    private GuideBookTag() {}

    private static NamespacedKey getKey() {
        if (key == null) {
            key = new NamespacedKey(Skilling.getInstance(), "guide_book");
        }
        return key;
    }

    public static void apply(ItemMeta meta) {
        meta.getPersistentDataContainer().set(getKey(), PersistentDataType.BYTE, (byte) 1);
    }

    public static boolean isTagged(ItemMeta meta) {
        return meta != null && meta.getPersistentDataContainer().has(getKey(), PersistentDataType.BYTE);
    }
}
