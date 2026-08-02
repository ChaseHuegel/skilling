package io.github.chasehuegel.skilling.web.config;

import org.bukkit.configuration.file.YamlConfiguration;

public record WebConfig(
    boolean enabled,
    int port,
    String username,
    String password,
    String bindAddress
) {
    public static WebConfig load(YamlConfiguration config) {
        return new WebConfig(
            config.getBoolean("web.enabled", false),
            config.getInt("web.port", 8082),
            config.getString("web.username", "admin"),
            config.getString("web.password", "skilling"),
            config.getString("web.bind_address", "0.0.0.0")
        );
    }
}
