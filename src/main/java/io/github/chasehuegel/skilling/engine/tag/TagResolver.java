package io.github.chasehuegel.skilling.engine.tag;

import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
 * <p>Every resolved reference is cached so vanilla tag lookups
 * ({@code Bukkit.getTag} + {@code getValues()}) and material name parsing happen
 * once per server run. {@link SkillManager} pre-warms the cache for every
 * filter/requirement reference at plugin load, so event dispatch and item
 * requirement checks are O(1) {@link EnumSet#contains} lookups with zero
 * per-event resolution.
 *
 * <p>Returned sets are owned by the resolver and must be treated as read-only.
 */
public final class TagResolver {

    private final CustomTagLoader customTagLoader;
    private final Map<String, EnumSet<Material>> resolvedCache = new ConcurrentHashMap<>();
    private final Map<String, Material> materialCache = new ConcurrentHashMap<>();
    private final AtomicLong resolutionCount = new AtomicLong();

    /**
     * Constructs a tag resolver backed by the given custom tag loader.
     *
     * @param customTagLoader the loader providing custom tag definitions
     */
    public TagResolver(CustomTagLoader customTagLoader) {
        this.customTagLoader = customTagLoader;
    }

    /**
     * Cached single-material resolution. Parses the material name once per unique
     * name and reuses the result instead of calling {@link Material#matchMaterial}
     * on every reference.
     *
     * @param name the material name (e.g. {@code minecraft:coal})
     * @return the material, or null if the name is unknown
     */
    public Material material(String name) {
        if (name == null || name.isBlank()) return null;
        return materialCache.computeIfAbsent(name, Material::matchMaterial);
    }

    /**
     * Pre-warms the resolution cache for a reference. Called at plugin load for
     * every filter/requirement reference so no tag or material resolution work
     * happens on the event path.
     *
     * @param reference the reference to resolve and cache (e.g. {@code #c:ores})
     */
    public void warm(String reference) {
        if (reference == null || reference.isBlank()) return;
        resolve(reference);
    }

    /**
     * Number of uncached resolution attempts performed by this resolver. Used by
     * tests to assert that event dispatch performs no re-resolution after load.
     *
     * @return the resolution attempt count
     */
    public long resolutionCount() {
        return resolutionCount.get();
    }

    /**
     * Whether a filter/requirement reference is known: a real material, a defined
     * custom tag, or a resolvable vanilla tag.
     *
     * <p>Used for fail-fast validation at load. When the custom tag store is empty
     * or Bukkit is unavailable (e.g. unit-test context) existence checks are
     * deferred so validation never rejects on infrastructure the loader cannot see.
     *
     * @param reference the reference string (e.g. {@code #c:ores}, {@code minecraft:stone})
     * @return true if the reference is known or cannot be verified
     */
    public boolean isKnown(String reference) {
        if (reference == null || reference.isBlank()) return true;
        if (!reference.startsWith("#")) {
            return material(reference) != null;
        }
        String namespace = reference.substring(1);
        int colonIndex = namespace.indexOf(':');
        if (colonIndex == -1) return false;
        String prefix = namespace.substring(0, colonIndex);
        String key = namespace.substring(colonIndex + 1);
        if ("c".equals(prefix)) {
            // Defer only when the loader never ran (e.g. a unit-test resolver
            // without a tags store); a loaded store is authoritative, so an
            // undefined #c: key is rejected at load instead of silently never
            // matching at runtime.
            if (!customTagLoader.isLoaded()) return true;
            return customTagLoader.getKeys().contains("#c:" + key);
        }
        if ("minecraft".equals(prefix)) {
            try {
                NamespacedKey nsKey = NamespacedKey.minecraft(key);
                return Bukkit.getTag(Tag.REGISTRY_BLOCKS, nsKey, Material.class) != null
                        || Bukkit.getTag(Tag.REGISTRY_ITEMS, nsKey, Material.class) != null;
            } catch (RuntimeException e) {
                return true; // Bukkit unavailable; defer
            }
        }
        return false;
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

        EnumSet<Material> cached = resolvedCache.get(tagString);
        if (cached != null) return cached;

        EnumSet<Material> resolved = resolveUncached(tagString);
        resolvedCache.put(tagString, resolved);
        return resolved;
    }

    private EnumSet<Material> resolveUncached(String tagString) {
        resolutionCount.incrementAndGet();

        if (!tagString.startsWith("#")) {
            // Single material reference
            Material material = material(tagString);
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
            EnumSet<Material> custom = customTagLoader.resolve("#c:" + key);
            // Copy so the resolver owns a stable set independent of the loader.
            return custom.isEmpty() ? custom : EnumSet.copyOf(custom);
        }

        throw new IllegalArgumentException("Unknown tag namespace: " + namespacePrefix);
    }

    private static EnumSet<Material> resolveVanillaTag(String key) {
        NamespacedKey nsKey = NamespacedKey.minecraft(key);
        try {
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
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            // Bukkit is unavailable (e.g. unit tests without a server). Defer
            // resolution rather than fail; the real server populates the cache at load.
            return EnumSet.noneOf(Material.class);
        }
    }
}