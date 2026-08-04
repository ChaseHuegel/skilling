package io.github.chasehuegel.skilling.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
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

            ctx.json(result);
        } catch (Exception e) {
            WebError.internal(ctx, LOGGER, "Failed to read config.yml", e);
        }
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
