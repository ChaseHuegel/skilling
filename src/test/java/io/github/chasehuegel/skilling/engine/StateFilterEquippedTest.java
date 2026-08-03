package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the target-driven {@code equipped_all} / {@code equipped_any} state
 * filters: armor slots match against a material or {@code #...} tag resolved via
 * the {@link TagResolver}, with no hard-coded armor-tier knowledge.
 */
class StateFilterEquippedTest {

    private StateFilterRegistry registry;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        Path tagsFile = Files.createTempFile("tags", ".yml");
        Files.writeString(tagsFile, """
                custom_tags:
                  light_armor:
                    - "minecraft:leather_helmet"
                    - "minecraft:leather_chestplate"
                    - "minecraft:leather_leggings"
                    - "minecraft:leather_boots"
                  heavy_armor:
                    - "minecraft:diamond_helmet"
                    - "minecraft:diamond_chestplate"
                    - "minecraft:diamond_leggings"
                    - "minecraft:diamond_boots"
                  unarmored:
                    - "minecraft:air"
                    - "minecraft:elytra"
                    - "minecraft:carved_pumpkin"
                """);
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        var resolver = new TagResolver(loader);
        var entityResolver = new EntityTagResolver(loader);
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry, resolver, entityResolver);

        player = mock(Player.class);
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
    }

    private void wear(ItemStack... slots) {
        when(player.getInventory().getArmorContents()).thenReturn(slots);
    }

    private static ItemStack item(Material mat) {
        var stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(mat);
        return stack;
    }

    private static ItemStack[] fullSet(Material mat) {
        return new ItemStack[]{item(mat), item(mat), item(mat), item(mat)};
    }

    @Test
    void fullLeatherSetMatchesEquippedAllLight() {
        wear(fullSet(Material.LEATHER_HELMET));
        assertTrue(registry.evaluate("equipped_all", player, null, "#c:light_armor"));
        assertFalse(registry.evaluate("equipped_all", player, null, "#c:heavy_armor"));
    }

    @Test
    void fullDiamondSetMatchesEquippedAllHeavy() {
        wear(fullSet(Material.DIAMOND_BOOTS));
        assertTrue(registry.evaluate("equipped_all", player, null, "#c:heavy_armor"));
        assertFalse(registry.evaluate("equipped_all", player, null, "#c:light_armor"));
    }

    @Test
    void mixedSetFailsEquippedAllButPassesEquippedAny() {
        wear(item(Material.LEATHER_HELMET), item(Material.LEATHER_CHESTPLATE),
                item(Material.LEATHER_LEGGINGS), item(Material.DIAMOND_BOOTS));
        assertFalse(registry.evaluate("equipped_all", player, null, "#c:light_armor"));
        assertTrue(registry.evaluate("equipped_any", player, null, "#c:light_armor"));
        assertTrue(registry.evaluate("equipped_any", player, null, "#c:heavy_armor"));
    }

    @Test
    void emptySlotsMatchUnarmoredOnly() {
        wear(item(Material.AIR), item(Material.AIR), item(Material.AIR), item(Material.AIR));
        assertTrue(registry.evaluate("equipped_all", player, null, "#c:unarmored"));
        assertFalse(registry.evaluate("equipped_all", player, null, "#c:light_armor"));
    }

    @Test
    void elytraPlusEmptySlotsMatchUnarmored() {
        wear(item(Material.AIR), item(Material.ELYTRA), item(Material.AIR), item(Material.AIR));
        assertTrue(registry.evaluate("equipped_all", player, null, "#c:unarmored"));
    }

    @Test
    void directMaterialTargetWorks() {
        wear(fullSet(Material.LEATHER_BOOTS));
        assertTrue(registry.evaluate("equipped_all", player, null, "minecraft:leather_boots"));
        assertFalse(registry.evaluate("equipped_all", player, null, "minecraft:diamond_helmet"));
    }

    @Test
    void equippedAnyRequiresAtLeastOneMatch() {
        wear(item(Material.AIR), item(Material.AIR), item(Material.AIR), item(Material.DIAMOND_HELMET));
        assertTrue(registry.evaluate("equipped_any", player, null, "#c:heavy_armor"));
        assertFalse(registry.evaluate("equipped_any", player, null, "#c:light_armor"));
    }

    @Test
    void unknownTargetFailsClosed() {
        wear(fullSet(Material.LEATHER_HELMET));
        assertFalse(registry.evaluate("equipped_all", player, null, "#c:nonexistent"));
    }
}
