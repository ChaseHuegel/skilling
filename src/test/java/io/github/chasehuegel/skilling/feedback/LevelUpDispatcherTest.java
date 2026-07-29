package io.github.chasehuegel.skilling.feedback;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LevelUpDispatcherTest {

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
        var abilities = List.of(new SkillDefinition.Ability("test", "Test", 5, null, null, null, null, null));
        var skill = new SkillDefinition("test", 100, null, null, null, abilities);
        assertTrue(LevelUpDispatcher.isMajorLevelUp(skill, 5));
    }

    @Test
    void isMajorLevelUpReturnsFalseWhenNoAbilityUnlocksAtLevel() {
        var abilities = List.of(new SkillDefinition.Ability("test", "Test", 5, null, null, null, null, null));
        var skill = new SkillDefinition("test", 100, null, null, null, abilities);
        assertFalse(LevelUpDispatcher.isMajorLevelUp(skill, 10));
    }
}
