package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.ui.branding.BrandingConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SkillMenuBuilderLoreTest {

    private static String plain(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component).replaceAll("\u00a7.", "");
    }

    private static SkillDefinition.Ability ability(String id, int unlockLevel, List<String> lore) {
        return new SkillDefinition.Ability(id, "Bash", unlockLevel, "block_break",
                new SkillDefinition.AbilityDisplay(lore),
                new SkillDefinition.Requirements(0.0, List.of(), List.of()),
                null,
                List.of(new SkillDefinition.MechanicEntry("core:xp_bonus", List.of(), java.util.Map.of())),
                null);
    }

    private static SkillDefinition skill(SkillDefinition.Display display,
                                         List<SkillDefinition.Ability> abilities,
                                         ParameterEvaluator evaluator) {
        return new SkillDefinition("test", 100, display,
                new SkillDefinition.Progression("polynomial", 50.0, 2.5, evaluator),
                List.of(), abilities, List.of());
    }

    private static SkillMenuBuilder builder() {
        return new SkillMenuBuilder(mock(SkillManager.class), GuiLayoutConfig.empty());
    }

    @Test
    void defaultLoreRendersLevelXpBarAndLockedAbility() {
        var display = new SkillDefinition.Display("Mining", "minecraft:iron_pickaxe", 0, "GREEN", "SOLID",
                List.of("&7A skill."));
        var skill = skill(display, List.of(ability("bash", 10, List.of("This is locked."))),
                new PolynomialEvaluator(50, 2.5));

        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("test", 3000);

        List<Component> lore = builder().buildSkillLore(skill, profile);

        assertTrue(plain(lore.get(0)).contains("Level 5 / 100"), plain(lore.get(0)));
        assertTrue(plain(lore.get(1)).contains("["), plain(lore.get(1)));
        assertTrue(plain(lore.get(2)).contains("XP: 205 / 1614"), plain(lore.get(2)));
        assertTrue(plain(lore.get(3)).contains("Total XP: 3000"), plain(lore.get(3)));
        assertTrue(plain(lore.get(4)).contains("\u2594"), plain(lore.get(4)));
        assertEquals("A skill.", plain(lore.get(5)));
        assertEquals("", plain(lore.get(6)));
        assertTrue(plain(lore.get(7)).contains("10 · Bash · Passive"), plain(lore.get(7)));
        assertEquals("This is locked.", plain(lore.get(8)));
        assertEquals("", plain(lore.get(9)));
    }

    @Test
    void maxedSkillPinsXpNeededToIntoAndFillsBar() {
        var display = new SkillDefinition.Display("Mining", "minecraft:iron_pickaxe", 0, "GREEN", "SOLID",
                List.of());
        var skill = skill(display, List.of(), new ConstantEvaluator(100));

        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("test", 10000);

        List<Component> lore = builder().buildSkillLore(skill, profile);

        assertEquals("Level 100 / 100", plain(lore.get(0)));
        // Maxed: the progress line reads full and the bar is entirely filled.
        assertTrue(plain(lore.get(2)).contains("XP: 9900 / 9900"), plain(lore.get(2)));
        assertTrue(plain(lore.get(1)).contains("["), plain(lore.get(1)));
        assertTrue(plain(lore.get(1)).endsWith("]"), plain(lore.get(1)));
    }

    @Test
    void emptyLoreAndNoAbilitiesDropTheirTemplateLines() {
        var display = new SkillDefinition.Display("Mining", "minecraft:iron_pickaxe", 0, "GREEN", "SOLID",
                List.of());
        var skill = skill(display, List.of(), new PolynomialEvaluator(50, 2.5));

        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("test", 3000);

        List<Component> lore = builder().buildSkillLore(skill, profile);

        // {lore} and {abilities} lines are dropped; the intentional blank remains.
        assertEquals(6, lore.size());
        assertEquals("Level 5 / 100", plain(lore.get(0)));
        assertEquals("", plain(lore.get(5)));
    }

    @Test
    void customBrandingChangesLoreLayout() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("branding.skill_template", List.of("&eLv {level}", "{bar}", "{abilities}"));
        config.set("branding.bar_template.width", 10);
        config.set("branding.bar_template.filled", "&b#");
        config.set("branding.bar_template.empty", "&8.");
        config.set("branding.bar_template.start", "<");
        config.set("branding.bar_template.end", ">");
        config.set("branding.abilities_template", List.of("{ability}", ""));
        config.set("branding.ability_unlocked_template", List.of("&a>{name}<", "{lore}"));
        config.set("branding.ability_locked_template", List.of("&c#{level}#", "{lore}"));
        BrandingConfig custom = BrandingConfig.from(config.getConfigurationSection("branding"));

        var display = new SkillDefinition.Display("Mining", "minecraft:iron_pickaxe", 0, "GREEN", "SOLID",
                List.of());
        var skill = skill(display, List.of(ability("bash", 5, List.of("Lore"))), new PolynomialEvaluator(50, 2.5));

        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("test", 3000);

        try (MockedStatic<Skilling> skillingStatic = mockStatic(Skilling.class)) {
            Skilling plugin = mock(Skilling.class);
            when(plugin.getBranding()).thenReturn(custom);
            skillingStatic.when(Skilling::getInstance).thenReturn(plugin);

            List<Component> lore = builder().buildSkillLore(skill, profile);

            // Level 5 beats the ability's unlock level 5 (>=), so the unlocked
            // template renders with the custom prefix.
            assertEquals("Lv 5", plain(lore.get(0)));
            assertTrue(plain(lore.get(1)).startsWith("<"), plain(lore.get(1)));
            assertEquals(">Bash<", plain(lore.get(2)));
            assertEquals("Lore", plain(lore.get(3)));
            assertEquals("", plain(lore.get(4)));
        }
    }

    @Test
    void skillColorTokenResolvesToLegacyCode() {
        var display = new SkillDefinition.Display("Mining", "minecraft:iron_pickaxe", 0, "AQUA", "SOLID",
                List.of());
        var skill = skill(display, List.of(), new PolynomialEvaluator(50, 2.5));

        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());
        profile.setXp("test", 3000);

        List<Component> lore = builder().buildSkillLore(skill, profile);

        // Line 3 is "{color}Total XP: 3000" -> aqua (&b) renders as a color code
        // that plaintext-stripping removes, so only the text remains.
        assertEquals("Total XP: 3000", plain(lore.get(3)));
        String serialized = LegacyComponentSerializer.legacySection().serialize(lore.get(3));
        assertTrue(serialized.contains("\u00a7b"), serialized);
    }
}
