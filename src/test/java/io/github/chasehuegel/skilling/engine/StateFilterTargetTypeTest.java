package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
 * Verifies the {@code target_type} state filter: it evaluates both
 * {@link EntityDamageByEntityEvent} (the damaged entity) and
 * {@link EntityDeathEvent} (the killed entity), supports {@code #...} entity
 * tags, and fails closed for events it cannot evaluate.
 */
class StateFilterTargetTypeTest {

    @TempDir
    Path tempDir;

    private StateFilterRegistry registry;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        Path tagsFile = tempDir.resolve("tags.yml");
        Files.writeString(tagsFile, """
                entity_tags:
                  undead:
                    - "minecraft:zombie"
                    - "minecraft:skeleton"
                    - "minecraft:wither_skeleton"
                """);
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        var resolver = new TagResolver(loader);
        var entityResolver = new EntityTagResolver(loader);
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry, resolver, entityResolver);

        player = mock(Player.class);
    }

    private static LivingEntity entity(EntityType type) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(type);
        return entity;
    }

    private static EntityDeathEvent deathEvent(EntityType type) {
        EntityDeathEvent event = mock(EntityDeathEvent.class);
        LivingEntity killed = entity(type);
        when(event.getEntity()).thenReturn(killed);
        return event;
    }

    @Test
    void entityKillMatchesExactType() {
        assertTrue(registry.evaluate("target_type", player,
                deathEvent(EntityType.ZOMBIE), "minecraft:zombie"));
    }

    @Test
    void entityKillRejectsOtherType() {
        assertFalse(registry.evaluate("target_type", player,
                deathEvent(EntityType.COW), "minecraft:zombie"));
    }

    @Test
    void entityKillMatchesCustomTag() {
        assertTrue(registry.evaluate("target_type", player,
                deathEvent(EntityType.WITHER_SKELETON), "#c:undead"));
        assertFalse(registry.evaluate("target_type", player,
                deathEvent(EntityType.COW), "#c:undead"));
    }

    @Test
    void entityDamageStillMatchesExactType() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        LivingEntity damaged = entity(EntityType.ZOMBIE);
        when(event.getEntity()).thenReturn(damaged);
        assertTrue(registry.evaluate("target_type", player, event, "minecraft:zombie"));
        assertFalse(registry.evaluate("target_type", player, event, "minecraft:cow"));
    }

    @Test
    void unsupportedEventFailsClosed() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        assertFalse(registry.evaluate("target_type", player, event, "minecraft:zombie"));
    }

    @Test
    void unknownTagFailsClosed() {
        assertFalse(registry.evaluate("target_type", player,
                deathEvent(EntityType.ZOMBIE), "#c:does_not_exist"));
    }

    @Test
    void blankValueFailsClosed() {
        assertFalse(registry.evaluate("target_type", player,
                deathEvent(EntityType.ZOMBIE), ""));
    }

    @Test
    void playerPlacedFalseMatchesNaturalBlocksOnly() {
        var placed = mock(org.bukkit.event.block.BlockBreakEvent.class);
        var natural = mock(org.bukkit.event.block.BlockBreakEvent.class);
        var placedBlock = mock(org.bukkit.block.Block.class);
        var naturalBlock = mock(org.bukkit.block.Block.class);
        when(placedBlock.hasMetadata("player_placed")).thenReturn(true);
        when(naturalBlock.hasMetadata("player_placed")).thenReturn(false);
        when(placed.getBlock()).thenReturn(placedBlock);
        when(natural.getBlock()).thenReturn(naturalBlock);

        assertTrue(registry.evaluate("player_placed", player, natural, "false"));
        assertFalse(registry.evaluate("player_placed", player, placed, "false"));
    }

    @Test
    void playerPlacedTrueMatchesPlayerPlacedBlocksOnly() {
        var placed = mock(org.bukkit.event.block.BlockBreakEvent.class);
        var natural = mock(org.bukkit.event.block.BlockBreakEvent.class);
        var placedBlock = mock(org.bukkit.block.Block.class);
        var naturalBlock = mock(org.bukkit.block.Block.class);
        when(placedBlock.hasMetadata("player_placed")).thenReturn(true);
        when(naturalBlock.hasMetadata("player_placed")).thenReturn(false);
        when(placed.getBlock()).thenReturn(placedBlock);
        when(natural.getBlock()).thenReturn(naturalBlock);

        assertTrue(registry.evaluate("player_placed", player, placed, "true"));
        assertFalse(registry.evaluate("player_placed", player, natural, "true"));
    }
}
