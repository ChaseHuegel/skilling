package io.github.chasehuegel.skilling.tag;

import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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

    @Test
    void unknownCustomTagKeyRejectedWhenStoreLoaded() throws IOException {
        File tagsFile = tempDir.resolve("tags.yml").toFile();
        Files.writeString(tagsFile.toPath(), "custom_tags:\n  ores:\n    - \"minecraft:coal\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile);

        var resolver = new TagResolver(loader);
        assertTrue(resolver.isKnown("#c:ores"));
        assertFalse(resolver.isKnown("#c:typo"));
    }

    @Test
    void unknownCustomTagKeyDefersWhenLoaderNeverRan() {
        // A resolver whose loader never ran (unit-test context) must not reject
        // #c: references it cannot verify.
        var resolver = new TagResolver(new CustomTagLoader());
        assertTrue(resolver.isKnown("#c:anything"));
    }

    @Test
    void singleMaterialResolvesToSingletonSet() {
        var resolver = new TagResolver(new CustomTagLoader());
        var set = resolver.resolve("minecraft:stone");
        assertEquals(1, set.size());
        assertTrue(set.contains(Material.STONE));
    }

    @Test
    void vanillaTagResolvesViaBukkitRegistry() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of(Material.OAK_LOG, Material.BIRCH_LOG));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(Material.class)))
                    .thenReturn(tag);

            var resolver = new TagResolver(new CustomTagLoader());
            var set = resolver.resolve("#minecraft:logs");
            assertTrue(set.contains(Material.OAK_LOG));
            assertTrue(set.contains(Material.BIRCH_LOG));
        }
    }

    @Test
    void vanillaTagResolutionIsCached() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of(Material.OAK_LOG));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(Material.class)))
                    .thenReturn(tag);

            var resolver = new TagResolver(new CustomTagLoader());
            resolver.resolve("#minecraft:logs");
            long before = resolver.resolutionCount();
            resolver.resolve("#minecraft:logs");
            assertEquals(before, resolver.resolutionCount(),
                    "a vanilla tag must be flattened once and cached");
        }
    }
}