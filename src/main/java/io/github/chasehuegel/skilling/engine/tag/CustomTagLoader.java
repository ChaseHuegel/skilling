package io.github.chasehuegel.skilling.engine.tag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads and resolves custom tag definitions from a {@code tags} data folder.
 *
 * <p>The folder is scanned recursively for {@code .yml} files (e.g.
 * {@code tags/base.yml}, {@code tags/custom.yml}), each contributing raw
 * definitions that are merged additively: when a tag key repeats across files
 * its entry lists are appended, never overwritten. All raw entries are resolved
 * in one global pass so cross-file {@code #c:} references work.
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
    private volatile boolean loaded;

    /**
     * Loads custom tags from a single YAML file. Malformed content fails fast.
     *
     * @param file the tags file
     * @throws IllegalArgumentException if the file or a tag definition is malformed
     */
    public void load(File file) {
        customTags.clear();
        customEntityTags.clear();
        if (!file.exists() || !file.isFile()) {
            // A definitive (empty) store: a #c: reference is genuinely unknown.
            loaded = true;
            return;
        }

        Map<String, List<String>> rawEntries = new HashMap<>();
        Map<String, List<String>> rawEntityEntries = new HashMap<>();
        try {
            gather(file, rawEntries, rawEntityEntries);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read tags file: " + file, e);
        }
        resolveAll(rawEntries, rawEntityEntries);
        loaded = true;
    }

    /**
     * Loads every {@code .yml} file in the given directory (recursively), merging
     * duplicate tag keys additively and resolving all tags in one pass so
     * cross-file {@code #c:} references resolve. A file that cannot be read as
     * tags logs a warning and is skipped; it never fails the load. A missing
     * directory is treated as an empty store.
     *
     * @param tagsDir the tags data directory
     */
    public void loadDirectory(File tagsDir) {
        customTags.clear();
        customEntityTags.clear();
        if (!tagsDir.exists() || !tagsDir.isDirectory()) {
            // A definitive (empty) store: a #c: reference is genuinely unknown.
            loaded = true;
            return;
        }

        Map<String, List<String>> rawEntries = new HashMap<>();
        Map<String, List<String>> rawEntityEntries = new HashMap<>();
        for (File file : collectYamlFiles(tagsDir)) {
            try {
                gather(file, rawEntries, rawEntityEntries);
            } catch (IOException | IllegalArgumentException e) {
                Bukkit.getLogger().warning(
                        "Skipping malformed tags file " + file.getName() + ": " + e.getMessage());
            }
        }
        resolveAll(rawEntries, rawEntityEntries);
        loaded = true;
    }

    /**
     * Whether a load attempt has completed for this loader. A loader that never
     * ran (e.g. a unit-test resolver without a tags store) defers existence
     * checks; a loaded store is authoritative about which {@code #c:} keys exist.
     *
     * @return true once a load attempt has completed
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Parses a single tags file into the shared raw-entry maps, validating each
     * entry so an unresolvable definition (unknown material, entity, or vanilla
     * tag) is attributed to this file. A {@code #c:} reference is deferred to the
     * global resolve pass because its target may live in another file.
     *
     * @param file             the tags file to parse
     * @param rawEntries       the accumulated {@code custom_tags} entries (additively merged)
     * @param rawEntityEntries the accumulated {@code entity_tags} entries (additively merged)
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the file is not a tags map or holds an invalid entry
     */
    private void gather(File file, Map<String, List<String>> rawEntries,
            Map<String, List<String>> rawEntityEntries) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        YamlConfiguration config;
        try {
            Object topLevel = new Yaml().load(content);
            if (topLevel != null && !(topLevel instanceof Map<?, ?>)) {
                throw new IllegalArgumentException(
                        "top-level YAML is not a map, got " + topLevel.getClass().getSimpleName());
            }
            config = YamlConfiguration.loadConfiguration(new StringReader(content));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to parse tags file: " + file, e);
        }
        gatherSection(config.getConfigurationSection("custom_tags"), rawEntries,
                CustomTagLoader::validateMaterialEntry);
        gatherSection(config.getConfigurationSection("entity_tags"), rawEntityEntries,
                CustomTagLoader::validateEntityEntry);
    }

    private static void gatherSection(ConfigurationSection section,
            Map<String, List<String>> rawEntries, java.util.function.Consumer<String> validator) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            List<String> entries = requireList(section, key);
            for (String entry : entries) {
                validator.accept(entry);
            }
            rawEntries.merge(key, entries, CustomTagLoader::append);
        }
    }

    private static List<String> append(List<String> existing, List<String> addition) {
        List<String> merged = new ArrayList<>(existing);
        merged.addAll(addition);
        return merged;
    }

    private static void validateMaterialEntry(String entry) {
        if (entry.startsWith("#")) {
            String tagKey = entry.substring(1);
            String nsKey = tagKey.contains(":") ? tagKey.substring(0, tagKey.indexOf(':')) : "";
            if (!"c".equals(nsKey) && loadVanillaTag(tagKey) == null) {
                throw new IllegalArgumentException("Unknown tag in custom tag definition: " + entry);
            }
        } else if (Material.matchMaterial(entry) == null) {
            throw new IllegalArgumentException("Unknown material in custom tag definition: " + entry);
        }
    }

    private static void validateEntityEntry(String entry) {
        if (entry.startsWith("#")) {
            String tagKey = entry.substring(1);
            String nsKey = tagKey.contains(":") ? tagKey.substring(0, tagKey.indexOf(':')) : "";
            if (!"c".equals(nsKey) && loadVanillaEntityTag(tagKey) == null) {
                throw new IllegalArgumentException("Unknown entity tag in custom tag definition: " + entry);
            }
        } else {
            String name = entry.contains(":") ? entry.substring(entry.indexOf(':') + 1) : entry;
            if (EntityType.fromName(name) == null) {
                throw new IllegalArgumentException("Unknown entity type in custom tag definition: " + entry);
            }
        }
    }

    private static List<File> collectYamlFiles(File dir) {
        List<File> files = new ArrayList<>();
        try (var stream = Files.walk(dir.toPath())) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml"))
                    .sorted(Comparator.comparing(p -> dir.toPath().relativize(p).toString()))
                    .forEach(p -> files.add(p.toFile()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to walk tags directory: " + dir, e);
        }
        return files;
    }

    /**
     * Resolves every gathered raw entry in one global pass. A tag key that fails
     * resolution (e.g. a dangling {@code #c:} reference) is skipped with a
     * warning instead of failing the load.
     *
     * @param rawEntries       the merged {@code custom_tags} entries
     * @param rawEntityEntries the merged {@code entity_tags} entries
     */
    private void resolveAll(Map<String, List<String>> rawEntries,
            Map<String, List<String>> rawEntityEntries) {
        Set<String> resolving = new HashSet<>();
        for (String key : rawEntries.keySet()) {
            try {
                resolve(key, rawEntries, resolving);
            } catch (IllegalArgumentException e) {
                customTags.remove("#c:" + key);
                Bukkit.getLogger().warning("Skipping custom tag '#c:" + key + "': " + e.getMessage());
            }
        }
        resolving.clear();
        for (String key : rawEntityEntries.keySet()) {
            try {
                resolveEntity(key, rawEntityEntries, resolving);
            } catch (IllegalArgumentException e) {
                customEntityTags.remove("#c:" + key);
                Bukkit.getLogger().warning(
                        "Skipping custom entity tag '#c:" + key + "': " + e.getMessage());
            }
        }
    }

    /**
     * Reads a tag value as a list of entries, rejecting a scalar or map value
     * (which would otherwise silently resolve to an empty tag).
     *
     * @param section the custom-tags section
     * @param key     the tag key
     * @return the list of entries
     * @throws IllegalArgumentException if the value is not a list
     */
    private static List<String> requireList(ConfigurationSection section, String key) {
        Object value = section.get(key);
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException(
                    "Custom tag '" + key + "' must be a list of entries, got: " + value);
        }
        return section.getStringList(key);
    }

    private EnumSet<Material> resolve(String key, Map<String, List<String>> rawEntries, Set<String> resolving) {
        String fullKey = "#c:" + key;
        if (customTags.containsKey(fullKey)) return customTags.get(fullKey);
        if (!rawEntries.containsKey(key)) return EnumSet.noneOf(Material.class);
        if (!resolving.add(key)) {
            Bukkit.getLogger().warning("Circular tag reference detected: #c:" + key);
            return EnumSet.noneOf(Material.class);
        }

        try {
            EnumSet<Material> materials = EnumSet.noneOf(Material.class);
            for (String entry : rawEntries.get(key)) {
                resolveEntry(entry, materials, rawEntries, resolving);
            }
            customTags.put(fullKey, materials);
            return materials;
        } finally {
            // Always unwind so a skipped key cannot leave the resolving set
            // poisoned for later keys that reference it.
            resolving.remove(key);
        }
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

        try {
            EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);
            for (String entry : rawEntries.get(key)) {
                resolveEntityEntry(entry, types, rawEntries, resolving);
            }
            customEntityTags.put(fullKey, types);
            return types;
        } finally {
            // Always unwind so a skipped key cannot leave the resolving set
            // poisoned for later keys that reference it.
            resolving.remove(key);
        }
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