package io.github.chasehuegel.skilling.engine.tag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads and resolves custom tag definitions from {@code tags.yml}.
 *
 * <p>Custom tags use the {@code #c:} prefix and are defined as lists of
 * material names and/or cross-references to vanilla {@code #minecraft:} tags
 * or other custom {@code #c:} tags.  All tags are flattened into
 * {@link EnumSet} at load time for O(1) lookups.
 */
public final class CustomTagLoader {

    private final Map<String, EnumSet<Material>> customTags = new HashMap<>();

    /**
     * Loads custom tags from the given YAML file.
     *
     * @param file the tags.yml file
     * @throws IllegalArgumentException if the file or a tag definition is malformed
     */
    public void load(File file) {
        customTags.clear();
        if (!file.exists() || !file.isFile()) return;

        try (var reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            ConfigurationSection section = config.getConfigurationSection("custom_tags");
            if (section == null) return;

            Map<String, List<String>> rawEntries = new HashMap<>();
            for (String key : section.getKeys(false)) {
                rawEntries.put(key, section.getStringList(key));
            }

            Set<String> resolving = new HashSet<>();
            for (String key : rawEntries.keySet()) {
                resolve(key, rawEntries, resolving);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read tags.yml: " + file, e);
        }
    }

    private EnumSet<Material> resolve(String key, Map<String, List<String>> rawEntries, Set<String> resolving) {
        String fullKey = "#c:" + key;
        if (customTags.containsKey(fullKey)) return customTags.get(fullKey);
        if (!rawEntries.containsKey(key)) return EnumSet.noneOf(Material.class);
        if (!resolving.add(key)) {
            Bukkit.getLogger().warning("Circular tag reference detected: #c:" + key);
            return EnumSet.noneOf(Material.class);
        }

        EnumSet<Material> materials = EnumSet.noneOf(Material.class);
        for (String entry : rawEntries.get(key)) {
            resolveEntry(entry, materials, rawEntries, resolving);
        }
        resolving.remove(key);
        customTags.put(fullKey, materials);
        return materials;
    }

    private void resolveEntry(String entry, EnumSet<Material> target,
                               Map<String, List<String>> rawEntries, Set<String> resolving) {
        if (entry.startsWith("#")) {
            String tagKey = entry.substring(1);
            String nsKey = tagKey.contains(":") ? tagKey.substring(0, tagKey.indexOf(':')) : "";
            if ("c".equals(nsKey) && rawEntries.containsKey(tagKey.substring(2))) {
                target.addAll(resolve(tagKey.substring(2), rawEntries, resolving));
            } else {
                Tag<Material> tag = loadVanillaTag(tagKey);
                if (tag == null) {
                    throw new IllegalArgumentException("Unknown tag in custom tag definition: " + entry);
                }
                target.addAll(tag.getValues());
            }
        } else {
            Material material = Material.matchMaterial(entry);
            if (material == null) {
                throw new IllegalArgumentException("Unknown material in custom tag definition: " + entry);
            }
            target.add(material);
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

    public EnumSet<Material> resolve(String key) {
        return customTags.getOrDefault(key, EnumSet.noneOf(Material.class));
    }

    public java.util.Set<String> getKeys() {
        return customTags.keySet();
    }

    public void clear() {
        customTags.clear();
    }
}