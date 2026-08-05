package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ConfigHandler {

    private static final Logger LOGGER = Logger.getLogger(ConfigHandler.class.getName());

    private static final String WEB_RESTART_REQUIRED =
            "Changing web.port, web.username, or web.password requires a server restart. "
            + "Edit config.yml directly and restart the server; leave the password blank to keep the current value.";

    private final StagingManager stagingManager;
    private final File configFile;

    public ConfigHandler(StagingManager stagingManager, File configFile) {
        this.stagingManager = stagingManager;
        this.configFile = configFile;
    }

    public void get(Context ctx) {
        try {
            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            Map<String, Object> result = new LinkedHashMap<>();

            Map<String, Object> db = new LinkedHashMap<>();
            db.put("poolSize", config.getInt("database.pool_size", 10));
            db.put("walMode", config.getBoolean("database.wal_mode", true));
            result.put("database", db);

            Map<String, Object> bb = new LinkedHashMap<>();
            bb.put("maxActive", config.getInt("bossbar.max_active", 2));
            bb.put("fadeTicks", config.getInt("bossbar.fade_ticks", 40));
            result.put("bossbar", bb);

            Map<String, Object> deb = new LinkedHashMap<>();
            deb.put("intervalMs", config.getInt("debouncer.interval_ms", 500));
            result.put("debouncer", deb);

            result.put("debugLogging", config.getBoolean("debug_logging", false));

            Map<String, Object> titles = new LinkedHashMap<>();
            titles.put("stayDuration", config.getInt("titles.stay_duration", 5000));
            result.put("titles", titles);

            result.put("globalXpModifier", config.getDouble("global_xp_modifier", 1.0));

            Map<String, Object> cropGrow = new LinkedHashMap<>();
            cropGrow.put("searchRadius", config.getInt("crop_grow.search_radius", 10));
            result.put("cropGrow", cropGrow);

            Map<String, Object> skillsGuideBook = new LinkedHashMap<>();
            skillsGuideBook.put("enabled", config.getBoolean("skills_guide_book.enabled", true));
            result.put("skillsGuideBook", skillsGuideBook);

            Map<String, Object> web = new LinkedHashMap<>();
            web.put("enabled", config.getBoolean("web.enabled", false));
            web.put("port", config.getInt("web.port", 8082));
            web.put("username", config.getString("web.username", "admin"));
            web.put("bindAddress", config.getString("web.bind_address", "0.0.0.0"));
            // Never echo the stored credential back to any client.
            web.put("password", "");
            result.put("web", web);

            result.put("branding", readBranding(config));

            ctx.json(result);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to read config.yml", e);
        }
    }

    /**
     * Reads the {@code branding} section with the same defaults the engine uses,
     * so the editor always renders a complete branding form even before the
     * admin has customized it.
     *
     * @param config the loaded plugin config
     * @return the branding map for the API response
     */
    private static Map<String, Object> readBranding(org.bukkit.configuration.file.YamlConfiguration config) {
        Map<String, Object> branding = new LinkedHashMap<>();
        branding.put("skillTemplate", config.getStringList("branding.skill_template").isEmpty()
                ? List.of("&aLevel {level} / {max_level}", "{bar}", "&aXP: {xp_into} / {xp_needed}",
                        "{color}Total XP: {xp_total}", "&7▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔", "{lore}", "", "{abilities}")
                : config.getStringList("branding.skill_template"));

        Map<String, Object> bar = new LinkedHashMap<>();
        bar.put("width", config.getInt("branding.bar_template.width", 20));
        bar.put("filled", config.getString("branding.bar_template.filled", "&a█"));
        bar.put("empty", config.getString("branding.bar_template.empty", "&8█"));
        bar.put("start", config.getString("branding.bar_template.start", "&7["));
        bar.put("end", config.getString("branding.bar_template.end", "&7]"));
        branding.put("barTemplate", bar);

        branding.put("abilitiesTemplate", config.getStringList("branding.abilities_template").isEmpty()
                ? List.of("{ability}", "")
                : config.getStringList("branding.abilities_template"));

        Map<String, Object> abilityType = new LinkedHashMap<>();
        abilityType.put("active", config.getString("branding.ability_type_template.active", "&8Active"));
        abilityType.put("passive", config.getString("branding.ability_type_template.passive", "&8Passive"));
        branding.put("abilityType", abilityType);

        branding.put("abilityLockedTemplate", config.getStringList("branding.ability_locked_template").isEmpty()
                ? List.of("&c❌ {level} &8· {name} &8· {type}", "{lore}")
                : config.getStringList("branding.ability_locked_template"));
        branding.put("abilityUnlockedTemplate", config.getStringList("branding.ability_unlocked_template").isEmpty()
                ? List.of("&a✔ {name} &8· {type}", "{lore}")
                : config.getStringList("branding.ability_unlocked_template"));

        Map<String, Object> levelUp = new LinkedHashMap<>();
        levelUp.put("title", config.getString("branding.level_up.title", "&6Level up!"));
        levelUp.put("subtitle", config.getString("branding.level_up.subtitle", "{color}{name} &aincreased to {level}"));
        levelUp.put("message", config.getString("branding.level_up.message", "&fYou leveled up &a[{name} {level}]"));
        levelUp.put("maxedMessage", config.getString("branding.level_up.maxed_message",
                "&f{player} has reached max level {color}[{name}]"));
        branding.put("levelUp", levelUp);

        Map<String, Object> abilityUnlock = new LinkedHashMap<>();
        abilityUnlock.put("title", config.getString("branding.ability_unlock.title", "&6Unlocked!"));
        abilityUnlock.put("subtitle", config.getString("branding.ability_unlock.subtitle", "&a✔ {name} &8· {type}"));
        abilityUnlock.put("message", config.getString("branding.ability_unlock.message",
                "&fYou unlocked the ability &a[{name} &8· {type}&a]"));
        branding.put("abilityUnlock", abilityUnlock);

        Map<String, Object> abilityFeedback = new LinkedHashMap<>();
        abilityFeedback.put("readyMessage", config.getString("branding.ability_feedback.ready_message",
                "&a✦ {color}{name} &ais ready!"));
        branding.put("abilityFeedback", abilityFeedback);

        Map<String, Object> gui = new LinkedHashMap<>();
        gui.put("title", config.getString("branding.gui.title", "&6Skills"));
        gui.put("prevPage", config.getString("branding.gui.prev_page", "&6◀ Prev Page"));
        gui.put("nextPage", config.getString("branding.gui.next_page", "&6Next Page ▶"));
        gui.put("pageCount", config.getString("branding.gui.page_count", "&7{count} skill(s)"));
        gui.put("skillNameUnlocked", config.getString("branding.gui.skill_name_unlocked", "&a{name}"));
        gui.put("skillNameLocked", config.getString("branding.gui.skill_name_locked", "&7{name} &8· Locked"));
        branding.put("gui", gui);

        Map<String, Object> guideBook = new LinkedHashMap<>();
        guideBook.put("name", config.getString("branding.guide_book.name", "&6Skills Guide"));
        guideBook.put("lore", config.getString("branding.guide_book.lore", "&7Right-click to open your skills"));
        branding.put("guideBook", guideBook);

        Map<String, Object> bossBar = new LinkedHashMap<>();
        bossBar.put("titleFormat", config.getString("branding.boss_bar.title_format", "{color}{name} &7- &f{level}"));
        bossBar.put("defaultColor", config.getString("branding.boss_bar.default_color", "white"));
        bossBar.put("defaultStyle", config.getString("branding.boss_bar.default_style", "solid"));
        branding.put("bossBar", bossBar);

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("header", config.getString("branding.command.header", "&6=== {title} ==="));
        command.put("command", config.getString("branding.command.command", "&e{command}"));
        command.put("description", config.getString("branding.command.description", "&f{description}"));
        command.put("usage", config.getString("branding.command.usage", "&eUsage: {usage}"));
        command.put("success", config.getString("branding.command.success", "&a{message}"));
        command.put("error", config.getString("branding.command.error", "&c{message}"));
        command.put("info", config.getString("branding.command.info", "&7{message}"));
        branding.put("command", command);

        return branding;
    }

    @SuppressWarnings("unchecked")
    public void update(Context ctx) {
        try {
            Map<String, Object> body = WebError.parseBody(ctx, Map.class);

            // Validate every modeled field's type and range up front, mirroring
            // the SPA constraints, so a malformed request is rejected with a 400
            // before anything is written into config.yml.
            validate(body);

            // Merge the editor payload over the LIVE config rather than rebuilding
            // from a whitelist, so keys the editor does not know about (setup.first_run,
            // admin-added sections, future keys) survive the round-trip. Only keys
            // actually present in the body are overwritten; everything else stays.
            org.bukkit.configuration.file.YamlConfiguration liveConfig =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);

            Map<String, Object> db = section(body, "database");
            if (db.containsKey("poolSize")) liveConfig.set("database.pool_size", db.get("poolSize"));
            if (db.containsKey("walMode")) liveConfig.set("database.wal_mode", db.get("walMode"));

            Map<String, Object> bb = section(body, "bossbar");
            if (bb.containsKey("maxActive")) liveConfig.set("bossbar.max_active", bb.get("maxActive"));
            if (bb.containsKey("fadeTicks")) liveConfig.set("bossbar.fade_ticks", bb.get("fadeTicks"));

            Map<String, Object> deb = section(body, "debouncer");
            if (deb.containsKey("intervalMs")) liveConfig.set("debouncer.interval_ms", deb.get("intervalMs"));

            if (body.containsKey("debugLogging")) liveConfig.set("debug_logging", body.get("debugLogging"));

            Map<String, Object> titles = section(body, "titles");
            if (titles.containsKey("stayDuration")) liveConfig.set("titles.stay_duration", titles.get("stayDuration"));

            if (body.containsKey("globalXpModifier")) liveConfig.set("global_xp_modifier", body.get("globalXpModifier"));

            Map<String, Object> cropGrow = section(body, "cropGrow");
            if (cropGrow.containsKey("searchRadius")) liveConfig.set("crop_grow.search_radius", cropGrow.get("searchRadius"));

            Map<String, Object> skillsGuideBook = section(body, "skillsGuideBook");
            if (skillsGuideBook.containsKey("enabled")) liveConfig.set("skills_guide_book.enabled", skillsGuideBook.get("enabled"));

            Map<String, Object> branding = section(body, "branding");
            if (!branding.isEmpty()) {
                writeBranding(liveConfig, branding);
            }

            Map<String, Object> web = section(body, "web");
            if (web.containsKey("enabled")) liveConfig.set("web.enabled", web.get("enabled"));

            // Port/credential changes cannot be applied to the running embedded
            // server; reject them explicitly so admins are not misled into
            // believing their change took effect.
            int currentPort = liveConfig.getInt("web.port", 8082);
            int requestedPort = web.containsKey("port")
                    ? ((Number) web.get("port")).intValue() : currentPort;
            if (requestedPort != currentPort) {
                throw new IllegalArgumentException(WEB_RESTART_REQUIRED);
            }

            String currentUsername = liveConfig.getString("web.username", "admin");
            String requestedUsername = web.containsKey("username")
                    ? String.valueOf(web.get("username")) : currentUsername;
            if (!currentUsername.equals(requestedUsername)) {
                throw new IllegalArgumentException(WEB_RESTART_REQUIRED);
            }

            Object passwordValue = web.get("password");
            String requestedPassword = passwordValue == null ? "" : String.valueOf(passwordValue);
            if (!requestedPassword.isBlank()) {
                throw new IllegalArgumentException(WEB_RESTART_REQUIRED);
            }
            // Blank password means "keep current"; it is already in the live
            // config and survives the merge untouched.

            String yamlContent = liveConfig.saveToString();
            stagingManager.stageConfigFile(yamlContent);
            ctx.json(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        } catch (JsonProcessingException e) {
            WebError.malformedJson(ctx);
        } catch (ClassCastException e) {
            WebError.badRequest(ctx, "Invalid request body shape");
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to stage config.yml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> body, String key) {
        Object raw = body.get(key);
        if (raw == null) return Map.of();
        if (raw instanceof Map<?, ?> map) return (Map<String, Object>) map;
        // A section key present with a non-object shape is a malformed request.
        throw new ClassCastException("section '" + key + "' must be an object");
    }

    /**
     * Validates every modeled config field's type and range before anything is
     * written, mirroring the SPA's input constraints. The web credential fields
     * are exempted here: {@code web.password} may be blank ("keep current") and
     * {@code web.port}/{@code web.username} changes are rejected later by the
     * restart-required check.
     *
     * @param body the parsed request body
     * @throws IllegalArgumentException on a wrong-typed or out-of-range value
     */
    private static void validate(Map<String, Object> body) {
        requireIntInRange(section(body, "database"), "poolSize", 1, 100, "database.poolSize");
        requireBoolean(section(body, "database"), "walMode", "database.walMode");

        requireIntInRange(section(body, "bossbar"), "maxActive", 1, 10, "bossbar.maxActive");
        requireIntInRange(section(body, "bossbar"), "fadeTicks", 0, 200, "bossbar.fadeTicks");

        requireIntInRange(section(body, "debouncer"), "intervalMs", 100, 5000, "debouncer.intervalMs");

        requireBoolean(body, "debugLogging", "debugLogging");

        requireIntInRange(section(body, "titles"), "stayDuration", 1000, 30000, "titles.stayDuration");

        requireDoubleInRange(body, "globalXpModifier", 0.1, 100.0, "globalXpModifier");

        requireIntInRange(section(body, "cropGrow"), "searchRadius", 1, 50, "cropGrow.searchRadius");

        requireBoolean(section(body, "skillsGuideBook"), "enabled", "skillsGuideBook.enabled");

        requireBoolean(section(body, "web"), "enabled", "web.enabled");
        requireIntInRange(section(body, "web"), "port", 1025, 65535, "web.port");

        validateBranding(body);
    }

    private static void validateBranding(Map<String, Object> body) {
        Map<String, Object> branding = section(body, "branding");
        if (branding.isEmpty()) return;

        stringList(branding, "skillTemplate");
        stringList(branding, "abilitiesTemplate");
        stringList(branding, "abilityLockedTemplate");
        stringList(branding, "abilityUnlockedTemplate");

        Map<String, Object> bar = section(branding, "barTemplate");
        requireIntInRange(bar, "width", 1, 200, "branding.barTemplate.width");
        requireNonBlankString(bar, "filled", "branding.barTemplate.filled");
        requireNonBlankString(bar, "empty", "branding.barTemplate.empty");
        requireNonBlankString(bar, "start", "branding.barTemplate.start");
        requireNonBlankString(bar, "end", "branding.barTemplate.end");

        Map<String, Object> abilityType = section(branding, "abilityType");
        requireString(abilityType, "active", "branding.abilityType.active");
        requireString(abilityType, "passive", "branding.abilityType.passive");

        Map<String, Object> levelUp = section(branding, "levelUp");
        requireString(levelUp, "title", "branding.levelUp.title");
        requireString(levelUp, "subtitle", "branding.levelUp.subtitle");
        requireString(levelUp, "message", "branding.levelUp.message");
        requireString(levelUp, "maxedMessage", "branding.levelUp.maxedMessage");

        Map<String, Object> abilityUnlock = section(branding, "abilityUnlock");
        requireString(abilityUnlock, "title", "branding.abilityUnlock.title");
        requireString(abilityUnlock, "subtitle", "branding.abilityUnlock.subtitle");
        requireString(abilityUnlock, "message", "branding.abilityUnlock.message");

        Map<String, Object> abilityFeedback = section(branding, "abilityFeedback");
        requireString(abilityFeedback, "readyMessage", "branding.abilityFeedback.readyMessage");

        Map<String, Object> gui = section(branding, "gui");
        requireString(gui, "title", "branding.gui.title");
        requireString(gui, "prevPage", "branding.gui.prevPage");
        requireString(gui, "nextPage", "branding.gui.nextPage");
        requireString(gui, "pageCount", "branding.gui.pageCount");
        requireString(gui, "skillNameUnlocked", "branding.gui.skillNameUnlocked");
        requireString(gui, "skillNameLocked", "branding.gui.skillNameLocked");

        Map<String, Object> guideBook = section(branding, "guideBook");
        requireString(guideBook, "name", "branding.guideBook.name");
        requireString(guideBook, "lore", "branding.guideBook.lore");

        Map<String, Object> bossBar = section(branding, "bossBar");
        requireString(bossBar, "titleFormat", "branding.bossBar.titleFormat");
        requireBarColor(bossBar, "defaultColor");
        requireBarStyle(bossBar, "defaultStyle");

        Map<String, Object> command = section(branding, "command");
        requireString(command, "header", "branding.command.header");
        requireString(command, "command", "branding.command.command");
        requireString(command, "description", "branding.command.description");
        requireString(command, "usage", "branding.command.usage");
        requireString(command, "success", "branding.command.success");
        requireString(command, "error", "branding.command.error");
        requireString(command, "info", "branding.command.info");
    }

    @SuppressWarnings("unchecked")
    private static void writeBranding(org.bukkit.configuration.file.YamlConfiguration liveConfig,
                                      Map<String, Object> branding) {
        if (branding.containsKey("skillTemplate")) {
            liveConfig.set("branding.skill_template", stringList(branding, "skillTemplate"));
        }

        Map<String, Object> bar = section(branding, "barTemplate");
        if (bar.containsKey("width")) {
            liveConfig.set("branding.bar_template.width", ((Number) bar.get("width")).intValue());
        }
        writeStringMap(liveConfig, bar, Map.of(
                "filled", "branding.bar_template.filled",
                "empty", "branding.bar_template.empty",
                "start", "branding.bar_template.start",
                "end", "branding.bar_template.end"));

        if (branding.containsKey("abilitiesTemplate")) {
            liveConfig.set("branding.abilities_template", stringList(branding, "abilitiesTemplate"));
        }
        writeStringMap(liveConfig, section(branding, "abilityType"), Map.of(
                "active", "branding.ability_type_template.active",
                "passive", "branding.ability_type_template.passive"));
        if (branding.containsKey("abilityLockedTemplate")) {
            liveConfig.set("branding.ability_locked_template", stringList(branding, "abilityLockedTemplate"));
        }
        if (branding.containsKey("abilityUnlockedTemplate")) {
            liveConfig.set("branding.ability_unlocked_template", stringList(branding, "abilityUnlockedTemplate"));
        }

        writeStringMap(liveConfig, section(branding, "levelUp"), Map.of(
                "title", "branding.level_up.title",
                "subtitle", "branding.level_up.subtitle",
                "message", "branding.level_up.message",
                "maxedMessage", "branding.level_up.maxed_message"));
        writeStringMap(liveConfig, section(branding, "abilityUnlock"), Map.of(
                "title", "branding.ability_unlock.title",
                "subtitle", "branding.ability_unlock.subtitle",
                "message", "branding.ability_unlock.message"));
        writeStringMap(liveConfig, section(branding, "abilityFeedback"), Map.of(
                "readyMessage", "branding.ability_feedback.ready_message"));
        writeStringMap(liveConfig, section(branding, "gui"), Map.of(
                "title", "branding.gui.title",
                "prevPage", "branding.gui.prev_page",
                "nextPage", "branding.gui.next_page",
                "pageCount", "branding.gui.page_count",
                "skillNameUnlocked", "branding.gui.skill_name_unlocked",
                "skillNameLocked", "branding.gui.skill_name_locked"));
        writeStringMap(liveConfig, section(branding, "guideBook"), Map.of(
                "name", "branding.guide_book.name",
                "lore", "branding.guide_book.lore"));
        writeStringMap(liveConfig, section(branding, "bossBar"), Map.of(
                "titleFormat", "branding.boss_bar.title_format",
                "defaultColor", "branding.boss_bar.default_color",
                "defaultStyle", "branding.boss_bar.default_style"));
        writeStringMap(liveConfig, section(branding, "command"), Map.of(
                "header", "branding.command.header",
                "command", "branding.command.command",
                "description", "branding.command.description",
                "usage", "branding.command.usage",
                "success", "branding.command.success",
                "error", "branding.command.error",
                "info", "branding.command.info"));
    }

    private static void writeStringMap(org.bukkit.configuration.file.YamlConfiguration config,
                                       Map<String, Object> section, Map<String, String> mappings) {
        for (var entry : mappings.entrySet()) {
            if (section.containsKey(entry.getKey())) {
                config.set(entry.getValue(), (String) section.get(entry.getKey()));
            }
        }
    }

    private static void requireString(Map<String, Object> section, String key, String path) {
        if (!section.containsKey(key)) return;
        if (!(section.get(key) instanceof String)) {
            throw new IllegalArgumentException(path + " must be a string");
        }
    }

    private static void requireNonBlankString(Map<String, Object> section, String key, String path) {
        if (!section.containsKey(key)) return;
        if (!(section.get(key) instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException(path + " must be a non-blank string");
        }
    }

    private static void requireBarColor(Map<String, Object> section, String key) {
        if (!section.containsKey(key)) return;
        if (!(section.get(key) instanceof String s)) {
            throw new IllegalArgumentException("branding.bossBar." + key + " must be a string");
        }
        try {
            org.bukkit.boss.BarColor.valueOf(s.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("branding.bossBar." + key + " is not a valid BarColor: " + s);
        }
    }

    private static void requireBarStyle(Map<String, Object> section, String key) {
        if (!section.containsKey(key)) return;
        if (!(section.get(key) instanceof String s)) {
            throw new IllegalArgumentException("branding.bossBar." + key + " must be a string");
        }
        try {
            org.bukkit.boss.BarStyle.valueOf(s.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("branding.bossBar." + key + " is not a valid BarStyle: " + s);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Map<String, Object> map, String key) {
        Object raw = map.get(key);
        if (raw == null) return null;
        if (raw instanceof List<?> list) {
            List<String> result = new java.util.ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof String s)) {
                    throw new IllegalArgumentException("branding." + key + " must be a list of strings");
                }
                result.add(s);
            }
            return result;
        }
        throw new IllegalArgumentException("branding." + key + " must be a list of strings");
    }

    private static void requireBoolean(Map<String, Object> section, String key, String path) {
        if (!section.containsKey(key)) return;
        if (!(section.get(key) instanceof Boolean)) {
            throw new IllegalArgumentException(path + " must be a boolean");
        }
    }

    private static void requireIntInRange(Map<String, Object> section, String key,
                                          int min, int max, String path) {
        if (!section.containsKey(key)) return;
        Object raw = section.get(key);
        if (!(raw instanceof Number n)) {
            throw new IllegalArgumentException(path + " must be a number");
        }
        int value = n.intValue();
        if (value < min || value > max) {
            throw new IllegalArgumentException(path + " must be between " + min + " and " + max);
        }
    }

    private static void requireDoubleInRange(Map<String, Object> body, String key,
                                             double min, double max, String path) {
        if (!body.containsKey(key)) return;
        Object raw = body.get(key);
        if (!(raw instanceof Number n)) {
            throw new IllegalArgumentException(path + " must be a number");
        }
        double value = n.doubleValue();
        if (value < min || value > max) {
            throw new IllegalArgumentException(path + " must be between " + min + " and " + max);
        }
    }
}
