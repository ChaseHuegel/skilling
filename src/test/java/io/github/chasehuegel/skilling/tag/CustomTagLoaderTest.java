package io.github.chasehuegel.skilling.tag;

import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
    void parsesTagKeys() throws IOException {
        File tagsFile = tempDir.resolve("tags.yml").toFile();
        try (var w = new FileWriter(tagsFile)) {
            w.write("custom_tags:\n  ores:\n    - \"minecraft:coal\"\n    - \"minecraft:iron_ingot\"\n");
        }

        var loader = new CustomTagLoader();
        loader.load(tagsFile);

        assertTrue(loader.getKeys().contains("#c:ores"));
        var resolved = loader.resolve("#c:ores");
        assertTrue(resolved.contains(Material.COAL));
        assertTrue(resolved.contains(Material.IRON_INGOT));
    }

    @Test
    void parsesVanillaCrossReferences() throws IOException {
        File tagsFile = tempDir.resolve("tags.yml").toFile();
        try (var w = new FileWriter(tagsFile)) {
            w.write("custom_tags:\n  logs:\n    - \"#minecraft:logs\"\n");
        }

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of(Material.OAK_LOG, Material.BIRCH_LOG));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(Material.class)))
                    .thenReturn(tag);

            var loader = new CustomTagLoader();
            loader.load(tagsFile);

            var resolved = loader.resolve("#c:logs");
            assertTrue(resolved.contains(Material.OAK_LOG));
            assertTrue(resolved.contains(Material.BIRCH_LOG));
        }
    }

    @Test
    void circularReferencesDoNotHangAndResolveEmpty() throws IOException {
        File tagsFile = tempDir.resolve("tags.yml").toFile();
        try (var w = new FileWriter(tagsFile)) {
            w.write("custom_tags:\n  a:\n    - \"#c:b\"\n  b:\n    - \"#c:a\"\n");
        }

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

            var loader = new CustomTagLoader();
            loader.load(tagsFile);

            assertTrue(loader.resolve("#c:a").isEmpty(),
                    "a circular tag reference must resolve to an empty set, not loop");
        }
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