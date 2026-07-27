package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TagHandler {

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
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    public void update(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            Map<String, List<String>> incomingTags = (Map<String, List<String>>) body.get("tags");

            Map<String, List<String>> customTags = new LinkedHashMap<>();
            for (var entry : incomingTags.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("#c:")) {
                    customTags.put(key.substring(3), entry.getValue());
                }
            }

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("custom_tags", customTags);
            var yaml = new org.yaml.snakeyaml.Yaml();
            String yamlContent = yaml.dump(root);

            stagingManager.stageTagsFile(yamlContent);
            ctx.json(Map.of("status", "ok"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
