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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code offhand} state filter: tag values ({@code offhand:#...})
 * resolve the offhand item through the {@link TagResolver}, while the legacy
 * {@code empty} / {@code weapon} values keep their behavior.
 */
class StateFilterOffhandTest {

    private StateFilterRegistry registry;
    private Player player;
    private PlayerInventory inventory;

    @BeforeEach
    void setUp() throws Exception {
        Path tagsFile = Files.createTempFile("tags", ".yml");
        Files.writeString(tagsFile, """
                custom_tags:
                  melee_weapons:
                    - "minecraft:wooden_sword"
                    - "minecraft:stone_sword"
                    - "minecraft:golden_sword"
                    - "minecraft:iron_sword"
                    - "minecraft:diamond_sword"
                    - "minecraft:netherite_sword"
                    - "minecraft:wooden_axe"
                    - "minecraft:stone_axe"
                    - "minecraft:golden_axe"
                    - "minecraft:iron_axe"
                    - "minecraft:diamond_axe"
                    - "minecraft:netherite_axe"
                    - "minecraft:mace"
                """);
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        var resolver = new TagResolver(loader);
        var entityResolver = new EntityTagResolver(loader);
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry, resolver, entityResolver);

        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
    }

    private void offhand(Material mat) {
        ItemStack stack = item(mat);
        when(inventory.getItemInOffHand()).thenReturn(stack);
    }

    private static ItemStack item(Material mat) {
        var stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(mat);
        return stack;
    }

    @Test
    void swordMatchesMeleeWeaponsTag() {
        offhand(Material.IRON_SWORD);
        assertTrue(registry.evaluate("offhand", player, null, "#c:melee_weapons"));
    }

    @Test
    void axeMatchesMeleeWeaponsTag() {
        offhand(Material.DIAMOND_AXE);
        assertTrue(registry.evaluate("offhand", player, null, "#c:melee_weapons"));
    }

    @Test
    void maceMatchesMeleeWeaponsTag() {
        offhand(Material.MACE);
        assertTrue(registry.evaluate("offhand", player, null, "#c:melee_weapons"));
    }

    @Test
    void tridentFailsMeleeWeaponsTagButPassesWeapon() {
        offhand(Material.TRIDENT);
        assertFalse(registry.evaluate("offhand", player, null, "#c:melee_weapons"));
        assertTrue(registry.evaluate("offhand", player, null, "weapon"));
    }

    @Test
    void nonWeaponFailsMeleeWeaponsTag() {
        offhand(Material.SHIELD);
        assertFalse(registry.evaluate("offhand", player, null, "#c:melee_weapons"));
        assertFalse(registry.evaluate("offhand", player, null, "weapon"));
    }

    @Test
    void emptyValueStillWorks() {
        offhand(Material.AIR);
        assertTrue(registry.evaluate("offhand", player, null, "empty"));
        assertFalse(registry.evaluate("offhand", player, null, "weapon"));
        assertFalse(registry.evaluate("offhand", player, null, "#c:melee_weapons"));
    }

    @Test
    void unknownTagFailsClosed() {
        offhand(Material.IRON_SWORD);
        assertFalse(registry.evaluate("offhand", player, null, "#c:nonexistent"));
    }
}
