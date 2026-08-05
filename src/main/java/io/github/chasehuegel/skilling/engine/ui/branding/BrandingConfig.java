package io.github.chasehuegel.skilling.engine.ui.branding;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable parse of the {@code branding:} section of {@code config.yml}.
 *
 * <p>Every in-game visual element is authored here as legacy {@code &}-code
 * templates with {@code {placeholder}} tokens. Templates are rendered verbatim;
 * the engine adds no hardcoded spacing or separators between template blocks.
 *
 * <p><b>Fail-fast:</b> wrong-typed values, an out-of-range bar width, blank
 * bar strings, and invalid {@link BarColor}/{@link BarStyle} names throw
 * {@link IllegalArgumentException} during load or reload. Missing keys fall
 * back to {@link #DEFAULT}, which reproduces the plugin's historical look.
 */
public final class BrandingConfig {

    /**
     * The full skill tooltip, one template line per rendered lore line.
     *
     * @param lines the template lines
     */
    public record SkillTemplate(List<String> lines) {}

    /**
     * The XP progress bar rendered for the {@code {bar}} token.
     *
     * @param width  character width of the bar (1-200)
     * @param filled per-unit filled string (may embed {@code &} codes)
     * @param empty  per-unit empty string (may embed {@code &} codes)
     * @param start  left bracket string
     * @param end    right bracket string
     */
    public record BarTemplate(int width, String filled, String empty, String start, String end) {}

    /**
     * Template repeated once per ability with no separator injected between
     * ability blocks.
     *
     * @param lines the template lines
     */
    public record AbilitiesTemplate(List<String> lines) {}

    /**
     * The {@code {type}} tokens for active vs passive abilities.
     *
     * @param active  rendered active-ability type text
     * @param passive rendered passive-ability type text
     */
    public record AbilityType(String active, String passive) {}

    /**
     * The locked and unlocked ability templates.
     *
     * @param locked   template for abilities the player has not yet unlocked
     * @param unlocked template for unlocked abilities
     */
    public record AbilityTemplates(List<String> locked, List<String> unlocked) {}

    /**
     * Level-up feedback templates.
     *
     * @param title        main title
     * @param subtitle     subtitle
     * @param message      chat message
     * @param maxedMessage broadcast message at max level
     */
    public record LevelUp(String title, String subtitle, String message, String maxedMessage) {}

    /**
     * Ability-unlock feedback templates.
     *
     * @param title    main title
     * @param subtitle subtitle
     * @param message  chat message
     */
    public record AbilityUnlock(String title, String subtitle, String message) {}

    /**
     * Ability readiness feedback.
     *
     * @param readyMessage chat + action-bar text when a cooldown finishes
     */
    public record AbilityFeedback(String readyMessage) {}

    /**
     * GUI chrome templates not covered by {@code gui.yml}.
     *
     * @param title              fallback chest title (flat layout)
     * @param prevPage           previous-page arrow name
     * @param nextPage           next-page arrow name
     * @param pageCount          page indicator count line ({@code {count}})
     * @param skillNameUnlocked  unlocked skill icon display name ({@code {name}}, {@code {color}})
     * @param skillNameLocked    locked skill icon display name
     */
    public record Gui(String title, String prevPage, String nextPage, String pageCount,
                      String skillNameUnlocked, String skillNameLocked) {}

    /**
     * Skills Guide Book item text.
     *
     * @param name the book display name
     * @param lore the book lore line
     */
    public record GuideBook(String name, String lore) {}

    /**
     * XP boss bar rendering.
     *
     * @param titleFormat  bar title template ({@code {color}} {@code {name}} {@code {level}} {@code {into}} {@code {needed}})
     * @param defaultColor BarColor for pool-created bars before skill override
     * @param defaultStyle BarStyle for pool-created bars before skill override
     */
    public record BossBar(String titleFormat, BarColor defaultColor, BarStyle defaultStyle) {}

    /**
     * Command feedback templates.
     *
     * @param header      help header ({@code {title}})
     * @param command     help command line ({@code {command}})
     * @param description help description ({@code {description}})
     * @param usage       usage line ({@code {usage}})
     * @param success     success message ({@code {message}})
     * @param error       error message ({@code {message}})
     * @param info        informational message ({@code {message}})
     */
    public record Command(String header, String command, String description, String usage,
                          String success, String error, String info) {}

    private final SkillTemplate skillTemplate;
    private final BarTemplate barTemplate;
    private final AbilitiesTemplate abilitiesTemplate;
    private final AbilityType abilityType;
    private final AbilityTemplates abilities;
    private final LevelUp levelUp;
    private final AbilityUnlock abilityUnlock;
    private final AbilityFeedback abilityFeedback;
    private final Gui gui;
    private final GuideBook guideBook;
    private final BossBar bossBar;
    private final Command command;

    private BrandingConfig(SkillTemplate skillTemplate, BarTemplate barTemplate,
                           AbilitiesTemplate abilitiesTemplate, AbilityType abilityType,
                           AbilityTemplates abilities, LevelUp levelUp, AbilityUnlock abilityUnlock,
                           AbilityFeedback abilityFeedback, Gui gui, GuideBook guideBook,
                           BossBar bossBar, Command command) {
        this.skillTemplate = skillTemplate;
        this.barTemplate = barTemplate;
        this.abilitiesTemplate = abilitiesTemplate;
        this.abilityType = abilityType;
        this.abilities = abilities;
        this.levelUp = levelUp;
        this.abilityUnlock = abilityUnlock;
        this.abilityFeedback = abilityFeedback;
        this.gui = gui;
        this.guideBook = guideBook;
        this.bossBar = bossBar;
        this.command = command;
    }

    /** Default branding that reproduces the plugin's historical visual look. */
    public static final BrandingConfig DEFAULT = new BrandingConfig(
            new SkillTemplate(List.of(
                    "&aLevel {level} / {max_level}",
                    "{bar}",
                    "&aXP: {xp_into} / {xp_needed}",
                    "{color}Total XP: {xp_total}",
                    "&7\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594\u2594",
                    "{lore}",
                    "",
                    "{abilities}")),
            new BarTemplate(20, "&a\u2588", "&8\u2588", "&7[", "&7]"),
            new AbilitiesTemplate(List.of("{ability}", "")),
            new AbilityType("&8Active", "&8Passive"),
            new AbilityTemplates(
                    List.of("&c\u274c {level} &8\u00b7 {name} &8\u00b7 {type}", "{lore}"),
                    List.of("&a\u2714 {name} &8\u00b7 {type}", "{lore}")),
            new LevelUp("&6Level up!", "{color}{name} &aincreased to {level}",
                    "&fYou leveled up &a[{name} {level}]",
                    "&f{player} has reached max level {color}[{name}]"),
            new AbilityUnlock("&6Unlocked!", "&a\u2714 {name} &8\u00b7 {type}",
                    "&fYou unlocked the ability &a[{name} &8\u00b7 {type}&a]"),
            new AbilityFeedback("&a\u2726 {color}{name} &ais ready!"),
            new Gui("&6Skills", "&6\u25c0 Prev Page", "&6Next Page \u25b6", "&7{count} skill(s)",
                    "&a{name}", "&7{name} &8\u00b7 Locked"),
            new GuideBook("&6Skills Guide", "&7Right-click to open your skills"),
            new BossBar("{color}{name} &7- &f{level}", BarColor.WHITE, BarStyle.SOLID),
            new Command("&6=== {title} ===", "&e{command}", "&f{description}",
                    "&eUsage: {usage}", "&a{message}", "&c{message}", "&7{message}")
    );

    /**
     * Parses the {@code branding} section of the plugin config.
     *
     * @param section the {@code branding} section, or null when absent
     * @return the parsed branding, or {@link #DEFAULT} when the section is absent
     * @throws IllegalArgumentException on a malformed value (fail-fast)
     */
    public static BrandingConfig from(ConfigurationSection section) {
        if (section == null) return DEFAULT;

        SkillTemplate skillTemplate = new SkillTemplate(lines(section, "skill_template", DEFAULT.skillTemplate.lines()));
        BarTemplate barTemplate = parseBarTemplate(section.getConfigurationSection("bar_template"));
        AbilitiesTemplate abilitiesTemplate = new AbilitiesTemplate(
                lines(section, "abilities_template", DEFAULT.abilitiesTemplate.lines()));

        ConfigurationSection abilityTypeSection = sub(section, "ability_type_template");
        AbilityType abilityType = new AbilityType(
                str(abilityTypeSection, "active", DEFAULT.abilityType.active()),
                str(abilityTypeSection, "passive", DEFAULT.abilityType.passive()));

        AbilityTemplates abilities = new AbilityTemplates(
                lines(section, "ability_locked_template", DEFAULT.abilities.locked()),
                lines(section, "ability_unlocked_template", DEFAULT.abilities.unlocked()));

        ConfigurationSection levelUpSection = sub(section, "level_up");
        LevelUp levelUp = new LevelUp(
                str(levelUpSection, "title", DEFAULT.levelUp.title()),
                str(levelUpSection, "subtitle", DEFAULT.levelUp.subtitle()),
                str(levelUpSection, "message", DEFAULT.levelUp.message()),
                str(levelUpSection, "maxed_message", DEFAULT.levelUp.maxedMessage()));

        ConfigurationSection unlockSection = sub(section, "ability_unlock");
        AbilityUnlock abilityUnlock = new AbilityUnlock(
                str(unlockSection, "title", DEFAULT.abilityUnlock.title()),
                str(unlockSection, "subtitle", DEFAULT.abilityUnlock.subtitle()),
                str(unlockSection, "message", DEFAULT.abilityUnlock.message()));

        ConfigurationSection feedbackSection = sub(section, "ability_feedback");
        AbilityFeedback abilityFeedback = new AbilityFeedback(
                str(feedbackSection, "ready_message", DEFAULT.abilityFeedback.readyMessage()));

        ConfigurationSection guiSection = sub(section, "gui");
        Gui gui = new Gui(
                str(guiSection, "title", DEFAULT.gui.title()),
                str(guiSection, "prev_page", DEFAULT.gui.prevPage()),
                str(guiSection, "next_page", DEFAULT.gui.nextPage()),
                str(guiSection, "page_count", DEFAULT.gui.pageCount()),
                str(guiSection, "skill_name_unlocked", DEFAULT.gui.skillNameUnlocked()),
                str(guiSection, "skill_name_locked", DEFAULT.gui.skillNameLocked()));

        ConfigurationSection bookSection = sub(section, "guide_book");
        GuideBook guideBook = new GuideBook(
                str(bookSection, "name", DEFAULT.guideBook.name()),
                str(bookSection, "lore", DEFAULT.guideBook.lore()));

        ConfigurationSection barSection = sub(section, "boss_bar");
        BossBar bossBar = new BossBar(
                str(barSection, "title_format", DEFAULT.bossBar.titleFormat()),
                barColor(barSection, "default_color", DEFAULT.bossBar.defaultColor()),
                barStyle(barSection, "default_style", DEFAULT.bossBar.defaultStyle()));

        ConfigurationSection commandSection = sub(section, "command");
        Command command = new Command(
                str(commandSection, "header", DEFAULT.command.header()),
                str(commandSection, "command", DEFAULT.command.command()),
                str(commandSection, "description", DEFAULT.command.description()),
                str(commandSection, "usage", DEFAULT.command.usage()),
                str(commandSection, "success", DEFAULT.command.success()),
                str(commandSection, "error", DEFAULT.command.error()),
                str(commandSection, "info", DEFAULT.command.info()));

        return new BrandingConfig(skillTemplate, barTemplate, abilitiesTemplate, abilityType,
                abilities, levelUp, abilityUnlock, abilityFeedback, gui, guideBook, bossBar, command);
    }

    private static BarTemplate parseBarTemplate(ConfigurationSection section) {
        if (section == null) return DEFAULT.barTemplate;
        int width = range(section, "width", 1, 200, DEFAULT.barTemplate.width());
        String filled = nonBlank(section, "filled", DEFAULT.barTemplate.filled());
        String empty = nonBlank(section, "empty", DEFAULT.barTemplate.empty());
        String start = nonBlank(section, "start", DEFAULT.barTemplate.start());
        String end = nonBlank(section, "end", DEFAULT.barTemplate.end());
        return new BarTemplate(width, filled, empty, start, end);
    }

    private static ConfigurationSection sub(ConfigurationSection section, String key) {
        return section == null ? null : section.getConfigurationSection(key);
    }

    private static String str(ConfigurationSection section, String key, String def) {
        if (section == null || !section.contains(key)) return def;
        Object value = section.get(key);
        if (value instanceof String s) return s;
        throw new IllegalArgumentException("branding: '" + key + "' must be a string");
    }

    private static String nonBlank(ConfigurationSection section, String key, String def) {
        String value = str(section, key, def);
        if (value.isBlank()) {
            throw new IllegalArgumentException("branding: '" + key + "' must not be blank");
        }
        return value;
    }

    private static List<String> lines(ConfigurationSection section, String key, List<String> def) {
        if (section == null || !section.contains(key)) return def;
        Object value = section.get(key);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof String s)) {
                    throw new IllegalArgumentException("branding: '" + key + "' must be a list of strings");
                }
                result.add(s);
            }
            return List.copyOf(result);
        }
        throw new IllegalArgumentException("branding: '" + key + "' must be a list of strings");
    }

    private static int range(ConfigurationSection section, String key, int min, int max, int def) {
        if (section == null || !section.contains(key)) return def;
        Object value = section.get(key);
        if (!(value instanceof Number n)) {
            throw new IllegalArgumentException("branding: '" + key + "' must be an integer");
        }
        int parsed = n.intValue();
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException("branding: '" + key + "' must be between " + min + " and " + max);
        }
        return parsed;
    }

    private static BarColor barColor(ConfigurationSection section, String key, BarColor def) {
        if (section == null || !section.contains(key)) return def;
        String value = str(section, key, null);
        try {
            return BarColor.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("branding: '" + key + "' is not a valid BarColor: " + value);
        }
    }

    private static BarStyle barStyle(ConfigurationSection section, String key, BarStyle def) {
        if (section == null || !section.contains(key)) return def;
        String value = str(section, key, null);
        try {
            return BarStyle.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("branding: '" + key + "' is not a valid BarStyle: " + value);
        }
    }

    public SkillTemplate skillTemplate() { return skillTemplate; }
    public BarTemplate barTemplate() { return barTemplate; }
    public AbilitiesTemplate abilitiesTemplate() { return abilitiesTemplate; }
    public AbilityType abilityType() { return abilityType; }
    public AbilityTemplates abilities() { return abilities; }
    public LevelUp levelUp() { return levelUp; }
    public AbilityUnlock abilityUnlock() { return abilityUnlock; }
    public AbilityFeedback abilityFeedback() { return abilityFeedback; }
    public Gui gui() { return gui; }
    public GuideBook guideBook() { return guideBook; }
    public BossBar bossBar() { return bossBar; }
    public Command command() { return command; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrandingConfig that)) return false;
        return skillTemplate.equals(that.skillTemplate)
                && barTemplate.equals(that.barTemplate)
                && abilitiesTemplate.equals(that.abilitiesTemplate)
                && abilityType.equals(that.abilityType)
                && abilities.equals(that.abilities)
                && levelUp.equals(that.levelUp)
                && abilityUnlock.equals(that.abilityUnlock)
                && abilityFeedback.equals(that.abilityFeedback)
                && gui.equals(that.gui)
                && guideBook.equals(that.guideBook)
                && bossBar.equals(that.bossBar)
                && command.equals(that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillTemplate, barTemplate, abilitiesTemplate, abilityType, abilities,
                levelUp, abilityUnlock, abilityFeedback, gui, guideBook, bossBar, command);
    }
}
