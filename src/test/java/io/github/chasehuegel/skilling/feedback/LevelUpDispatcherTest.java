package io.github.chasehuegel.skilling.feedback;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LevelUpDispatcherTest {

    private static SkillDefinition.Ability abilityWithLore(List<String> lore, Map<String, ParameterEvaluator> params) {
        return new SkillDefinition.Ability("test", "Test", 5, "level_up",
                new SkillDefinition.AbilityDisplay(lore),
                new SkillDefinition.Requirements(0.0, List.of(), List.of()),
                null,
                List.of(new SkillDefinition.MechanicEntry("core:dummy", List.of(), params)),
                null);
    }

    @Test
    void unlockLineCarriesLoreHover() {
        var ability = abilityWithLore(List.of("Deals {damage} damage"),
                Map.of("damage", new ConstantEvaluator(5.0)));
        Component line = SkillMenuBuilder.formatAbilityLine(ability, 10);
        Component hovered = LevelUpDispatcher.withAbilityLoreHover(line, ability, 10);

        HoverEvent<?> hover = hovered.hoverEvent();
        assertNotNull(hover);
        Object value = hover.value();
        assertInstanceOf(Component.class, value);
        assertEquals("Deals 5 damage", LegacyComponentSerializer.legacySection().serialize((Component) value));
    }

    @Test
    void emptyLoreRendersPlainNameWithoutHover() {
        var ability = abilityWithLore(List.of(), Map.of());
        Component line = Component.text("Test");
        Component hovered = LevelUpDispatcher.withAbilityLoreHover(line, ability, 10);
        assertNull(hovered.hoverEvent());
    }

    @Test
    void absentLoreRendersPlainNameWithoutHover() {
        var ability = new SkillDefinition.Ability("test", "Test", 5, "level_up",
                null, null, null, null, null);
        Component line = Component.text("Test");
        Component hovered = LevelUpDispatcher.withAbilityLoreHover(line, ability, 10);
        assertNull(hovered.hoverEvent());
    }

    @Test
    void randomBrightColorReturnsNonNull() {
        assertNotNull(LevelUpDispatcher.randomBrightColor());
    }

    @Test
    void isMajorLevelUpReturnsTrueWhenAbilityUnlocksAtLevel() {
        var abilities = List.of(new SkillDefinition.Ability("test", "Test", 5, "level_up", null, null, null, null, null));
        var skill = new SkillDefinition("test", 100, null, null, null, abilities);
        assertTrue(LevelUpDispatcher.isMajorLevelUp(skill, 5));
    }

    @Test
    void isMajorLevelUpReturnsFalseWhenNoAbilityUnlocksAtLevel() {
        var abilities = List.of(new SkillDefinition.Ability("test", "Test", 5, "level_up", null, null, null, null, null));
        var skill = new SkillDefinition("test", 100, null, null, null, abilities);
        assertFalse(LevelUpDispatcher.isMajorLevelUp(skill, 10));
    }

    @Test
    void showXpBossBarWithNegativeXpClampsProgressWithoutThrowing() {
        var skill = new SkillDefinition("test", 100,
                new SkillDefinition.Display("Test", "minecraft:barrier", 0, "GREEN", "SOLID"),
                new SkillDefinition.Progression("polynomial", 50.0, 2.5, new PolynomialEvaluator(50, 2.5)),
                List.of(), List.of(), List.of());

        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("test", -500);

        BossBarPool pool = mock(BossBarPool.class);
        BossBar bar = mock(BossBar.class);
        when(pool.getOrCreate(any(Player.class), eq("test"))).thenReturn(bar);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Skilling plugin = mock(Skilling.class);
        when(plugin.isDebugLogging()).thenReturn(false);

        assertDoesNotThrow(() -> LevelUpDispatcher.showXpBossBar(player, skill, profile, pool, plugin));
        verify(bar).setProgress(0.0);
    }

    @Test
    void scheduledUnlockTaskNoOpsForOfflinePlayer() {
        var ability = new SkillDefinition.Ability("bash", "Bash", 2, "player_interact",
                new SkillDefinition.AbilityDisplay(List.of()),
                new SkillDefinition.Requirements(0.0, List.of(), List.of()),
                null,
                List.of(new SkillDefinition.MechanicEntry("core:dummy", List.of(), Map.of())), null);
        var skill = new SkillDefinition("test", 100,
                new SkillDefinition.Display("Test", "minecraft:barrier", 0, "GREEN", "SOLID"),
                null, List.of(), List.of(ability), List.of());

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var world = mock(org.bukkit.World.class);
        when(player.getLocation()).thenReturn(new org.bukkit.Location(world, 0, 0, 0));
        var firework = mock(org.bukkit.entity.Firework.class);
        when(firework.getPersistentDataContainer())
                .thenReturn(mock(org.bukkit.persistence.PersistentDataContainer.class));
        when(firework.getFireworkMeta()).thenReturn(mock(org.bukkit.inventory.meta.FireworkMeta.class));
        when(world.spawn(any(org.bukkit.Location.class), eq(org.bukkit.entity.Firework.class)))
                .thenReturn(firework);

        Skilling plugin = mock(Skilling.class);
        var profileManager = mock(io.github.chasehuegel.skilling.engine.profile.ProfileManager.class);
        when(plugin.getProfileManager()).thenReturn(profileManager);
        when(plugin.getTitleStayDuration()).thenReturn(1000);
        when(plugin.isDebugLogging()).thenReturn(false);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());

        List<Runnable> scheduled = new ArrayList<>();
        var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        when(scheduler.runTaskLater(any(org.bukkit.plugin.Plugin.class), any(Runnable.class), anyLong()))
                .thenAnswer(inv -> {
                    scheduled.add(inv.getArgument(1));
                    return mock(org.bukkit.scheduler.BukkitTask.class);
                });

        try (org.mockito.MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getScheduler()).thenReturn(scheduler);
            LevelUpDispatcher.broadcastLevelUp(player, skill, 2, plugin, mock(BossBarPool.class));
        }

        assertEquals(1, scheduled.size(), "one unlock announcement must be scheduled");
        clearInvocations(player);
        when(player.isOnline()).thenReturn(false);

        assertDoesNotThrow(() -> scheduled.get(0).run(), "an offline player must not throw in the task");
        verify(player, never()).sendMessage(any(net.kyori.adventure.text.Component.class));
        verify(player, never()).showTitle(any(net.kyori.adventure.title.Title.class));
    }
}
