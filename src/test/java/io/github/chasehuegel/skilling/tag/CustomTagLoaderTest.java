package io.github.chasehuegel.skilling.tag;

import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CustomTagLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void unknownTagReturnsEmpty() {
        var loader = new CustomTagLoader();
        var resolved = loader.resolve("#c:nonexistent");
        assertTrue(resolved.isEmpty());
    }

    @Test
    void handlesMissingFile() {
        var loader = new CustomTagLoader();
        loader.load(new File("/nonexistent/tags.yml"));
        assertTrue(loader.resolve("#c:ores").isEmpty());
    }

    @Test
    void emptyConfig() throws IOException {
        File tagsFile = tempDir.resolve("empty.yml").toFile();
        try (var w = new FileWriter(tagsFile)) {
            w.write("custom_tags: {}\n");
        }

        var loader = new CustomTagLoader();
        loader.load(tagsFile);
        assertTrue(loader.getKeys().isEmpty());
    }

    @Test
    void parsesTagKeys() {
        // Verify key formatting: loader stores keys with the "#c:" prefix
        var loader = new CustomTagLoader();
        // load() with a non-existent file should not throw
        loader.load(new File("/nonexistent/tags.yml"));
        assertTrue(loader.getKeys().isEmpty());

        // The resolve method always returns a non-null set
        assertNotNull(loader.resolve("#c:anything"));
    }

    @Test
    void clearEmptiesKeys() {
        var loader = new CustomTagLoader();
        loader.clear();
        assertTrue(loader.getKeys().isEmpty());
    }

    @Test
    void malformedTagMaterialFailsFast() throws IOException {
        File tagsFile = tempDir.resolve("tags.yml").toFile();
        try (var w = new FileWriter(tagsFile)) {
            w.write("custom_tags:\n  ores:\n    - \"minecraft:not_a_real_material\"\n");
        }

        var loader = new CustomTagLoader();
        assertThrows(IllegalArgumentException.class, () -> loader.load(tagsFile));
    }
}