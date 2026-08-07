package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the tags/base.yml round-trip through the web GUI preserves the
 * read-only {@code entity_tags} section (used by the {@code target_type} state
 * filter) when saving custom tags.
 */
class TagHandlerEntityTagsRoundTripTest {

    @TempDir
    Path tempDir;

    private static final String TAGS_YML = """
            # header comment
            custom_tags:
              ores:
                - "minecraft:iron_ore"
              logs:
                - "#minecraft:logs"
            entity_tags:
              undead:
                - "#minecraft:zombies"
                - "minecraft:stray"
              aquatic:
                - "minecraft:drowned"
            """;

    private Path writeTagsFile() throws IOException {
        Path tagsFile = tempDir.resolve("tags").resolve("base.yml");
        Files.createDirectories(tagsFile.getParent());
        Files.writeString(tagsFile, TAGS_YML);
        return tagsFile;
    }

    @Test
    void updatePreservesEntityTagsInStagedFile() throws IOException {
        Path tagsFile = writeTagsFile();
        StagingManager staging = new StagingManager(tempDir.toFile());

        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
                "tags", Map.of("#c:ores", List.of("minecraft:iron_ore", "minecraft:gold_ore"))));

        new TagHandler(staging, tagsFile.toFile()).update(ctx);

        verify(ctx).json(Map.of("status", "ok"));
        Path staged = tempDir.resolve(".web_staging").resolve("tags").resolve("base.yml");
        assertTrue(Files.exists(staged), "staged tags/base.yml must be written");

        Map<String, Object> raw = new Yaml().load(Files.readString(staged, StandardCharsets.UTF_8));
        assertNotNull(raw.get("custom_tags"), "custom_tags must be present");
        Object entityTags = raw.get("entity_tags");
        assertNotNull(entityTags, "entity_tags must be preserved on save");
        Map<String, Object> entityTagsMap = (Map<String, Object>) entityTags;
        assertNotNull(entityTagsMap.get("undead"), "original entity tag entry must survive");
        assertEquals(List.of("#minecraft:zombies", "minecraft:stray"), entityTagsMap.get("undead"));
        assertNotNull(entityTagsMap.get("aquatic"));
    }

    @Test
    void updateWithoutExistingFileStagesCustomTagsOnly() throws IOException {
        Path tagsFile = tempDir.resolve("tags").resolve("base.yml");
        StagingManager staging = new StagingManager(tempDir.toFile());

        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
                "tags", Map.of("#c:ores", List.of("minecraft:iron_ore"))));

        new TagHandler(staging, tagsFile.toFile()).update(ctx);

        Path staged = tempDir.resolve(".web_staging").resolve("tags").resolve("base.yml");
        Map<String, Object> raw = new Yaml().load(Files.readString(staged, StandardCharsets.UTF_8));
        assertNotNull(raw.get("custom_tags"));
        assertEquals(null, raw.get("entity_tags"), "no entity_tags section when none existed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReturnsEntityTagsReadOnly() throws IOException {
        Path tagsFile = writeTagsFile();
        Context ctx = mock(Context.class, RETURNS_SELF);

        new TagHandler(mock(StagingManager.class), tagsFile.toFile()).get(ctx);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        Map<String, Object> body = (Map<String, Object>) captor.getValue();
        Map<String, Object> entityTags = (Map<String, Object>) body.get("entityTags");
        assertNotNull(entityTags);
        assertEquals(List.of("#minecraft:zombies", "minecraft:stray"), entityTags.get("#c:undead"));
    }
}
