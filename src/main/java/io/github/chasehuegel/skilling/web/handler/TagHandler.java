package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.io.File;
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
                }
            }
            ctx.json(Map.of("tags", tags));
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to read tags.yml", e);
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
            var yaml = new org.yaml.snakeyaml.Yaml();
            String yamlContent = yaml.dump(root);

            stagingManager.stageTagsFile(yamlContent);
            ctx.json(Map.of("status", "ok"));
        } catch (JsonProcessingException e) {
            WebError.malformedJson(ctx);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to stage tags.yml", e);
        }
    }
}
