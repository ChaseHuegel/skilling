package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class TagHandler {

    private static final Logger LOGGER = Logger.getLogger(TagHandler.class.getName());

    private final StagingManager stagingManager;
    private final File tagsFile;

    public TagHandler(StagingManager stagingManager, File tagsFile) {
        this.stagingManager = stagingManager;
        this.tagsFile = tagsFile;
    }

    /**
     * The tags file to read: the staged file when an edit is pending, otherwise
     * the live file, so a second save before reload reflects the pending edit.
     *
     * @return the staged tags/base.yml if it exists, else the live one
     */
    private File sourceTagsFile() {
        File staged = stagingManager.stagedTagsFile();
        return staged != null && staged.exists() ? staged : tagsFile;
    }

    public void get(Context ctx) {
        try {
            Map<String, List<String>> tags = new LinkedHashMap<>();
            Map<String, List<String>> entityTags = new LinkedHashMap<>();
            File source = sourceTagsFile();
            if (source.exists()) {
                String content = Files.readString(source.toPath(), StandardCharsets.UTF_8);
                var yaml = new org.yaml.snakeyaml.Yaml();
                Map<String, Object> raw = yaml.load(content);
                if (raw != null) {
                    Map<String, Object> customTags = (Map<String, Object>) raw.get("custom_tags");
                    if (customTags != null) {
                        for (var entry : customTags.entrySet()) {
                            tags.put("#c:" + entry.getKey(), (List<String>) entry.getValue());
                        }
                    }
                    Map<String, Object> rawEntityTags = (Map<String, Object>) raw.get("entity_tags");
                    if (rawEntityTags != null) {
                        for (var entry : rawEntityTags.entrySet()) {
                            entityTags.put("#c:" + entry.getKey(), (List<String>) entry.getValue());
                        }
                    }
                }
            }
            ctx.json(Map.of("tags", tags, "entityTags", entityTags));
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to read tags/base.yml", e);
        }
    }

    public void update(Context ctx) {
        try {
            Map<String, Object> body = WebError.parseBody(ctx, Map.class);
            if (body == null) {
                WebError.badRequest(ctx, "Request body is required");
                return;
            }
            Object tagsObj = body.get("tags");
            if (!(tagsObj instanceof Map<?, ?> tagsRaw)) {
                WebError.badRequest(ctx, "body must contain a 'tags' object");
                return;
            }

            Map<String, List<String>> customTags = new LinkedHashMap<>();
            for (var entry : tagsRaw.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                if (!(value instanceof List<?> rawList)) {
                    WebError.badRequest(ctx, "tag '" + key + "' must be a list of strings");
                    return;
                }
                List<String> materials = new ArrayList<>();
                for (Object item : rawList) {
                    if (item != null) materials.add(item.toString());
                }
                if (key.startsWith("#c:")) {
                    customTags.put(key.substring(3), materials);
                }
            }

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("custom_tags", customTags);

            // entityTags is optional: when present it is validated and written
            // explicitly; when absent the live entity_tags section is preserved so
            // a material-only save never silently deletes it (EntityTagResolver
            // reads it to resolve the target_type state filter).
            Object entityTagsRaw = body.get("entityTags");
            if (entityTagsRaw instanceof Map<?, ?> entityTagsMap) {
                Map<String, List<String>> entityTags = new LinkedHashMap<>();
                for (var entry : entityTagsMap.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    Object value = entry.getValue();
                    if (!(value instanceof List<?> rawList)) {
                        WebError.badRequest(ctx, "entity tag '" + key + "' must be a list of strings");
                        return;
                    }
                    List<String> refs = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item != null) refs.add(item.toString());
                    }
                    for (String ref : refs) {
                        if (!isKnownEntityReference(ref)) {
                            WebError.badRequest(ctx, "entity tag '" + key + "' has unknown value '" + ref + "'");
                            return;
                        }
                    }
                    if (key.startsWith("#c:")) {
                        entityTags.put(key.substring(3), refs);
                    }
                }
                root.put("entity_tags", entityTags);
            } else {
                Object entityTags = loadEntityTags();
                if (entityTags != null) {
                    root.put("entity_tags", entityTags);
                }
            }
            var yaml = new org.yaml.snakeyaml.Yaml();
            String yamlContent = yaml.dump(root);

            stagingManager.stageTagsFile(yamlContent);
            ctx.json(Map.of("status", "ok"));
        } catch (JsonProcessingException e) {
            WebError.malformedJson(ctx);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to stage tags/base.yml", e);
        }
    }

    /**
     * Whether an entity-tag value is a valid reference, matching the engine's
     * {@code CustomTagLoader.validateEntityEntry} semantics: a {@code #c:}
     * cross-reference is always accepted (its target is resolved later), a
     * {@code #minecraft:} entity tag must exist in the vanilla registry, and a
     * bare entity type name must resolve via {@link EntityType#fromName}. When
     * the vanilla registry is unavailable (e.g. a unit test without a server),
     * tag existence checks are deferred so validation never rejects on
     * infrastructure the loader cannot see.
     *
     * @param ref the entity-tag value
     * @return true if the reference is known or cannot be verified
     */
    private static boolean isKnownEntityReference(String ref) {
        if (ref.startsWith("#")) {
            String tagKey = ref.substring(1);
            String namespace = tagKey.contains(":") ? tagKey.substring(0, tagKey.indexOf(':')) : "";
            if ("c".equals(namespace)) return true;
            if ("minecraft".equals(namespace)) {
                NamespacedKey nsKey = NamespacedKey.fromString(tagKey);
                if (nsKey == null) return false;
                try {
                    return Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, nsKey, EntityType.class) != null;
                } catch (RuntimeException e) {
                    return true; // Bukkit unavailable; defer
                }
            }
            return false;
        }
        String name = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
        return EntityType.fromName(name) != null;
    }

    /**
     * Loads the {@code entity_tags} section from the live tags/base.yml, or null
     * when the file is absent or has no such section. An unreadable file is
     * treated as having nothing to preserve rather than failing the stage.
     *
     * @return the raw entity_tags value, or null
     */
    private Object loadEntityTags() {
        if (!tagsFile.exists()) return null;
        try {
            String content = Files.readString(tagsFile.toPath(), StandardCharsets.UTF_8);
            Object loaded = new org.yaml.snakeyaml.Yaml().load(content);
            if (loaded instanceof Map<?, ?> existing) {
                return existing.get("entity_tags");
            }
        } catch (IOException e) {
            LOGGER.warning("Could not read tags/base.yml to preserve entity_tags: " + e.getMessage());
        }
        return null;
    }
}
