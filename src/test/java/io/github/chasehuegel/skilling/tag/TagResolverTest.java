package io.github.chasehuegel.skilling.tag;

import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TagResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void nullOrBlankReturnsEmpty() {
        var resolver = new TagResolver(new CustomTagLoader());
        assertTrue(resolver.resolve(null).isEmpty());
        assertTrue(resolver.resolve("").isEmpty());
        assertTrue(resolver.resolve("   ").isEmpty());
    }

    @Test
    void missingCustomTagReturnsEmpty() {
        var resolver = new TagResolver(new CustomTagLoader());
        var result = resolver.resolve("#c:nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void unknownNamespaceThrows() {
        var resolver = new TagResolver(new CustomTagLoader());
        assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve("#unknown:tag"));
    }

    @Test
    void invalidTagFormatThrows() {
        var resolver = new TagResolver(new CustomTagLoader());
        assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve("#invalidformat"));
    }

    @Test
    void repeatedMaterialResolutionIsCached() {
        var resolver = new TagResolver(new CustomTagLoader());
        resolver.resolve("minecraft:coal");
        long before = resolver.resolutionCount();
        resolver.resolve("minecraft:coal");
        resolver.resolve("minecraft:coal");
        assertEquals(before, resolver.resolutionCount(),
                "a cached reference must not be re-resolved");
    }

    @Test
    void warmPrepopulatesCache() throws IOException {
        File tagsFile = tempDir.resolve("tags.yml").toFile();
        Files.writeString(tagsFile.toPath(), "custom_tags:\n  ores:\n    - \"minecraft:coal\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile);

        var resolver = new TagResolver(loader);
        resolver.warm("#c:ores");
        long before = resolver.resolutionCount();
        resolver.resolve("#c:ores");
        assertEquals(before, resolver.resolutionCount(),
                "warm() must flatten the reference into the cache at load");
    }

    @Test
    void customTagResolutionMatchesLoaderContent() throws IOException {
        File tagsFile = tempDir.resolve("tags.yml").toFile();
        Files.writeString(tagsFile.toPath(), "custom_tags:\n  ores:\n    - \"minecraft:coal\"\n    - \"minecraft:iron_ingot\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile);

        var resolver = new TagResolver(loader);
        var set = resolver.resolve("#c:ores");
        assertTrue(set.contains(org.bukkit.Material.COAL));
        assertTrue(set.contains(org.bukkit.Material.IRON_INGOT));
    }
}