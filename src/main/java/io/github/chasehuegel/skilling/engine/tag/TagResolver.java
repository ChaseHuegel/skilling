package io.github.chasehuegel.skilling.engine.tag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import java.util.EnumSet;

/**
 * Resolves tag strings to {@link EnumSet EnumSets} of {@link Material}.
 *
 * <p>Supports three formats:
 * <ul>
 *   <li>{@code #minecraft:<tag>} — resolved via Bukkit's {@link Tag} API</li>
 *   <li>{@code #c:<tag>} — resolved via the custom tag definitions loaded by {@link CustomTagLoader}</li>
 *   <li>{@code minecraft:<item>} — resolved as a single material</li>
 * </ul>
 *
 * <p>All resolution is performed at plugin load and flattened into {@link EnumSet}
 * for O(1) membership checks during gameplay.
 */
public final class TagResolver {

    private final CustomTagLoader customTagLoader;

    /**
     * Constructs a tag resolver backed by the given custom tag loader.
     *
     * @param customTagLoader the loader providing custom tag definitions
     */
    public TagResolver(CustomTagLoader customTagLoader) {
        this.customTagLoader = customTagLoader;
    }

    /**
     * Resolves a tag string into an {@link EnumSet} of materials.
     *
     * @param tagString the tag string (e.g., {@code #minecraft:logs}, {@code #c:ores}, {@code minecraft:stone})
     * @return the resolved set of materials (empty set if unresolvable)
     * @throws IllegalArgumentException if the format is invalid
     */
    public EnumSet<Material> resolve(String tagString) {
        if (tagString == null || tagString.isBlank()) {
            return EnumSet.noneOf(Material.class);
        }

        if (!tagString.startsWith("#")) {
            // Single material reference
            Material material = Material.matchMaterial(tagString);
            if (material == null) {
                throw new IllegalArgumentException("Unknown material: " + tagString);
            }
            return EnumSet.of(material);
        }

        // Tag reference
        String namespace = tagString.substring(1);
        int colonIndex = namespace.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Invalid tag format (missing namespace): " + tagString);
        }

        String namespacePrefix = namespace.substring(0, colonIndex);
        String key = namespace.substring(colonIndex + 1);

        if ("minecraft".equals(namespacePrefix)) {
            return resolveVanillaTag(key);
        } else if ("c".equals(namespacePrefix)) {
            return customTagLoader.resolve("#c:" + key);
        }

        throw new IllegalArgumentException("Unknown tag namespace: " + namespacePrefix);
    }

    private static EnumSet<Material> resolveVanillaTag(String key) {
        NamespacedKey nsKey = NamespacedKey.minecraft(key);
        // Try block registry first, then item registry
        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, nsKey, Material.class);
        if (tag == null) {
            tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, nsKey, Material.class);
        }
        if (tag == null) {
            throw new IllegalArgumentException("Unknown vanilla tag: #minecraft:" + key);
        }
        EnumSet<Material> result = EnumSet.noneOf(Material.class);
        result.addAll(tag.getValues());
        return result;
    }
}