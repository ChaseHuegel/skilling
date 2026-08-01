package io.github.chasehuegel.skilling.feedback;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
}
