package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigHandler {

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

            Map<String, Object> web = new LinkedHashMap<>();
            web.put("enabled", config.getBoolean("web.enabled", false));
            web.put("port", config.getInt("web.port", 8082));
            web.put("username", config.getString("web.username", "admin"));
            web.put("password", config.getString("web.password", "skilling"));
            result.put("web", web);

            ctx.json(result);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    public void update(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

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

            Map<String, Object> web = (Map<String, Object>) body.getOrDefault("web", Map.of());
            yamlConfig.set("web.enabled", web.getOrDefault("enabled", false));
            yamlConfig.set("web.port", web.getOrDefault("port", 8082));
            yamlConfig.set("web.username", web.getOrDefault("username", "admin"));
            yamlConfig.set("web.password", web.getOrDefault("password", "skilling"));

            String yamlContent = yamlConfig.saveToString();
            stagingManager.stageConfigFile(yamlContent);
            ctx.json(Map.of("status", "ok"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
