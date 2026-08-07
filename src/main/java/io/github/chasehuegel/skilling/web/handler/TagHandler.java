package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
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

    public void get(Context ctx) {
        try {
            Map<String, List<String>> tags = new LinkedHashMap<>();
            Map<String, List<String>> entityTags = new LinkedHashMap<>();
            if (tagsFile.exists()) {
                String content = Files.readString(tagsFile.toPath(), StandardCharsets.UTF_8);
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
            // Preserve the read-only entity_tags section so a GUI save does not
            // silently delete it: EntityTagResolver reads it to resolve the
            // target_type state filter, and skills may reference e.g.
            // state: "target_type:#c:undead".
            Object entityTags = loadEntityTags();
            if (entityTags != null) {
                root.put("entity_tags", entityTags);
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
