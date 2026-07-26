package io.github.chasehuegel.skilling.tag;

import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.junit.jupiter.api.Test;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class TagResolverTest {

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
}