package io.github.chasehuegel.skilling.engine.ui.branding;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrandingConfigTest {

    private static BrandingConfig parse(YamlConfiguration config) {
        return BrandingConfig.from(config.getConfigurationSection("branding"));
    }

    @Test
    void absentSectionReturnsDefault() {
        assertEquals(BrandingConfig.DEFAULT, BrandingConfig.from(null));
        assertEquals(BrandingConfig.DEFAULT, parse(new YamlConfiguration()));
    }

    @Test
    void parsesFullSection() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("branding.skill_template", List.of("A", "{bar}"));
        config.set("branding.bar_template.width", 12);
        config.set("branding.bar_template.filled", "&a=");
        config.set("branding.bar_template.empty", "&8-");
        config.set("branding.bar_template.start", "<");
        config.set("branding.bar_template.end", ">");
        config.set("branding.abilities_template", List.of("{ability}"));
        config.set("branding.ability_type_template.active", "&8ACTIVE");
        config.set("branding.ability_type_template.passive", "&8PASSIVE");
        config.set("branding.ability_locked_template", List.of("L"));
        config.set("branding.ability_unlocked_template", List.of("U"));
        config.set("branding.level_up.title", "T");
        config.set("branding.level_up.subtitle", "S");
        config.set("branding.level_up.message", "M");
        config.set("branding.level_up.maxed_message", "MAX");
        config.set("branding.ability_unlock.title", "UT");
        config.set("branding.ability_unlock.subtitle", "US");
        config.set("branding.ability_unlock.message", "UM");
        config.set("branding.ability_feedback.ready_message", "R");
        config.set("branding.gui.title", "G");
        config.set("branding.gui.prev_page", "P");
        config.set("branding.gui.next_page", "N");
        config.set("branding.gui.page_count", "C");
        config.set("branding.gui.skill_name_unlocked", "SU");
        config.set("branding.gui.skill_name_locked", "SL");
        config.set("branding.guide_book.name", "GN");
        config.set("branding.guide_book.lore", "GL");
        config.set("branding.boss_bar.title_format", "BF");
        config.set("branding.boss_bar.default_color", "RED");
        config.set("branding.boss_bar.default_style", "SEGMENTED_10");
        config.set("branding.command.header", "CH");
        config.set("branding.command.command", "CC");
        config.set("branding.command.description", "CD");
        config.set("branding.command.usage", "CU");
        config.set("branding.command.success", "CS");
        config.set("branding.command.error", "CE");
        config.set("branding.command.info", "CI");

        BrandingConfig branding = parse(config);

        assertEquals(List.of("A", "{bar}"), branding.skillTemplate().lines());
        assertEquals(12, branding.barTemplate().width());
        assertEquals("&a=", branding.barTemplate().filled());
        assertEquals("&8-", branding.barTemplate().empty());
        assertEquals("<", branding.barTemplate().start());
        assertEquals(">", branding.barTemplate().end());
        assertEquals(List.of("{ability}"), branding.abilitiesTemplate().lines());
        assertEquals("&8ACTIVE", branding.abilityType().active());
        assertEquals("&8PASSIVE", branding.abilityType().passive());
        assertEquals(List.of("L"), branding.abilities().locked());
        assertEquals(List.of("U"), branding.abilities().unlocked());
        assertEquals("T", branding.levelUp().title());
        assertEquals("S", branding.levelUp().subtitle());
        assertEquals("M", branding.levelUp().message());
        assertEquals("MAX", branding.levelUp().maxedMessage());
        assertEquals("UT", branding.abilityUnlock().title());
        assertEquals("US", branding.abilityUnlock().subtitle());
        assertEquals("UM", branding.abilityUnlock().message());
        assertEquals("R", branding.abilityFeedback().readyMessage());
        assertEquals("G", branding.gui().title());
        assertEquals("P", branding.gui().prevPage());
        assertEquals("N", branding.gui().nextPage());
        assertEquals("C", branding.gui().pageCount());
        assertEquals("SU", branding.gui().skillNameUnlocked());
        assertEquals("SL", branding.gui().skillNameLocked());
        assertEquals("GN", branding.guideBook().name());
        assertEquals("GL", branding.guideBook().lore());
        assertEquals("BF", branding.bossBar().titleFormat());
        assertEquals(BarColor.RED, branding.bossBar().defaultColor());
        assertEquals(BarStyle.SEGMENTED_10, branding.bossBar().defaultStyle());
        assertEquals("CH", branding.command().header());
        assertEquals("CC", branding.command().command());
        assertEquals("CD", branding.command().description());
        assertEquals("CU", branding.command().usage());
        assertEquals("CS", branding.command().success());
        assertEquals("CE", branding.command().error());
        assertEquals("CI", branding.command().info());
    }

    @Test
    void missingKeysFallBackToDefaults() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("branding.skill_template", List.of("Custom"));

        BrandingConfig branding = parse(config);

        assertEquals(List.of("Custom"), branding.skillTemplate().lines());
        assertEquals(BrandingConfig.DEFAULT.barTemplate(), branding.barTemplate());
        assertEquals(BrandingConfig.DEFAULT.levelUp(), branding.levelUp());
        assertEquals(BrandingConfig.DEFAULT.abilities(), branding.abilities());
    }

    @Test
    void outOfRangeBarWidthFails() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("branding.bar_template.width", 0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(config));
        assertTrue(ex.getMessage().contains("width"), ex.getMessage());

        config.set("branding.bar_template.width", 201);
        assertThrows(IllegalArgumentException.class, () -> parse(config));
    }

    @Test
    void blankBarStringFails() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("branding.bar_template.filled", "");
        assertThrows(IllegalArgumentException.class, () -> parse(config));
    }

    @Test
    void invalidBarColorFails() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("branding.boss_bar.default_color", "not_a_color");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(config));
        assertTrue(ex.getMessage().contains("BarColor"), ex.getMessage());
    }

    @Test
    void invalidBarStyleFails() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("branding.boss_bar.default_style", "not_a_style");
        assertThrows(IllegalArgumentException.class, () -> parse(config));
    }

    @Test
    void scalarInsteadOfListFails() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("branding.skill_template", "&aLevel {level}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(config));
        assertTrue(ex.getMessage().contains("skill_template"), ex.getMessage());
    }

    @Test
    void defaultsReproduceHistoricalLook() {
        // Spot-check the defaults preserve the plugin's original styling roles:
        // green positive accents, aqua XP numbers, gold titles, gray secondary text.
        BrandingConfig d = BrandingConfig.DEFAULT;
        assertTrue(d.skillTemplate().lines().get(0).startsWith("&aLevel"), d.skillTemplate().lines().get(0));
        assertTrue(d.skillTemplate().lines().get(2).startsWith("&aXP:"), d.skillTemplate().lines().get(2));
        assertTrue(d.skillTemplate().lines().get(3).startsWith("{color}"), d.skillTemplate().lines().get(3));
        assertEquals("&6Level up!", d.levelUp().title());
        assertEquals("&6Skills", d.gui().title());
        assertEquals(20, d.barTemplate().width());
        assertEquals("&a█", d.barTemplate().filled());
        assertEquals(BarColor.WHITE, d.bossBar().defaultColor());
    }

    @Test
    void shippedConfigYmlBrandingParsesToDefaults() {
        // The bundled config.yml template must be valid YAML and reproduce the
        // engine defaults exactly, so a first-run server renders the same look
        // whether branding is loaded from the file or from BrandingConfig.DEFAULT.
        java.io.InputStream in = BrandingConfigTest.class.getResourceAsStream("/config.yml");
        assertTrue(in != null, "shipped config.yml must exist on the test classpath");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in));
        assertEquals(BrandingConfig.DEFAULT, BrandingConfig.from(config.getConfigurationSection("branding")));
    }
}
