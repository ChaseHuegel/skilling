package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code grown} state filter: it is true only for harvest-ready
 * crops (ageable crops at maximum age, or non-ageable crops like melon fruit
 * and sugar cane) and fails closed for immature crops, non-crop blocks, and
 * non-block-break events, so place+break can never farm XP or seed yield.
 */
class StateFilterGrownTest {

    @TempDir
    Path tempDir;

    private StateFilterRegistry registry;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        Path tagsFile = tempDir.resolve("tags.yml");
        Files.writeString(tagsFile, """
                custom_tags:
                  crops:
                    - "minecraft:wheat"
                    - "minecraft:carrots"
                    - "minecraft:melon"
                    - "minecraft:sugar_cane"
                """);
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry, new TagResolver(loader), new EntityTagResolver(loader));
        player = mock(Player.class);
    }

    private BlockBreakEvent breakEventWith(Material type, org.bukkit.block.data.BlockData data) {
        var event = mock(BlockBreakEvent.class);
        var block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        when(block.getBlockData()).thenReturn(data);
        when(event.getBlock()).thenReturn(block);
        return event;
    }

    private org.bukkit.block.data.Ageable ageable(int age, int maxAge) {
        var data = mock(org.bukkit.block.data.Ageable.class);
        when(data.getAge()).thenReturn(age);
        when(data.getMaximumAge()).thenReturn(maxAge);
        return data;
    }

    @Test
    void matureAgeableCropIsGrown() {
        var event = breakEventWith(Material.WHEAT, ageable(7, 7));
        assertTrue(registry.evaluate("grown", player, event, ""));
    }

    @Test
    void immatureAgeableCropIsNotGrown() {
        var event = breakEventWith(Material.WHEAT, ageable(2, 7));
        assertFalse(registry.evaluate("grown", player, event, ""), "an immature crop must not count as grown");
    }

    @Test
    void nonAgeableCropIsAlwaysGrown() {
        // Melon fruit has no progress stage; it becomes breakable only when the
        // attached stem matures, so it is always considered grown.
        var event = breakEventWith(Material.MELON, mock(org.bukkit.block.data.BlockData.class));
        assertTrue(registry.evaluate("grown", player, event, ""));
    }

    @Test
    void nonCropBlockIsNotGrown() {
        var event = breakEventWith(Material.STONE, mock(org.bukkit.block.data.BlockData.class));
        assertFalse(registry.evaluate("grown", player, event, ""), "a non-crop block must never count as grown");
    }

    @Test
    void nonBlockBreakEventFailsClosed() {
        assertFalse(registry.evaluate("grown", player, mock(org.bukkit.event.entity.EntityDamageEvent.class), ""));
    }
}