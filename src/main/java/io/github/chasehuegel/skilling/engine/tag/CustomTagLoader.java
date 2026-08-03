package io.github.chasehuegel.skilling.engine.tag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
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
 * <p>Two independent stores are parsed:
 * <ul>
 *   <li>{@code custom_tags} — item/block tags resolved to {@link EnumSet} of
 *       {@link Material}, used by the material {@link TagResolver}.</li>
 *   <li>{@code entity_tags} — entity-type tags resolved to {@link EnumSet} of
 *       {@link EntityType}, used by the {@link EntityTagResolver} for filters
 *       such as {@code target_type}.</li>
 * </ul>
 *
 * <p>Custom tags use the {@code #c:} prefix and are defined as lists of
 * registry names and/or cross-references to vanilla {@code #minecraft:} tags
 * or other custom {@code #c:} tags. All tags are flattened into
 * {@link EnumSet} at load time for O(1) lookups.
 */
public final class CustomTagLoader {

    private final Map<String, EnumSet<Material>> customTags = new HashMap<>();
    private final Map<String, EnumSet<EntityType>> customEntityTags = new HashMap<>();

    /**
     * Loads custom tags from the given YAML file.
     *
     * @param file the tags.yml file
     * @throws IllegalArgumentException if the file or a tag definition is malformed
     */
    public void load(File file) {
        customTags.clear();
        customEntityTags.clear();
        if (!file.exists() || !file.isFile()) return;

        try (var reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            loadMaterialSection(config);
            loadEntitySection(config);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read tags.yml: " + file, e);
        }
    }

    private void loadMaterialSection(YamlConfiguration config) {
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
    }

    private void loadEntitySection(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("entity_tags");
        if (section == null) return;

        Map<String, List<String>> rawEntries = new HashMap<>();
        for (String key : section.getKeys(false)) {
            rawEntries.put(key, section.getStringList(key));
        }

        Set<String> resolving = new HashSet<>();
        for (String key : rawEntries.keySet()) {
            resolveEntity(key, rawEntries, resolving);
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

    /**
     * Resolves an {@code #c:} entity tag to its flattened set of entity types.
     *
     * @param key the fully-prefixed key (e.g. {@code #c:undead})
     * @return the resolved set, or an empty set when the tag is undefined
     */
    public EnumSet<EntityType> resolveEntity(String key) {
        return customEntityTags.getOrDefault(key, EnumSet.noneOf(EntityType.class));
    }

    /**
     * Returns the {@code #c:} entity tag keys known to this loader.
     *
     * @return the entity tag keys
     */
    public java.util.Set<String> getEntityKeys() {
        return customEntityTags.keySet();
    }

    public void clear() {
        customTags.clear();
        customEntityTags.clear();
    }

    private EnumSet<EntityType> resolveEntity(String key,
            Map<String, List<String>> rawEntries, Set<String> resolving) {
        String fullKey = "#c:" + key;
        if (customEntityTags.containsKey(fullKey)) return customEntityTags.get(fullKey);
        if (!rawEntries.containsKey(key)) return EnumSet.noneOf(EntityType.class);
        if (!resolving.add(key)) {
            Bukkit.getLogger().warning("Circular entity tag reference detected: #c:" + key);
            return EnumSet.noneOf(EntityType.class);
        }

        EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);
        for (String entry : rawEntries.get(key)) {
            resolveEntityEntry(entry, types, rawEntries, resolving);
        }
        resolving.remove(key);
        customEntityTags.put(fullKey, types);
        return types;
    }

    private void resolveEntityEntry(String entry, EnumSet<EntityType> target,
            Map<String, List<String>> rawEntries, Set<String> resolving) {
        if (entry.startsWith("#")) {
            String tagKey = entry.substring(1);
            String nsKey = tagKey.contains(":") ? tagKey.substring(0, tagKey.indexOf(':')) : "";
            if ("c".equals(nsKey) && rawEntries.containsKey(tagKey.substring(2))) {
                target.addAll(resolveEntity(tagKey.substring(2), rawEntries, resolving));
            } else {
                Tag<EntityType> tag = loadVanillaEntityTag(tagKey);
                if (tag == null) {
                    throw new IllegalArgumentException("Unknown entity tag in custom tag definition: " + entry);
                }
                target.addAll(tag.getValues());
            }
        } else {
            String name = entry.contains(":") ? entry.substring(entry.indexOf(':') + 1) : entry;
            EntityType type = EntityType.fromName(name);
            if (type == null) {
                throw new IllegalArgumentException("Unknown entity type in custom tag definition: " + entry);
            }
            target.add(type);
        }
    }

    private static Tag<EntityType> loadVanillaEntityTag(String key) {
        NamespacedKey nsKey = NamespacedKey.fromString(key);
        if (nsKey == null) return null;
        return Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, nsKey, EntityType.class);
    }
}