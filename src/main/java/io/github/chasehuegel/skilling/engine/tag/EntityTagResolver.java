package io.github.chasehuegel.skilling.engine.tag;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves tag strings to {@link EnumSet EnumSets} of {@link EntityType}.
 *
 * <p>Complements the material {@link TagResolver} for entity-type filters such
 * as {@code target_type}. Supports three formats:
 * <ul>
 *   <li>{@code #minecraft:<tag>} — resolved via Bukkit's {@link Tag} API
 *       (entity registry)</li>
 *   <li>{@code #c:<tag>} — resolved via the custom {@code entity_tags} section
 *       loaded by {@link CustomTagLoader}</li>
 *   <li>{@code minecraft:<entity>} — resolved as a single entity type</li>
 * </ul>
 *
 * <p>Every resolved reference is cached so vanilla entity-tag lookups and
 * registry name parsing happen once per server run. Returned sets are owned by
 * the resolver and must be treated as read-only.
 */
public final class EntityTagResolver {

    private final CustomTagLoader customTagLoader;
    private final Map<String, EnumSet<EntityType>> resolvedCache = new ConcurrentHashMap<>();
    private final Map<String, EntityType> entityCache = new ConcurrentHashMap<>();

    /**
     * Constructs a resolver backed by the given custom tag loader.
     *
     * @param customTagLoader the loader providing custom entity tag definitions
     */
    public EntityTagResolver(CustomTagLoader customTagLoader) {
        this.customTagLoader = customTagLoader;
    }

    /**
     * Cached single-entity-type resolution. Parses the registry name once per
     * unique name and reuses the result.
     *
     * @param name the entity type name (e.g. {@code minecraft:zombie})
     * @return the entity type, or null if the name is unknown
     */
    public EntityType entity(String name) {
        if (name == null || name.isBlank()) return null;
        return entityCache.computeIfAbsent(name, n -> {
            String key = n.contains(":") ? n.substring(n.indexOf(':') + 1) : n;
            return EntityType.fromName(key);
        });
    }

    /**
     * Resolves a tag string into an {@link EnumSet} of entity types.
     *
     * @param tagString the tag string (e.g., {@code #minecraft:zombies},
     *                  {@code #c:undead}, {@code minecraft:zombie})
     * @return the resolved set of entity types (empty set if unresolvable)
     * @throws IllegalArgumentException if the format is invalid
     */
    public EnumSet<EntityType> resolve(String tagString) {
        if (tagString == null || tagString.isBlank()) {
            return EnumSet.noneOf(EntityType.class);
        }

        EnumSet<EntityType> cached = resolvedCache.get(tagString);
        if (cached != null) return cached;

        EnumSet<EntityType> resolved = resolveUncached(tagString);
        resolvedCache.put(tagString, resolved);
        return resolved;
    }

    private EnumSet<EntityType> resolveUncached(String tagString) {
        if (!tagString.startsWith("#")) {
            EntityType type = entity(tagString);
            if (type == null) {
                throw new IllegalArgumentException("Unknown entity type: " + tagString);
            }
            return EnumSet.of(type);
        }

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
            EnumSet<EntityType> custom = customTagLoader.resolveEntity("#c:" + key);
            return custom.isEmpty() ? custom : EnumSet.copyOf(custom);
        }

        throw new IllegalArgumentException("Unknown tag namespace: " + namespacePrefix);
    }

    private static EnumSet<EntityType> resolveVanillaTag(String key) {
        NamespacedKey nsKey = NamespacedKey.minecraft(key);
        try {
            Tag<EntityType> tag = Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, nsKey, EntityType.class);
            if (tag == null) {
                throw new IllegalArgumentException("Unknown vanilla entity tag: #minecraft:" + key);
            }
            // EnumSet.copyOf throws on an empty collection; addAll handles it.
            EnumSet<EntityType> result = EnumSet.noneOf(EntityType.class);
            result.addAll(tag.getValues());
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            // Bukkit is unavailable (e.g. unit tests without a server). Defer
            // resolution rather than fail; the real server populates the cache at load.
            return EnumSet.noneOf(EntityType.class);
        }
    }
}
