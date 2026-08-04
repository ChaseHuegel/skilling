package io.github.chasehuegel.skilling.web.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

/**
 * Immutable snapshot of the {@code web:} section of config.yml, taken at startup.
 */
public record WebConfig(
    boolean enabled,
    int port,
    String username,
    String password,
    String bindAddress,
    boolean behindProxy,
    List<String> allowedOrigins
) {
    public WebConfig {
        allowedOrigins = allowedOrigins != null ? List.copyOf(allowedOrigins) : List.of();
    }

    public static WebConfig load(YamlConfiguration config) {
        return new WebConfig(
            config.getBoolean("web.enabled", false),
            config.getInt("web.port", 8082),
            config.getString("web.username", "admin"),
            config.getString("web.password", "skilling"),
            config.getString("web.bind_address", "0.0.0.0"),
            config.getBoolean("web.behind_proxy", false),
            config.getStringList("web.allowed_origins")
        );
    }
}
