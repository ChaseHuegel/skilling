package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link UnlockRecipeMechanic}: the unlock performs {@code discoverRecipe}
 * exactly once, and every repeat path (already discovered, unregistered recipe,
 * missing parameter) is a no-op that never touches the recipe book.
 */
class UnlockRecipeMechanicTest {

    private static final NamespacedKey PICKAXE = NamespacedKey.minecraft("netherite_pickaxe");

    private static void stubServer(MockedStatic<Bukkit> bukkit, Recipe registered) {
        Server server = mock(Server.class);
        when(server.getRecipe(PICKAXE)).thenReturn(registered);
        bukkit.when(Bukkit::getServer).thenReturn(server);
    }

    private static Recipe recipe() {
        return mock(Recipe.class);
    }

    @Test
    void unlocksRecipeWhenUndiscovered() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            stubServer(bukkit, recipe());
            Player player = mock(Player.class);
            when(player.hasDiscoveredRecipe(PICKAXE)).thenReturn(false);

            assertTrue(new UnlockRecipeMechanic().execute(
                    player, Map.of("recipe", "minecraft:netherite_pickaxe"), null));
            verify(player).discoverRecipe(PICKAXE);
        }
    }

    @Test
    void alreadyDiscoveredRecipeIsNoOp() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            stubServer(bukkit, recipe());
            Player player = mock(Player.class);
            when(player.hasDiscoveredRecipe(PICKAXE)).thenReturn(true);

            assertFalse(new UnlockRecipeMechanic().execute(
                    player, Map.of("recipe", "minecraft:netherite_pickaxe"), null));
            verify(player, never()).discoverRecipe(PICKAXE);
        }
    }

    @Test
    void unregisteredRecipeIsNoOp() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            stubServer(bukkit, null);
            Player player = mock(Player.class);

            assertFalse(new UnlockRecipeMechanic().execute(
                    player, Map.of("recipe", "minecraft:netherite_pickaxe"), null));
            verify(player, never()).discoverRecipe(any());
        }
    }

    @Test
    void missingOrBlankParamIsNoOp() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            stubServer(bukkit, recipe());
            Player player = mock(Player.class);

            assertFalse(new UnlockRecipeMechanic().execute(player, Map.of(), null));
            assertFalse(new UnlockRecipeMechanic().execute(player, Map.of("recipe", ""), null));
            verify(player, never()).discoverRecipe(any());
        }
    }
}
