package io.github.chasehuegel.skilling.tag;

import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies the entity-type tag resolver used by the {@code target_type} state
 * filter: vanilla {@code #minecraft:} entity tags, custom {@code #c:} entity
 * tags, single entity types, caching, and fail-fast on invalid references.
 */
class EntityTagResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void nullOrBlankReturnsEmpty() {
        var resolver = new EntityTagResolver(new CustomTagLoader());
        assertTrue(resolver.resolve(null).isEmpty());
        assertTrue(resolver.resolve("").isEmpty());
        assertTrue(resolver.resolve("   ").isEmpty());
    }

    @Test
    void missingCustomTagReturnsEmpty() {
        var resolver = new EntityTagResolver(new CustomTagLoader());
        assertTrue(resolver.resolve("#c:nonexistent").isEmpty());
    }

    @Test
    void unknownNamespaceThrows() {
        var resolver = new EntityTagResolver(new CustomTagLoader());
        assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve("#unknown:tag"));
    }

    @Test
    void invalidTagFormatThrows() {
        var resolver = new EntityTagResolver(new CustomTagLoader());
        assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve("#invalidformat"));
    }

    @Test
    void singleEntityResolvesToSingletonSet() {
        var resolver = new EntityTagResolver(new CustomTagLoader());
        var set = resolver.resolve("minecraft:zombie");
        assertEquals(1, set.size());
        assertTrue(set.contains(EntityType.ZOMBIE));
    }

    @Test
    void customEntityTagMatchesLoaderContent() throws IOException {
        File tagsFile = tempDir.resolve("tags.yml").toFile();
        Files.writeString(tagsFile.toPath(), """
                entity_tags:
                  undead:
                    - "minecraft:zombie"
                    - "minecraft:skeleton"
                """);
        var loader = new CustomTagLoader();
        loader.load(tagsFile);

        var resolver = new EntityTagResolver(loader);
        var set = resolver.resolve("#c:undead");
        assertTrue(set.contains(EntityType.ZOMBIE));
        assertTrue(set.contains(EntityType.SKELETON));
    }

    @Test
    void vanillaEntityTagResolvesViaBukkitRegistry() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            @SuppressWarnings("unchecked")
            Tag<EntityType> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of(EntityType.ZOMBIE, EntityType.HUSK));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(EntityType.class)))
                    .thenReturn(tag);

            var resolver = new EntityTagResolver(new CustomTagLoader());
            var set = resolver.resolve("#minecraft:zombies");
            assertTrue(set.contains(EntityType.ZOMBIE));
            assertTrue(set.contains(EntityType.HUSK));
        }
    }

    @Test
    void emptyVanillaTagResolvesToEmptySetNotThrow() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            @SuppressWarnings("unchecked")
            Tag<EntityType> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of());
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(EntityType.class)))
                    .thenReturn(tag);

            var resolver = new EntityTagResolver(new CustomTagLoader());
            assertTrue(resolver.resolve("#minecraft:empty_tag").isEmpty(),
                    "an empty vanilla tag must resolve to an empty set, not throw");
        }
    }

    @Test
    void repeatedEntityResolutionIsCached() {
        var resolver = new EntityTagResolver(new CustomTagLoader());
        resolver.resolve("minecraft:zombie");
        assertTrue(resolver.resolve("minecraft:zombie").contains(EntityType.ZOMBIE));
        assertTrue(resolver.resolve("minecraft:zombie").contains(EntityType.ZOMBIE));
    }
}
