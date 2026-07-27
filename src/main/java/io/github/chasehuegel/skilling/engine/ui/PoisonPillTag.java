package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class PoisonPillTag {

    private static NamespacedKey key;

    private PoisonPillTag() {}

    private static NamespacedKey getKey() {
        if (key == null) {
            key = new NamespacedKey(Skilling.getInstance(), "ui_item");
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
