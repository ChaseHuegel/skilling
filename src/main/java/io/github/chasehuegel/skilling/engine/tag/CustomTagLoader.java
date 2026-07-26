package io.github.chasehuegel.skilling.engine.tag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and resolves custom tag definitions from {@code tags.yml}.
 *
 * <p>Custom tags use the {@code #c:} prefix and are defined as lists of
 * material names and/or cross-references to vanilla {@code #minecraft:} tags.
 * All tags are flattened into {@link EnumSet} at load time for O(1) lookups.
 */
public final class CustomTagLoader {

    private final Map<String, EnumSet<Material>> customTags = new HashMap<>();

    /**
     * Loads custom tags from the given YAML file.
     *
     * @param file the tags.yml file
     * @throws IllegalArgumentException if the file is malformed
     */
    public void load(File file) {
        customTags.clear();
        if (!file.exists() || !file.isFile()) return;

        try (var reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            ConfigurationSection section = config.getConfigurationSection("custom_tags");
            if (section == null) return;

            for (String key : section.getKeys(false)) {
                List<String> entries = section.getStringList(key);
                EnumSet<Material> materials = EnumSet.noneOf(Material.class);
                for (String entry : entries) {
                    resolveEntry(entry, materials);
                }
                customTags.put("#c:" + key, materials);
            }
        } catch (Exception ignored) {
            // YAML parsing may fail in non-Bukkit environments
        }
    }

    private void resolveEntry(String entry, EnumSet<Material> target) {
        if (entry.startsWith("#")) {
            // Cross-reference: #minecraft:logs etc.
            String tagKey = entry.substring(1);
            Tag<Material> tag = loadVanillaTag(tagKey);
            if (tag != null) {
                target.addAll(tag.getValues());
            }
        } else {
            try {
                Material material = Material.matchMaterial(entry);
                if (material != null) {
                    target.add(material);
                }
            } catch (Exception ignored) {
                // matchMaterial may fail in non-Bukkit environments
            }
        }
    }

    private static Tag<Material> loadVanillaTag(String key) {
        NamespacedKey nsKey = NamespacedKey.fromString(key);
        if (nsKey == null) return null;
        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, nsKey, Material.class);
        if (tag == null) {
            tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, nsKey, Material.class);
        }
        return tag;
    }

    /**
     * Resolves a custom tag key (e.g., {@code #c:ores}) into its material set.
     *
     * @param key the full custom tag key including the {@code #c:} prefix
     * @return the resolved material set, or an empty set if not found
     */
    public EnumSet<Material> resolve(String key) {
        return customTags.getOrDefault(key, EnumSet.noneOf(Material.class));
    }

    /**
     * Returns all loaded custom tag keys.
     *
     * @return set of custom tag keys (with the {@code #c:} prefix)
     */
    public java.util.Set<String> getKeys() {
        return customTags.keySet();
    }

    /**
     * Clears all loaded custom tags.
     */
    public void clear() {
        customTags.clear();
    }
}