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

            org.bukkit.configuration.file.YamlConfiguration liveConfig =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);

            var yamlConfig = new org.bukkit.configuration.file.YamlConfiguration();

            Map<String, Object> db = (Map<String, Object>) body.getOrDefault("database", Map.of());
            yamlConfig.set("database.pool_size", db.getOrDefault("poolSize", 10));
            yamlConfig.set("database.wal_mode", db.getOrDefault("walMode", true));

            Map<String, Object> bb = (Map<String, Object>) body.getOrDefault("bossbar", Map.of());
            yamlConfig.set("bossbar.max_active", bb.getOrDefault("maxActive", 2));
            yamlConfig.set("bossbar.fade_ticks", bb.getOrDefault("fadeTicks", 40));

            Map<String, Object> deb = (Map<String, Object>) body.getOrDefault("debouncer", Map.of());
            yamlConfig.set("debouncer.interval_ms", deb.getOrDefault("intervalMs", 500));

            yamlConfig.set("debug_logging", body.getOrDefault("debugLogging", false));

            Map<String, Object> titles = (Map<String, Object>) body.getOrDefault("titles", Map.of());
            yamlConfig.set("titles.stay_duration", titles.getOrDefault("stayDuration", 5000));

            yamlConfig.set("global_xp_modifier", body.getOrDefault("globalXpModifier", 1.0));

            Map<String, Object> cropGrow = (Map<String, Object>) body.getOrDefault("cropGrow", Map.of());
            yamlConfig.set("crop_grow.search_radius", cropGrow.getOrDefault("searchRadius", 10));

            Map<String, Object> skillsGuideBook = (Map<String, Object>) body.getOrDefault("skillsGuideBook", Map.of());
            yamlConfig.set("skills_guide_book.enabled", skillsGuideBook.getOrDefault("enabled", true));

            Map<String, Object> web = (Map<String, Object>) body.getOrDefault("web", Map.of());
            yamlConfig.set("web.enabled", web.getOrDefault("enabled", false));

            // Port/credential changes cannot be applied to the running embedded
            // server; reject them explicitly so admins are not misled into
            // believing their change took effect.
            int currentPort = liveConfig.getInt("web.port", 8082);
            int requestedPort = ((Number) web.getOrDefault("port", currentPort)).intValue();
            if (requestedPort != currentPort) {
                throw new IllegalArgumentException(WEB_RESTART_REQUIRED);
            }
            yamlConfig.set("web.port", requestedPort);

            String currentUsername = liveConfig.getString("web.username", "admin");
            String requestedUsername = String.valueOf(web.getOrDefault("username", currentUsername));
            if (!currentUsername.equals(requestedUsername)) {
                throw new IllegalArgumentException(WEB_RESTART_REQUIRED);
            }
            yamlConfig.set("web.username", requestedUsername);

            Object passwordValue = web.get("password");
            String requestedPassword = passwordValue == null ? "" : String.valueOf(passwordValue);
            String currentPassword = liveConfig.getString("web.password", "skilling");
            if (!requestedPassword.isBlank()) {
                throw new IllegalArgumentException(WEB_RESTART_REQUIRED);
            }
            // Blank password means "keep current"; preserve it so an apply
            // round-trip never resets credentials to the default.
            yamlConfig.set("web.password", currentPassword);

            // Preserve the bind address (not exposed in the editor) so a save
            // round-trip never drops a locally-scoped binding.
            yamlConfig.set("web.bind_address", liveConfig.getString("web.bind_address", "0.0.0.0"));

            String yamlContent = yamlConfig.saveToString();
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
}
