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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    void resolveBarColorNullReturnsNull() {
        assertNull(LevelUpDispatcher.resolveBarColor(null));
    }

    @Test
    void resolveBarColorBlankReturnsNull() {
        assertNull(LevelUpDispatcher.resolveBarColor(""));
    }

    @Test
    void resolveBarColorRedReturnsNamedTextColorRed() {
        assertEquals(NamedTextColor.RED, LevelUpDispatcher.resolveBarColor("RED"));
    }

    @Test
    void resolveBarColorBlueReturnsNamedTextColorBlue() {
        assertEquals(NamedTextColor.BLUE, LevelUpDispatcher.resolveBarColor("BLUE"));
    }

    @Test
    void mmColorNamePinkReturnsLightPurple() {
        assertEquals("light_purple", LevelUpDispatcher.mmColorName("PINK"));
    }

    @Test
    void mmColorNamePurpleReturnsDarkPurple() {
        assertEquals("dark_purple", LevelUpDispatcher.mmColorName("PURPLE"));
    }

    @Test
    void mmColorNameRedReturnsRed() {
        assertEquals("red", LevelUpDispatcher.mmColorName("RED"));
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
}
