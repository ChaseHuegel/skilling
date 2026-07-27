package io.github.chasehuegel.skilling.web;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.web.auth.BasicAuthenticator;
import io.github.chasehuegel.skilling.web.config.WebConfig;
import io.javalin.Javalin;
import java.util.Map;
import java.util.logging.Level;

public final class WebServer {

    private final Skilling plugin;
    private final WebConfig config;
    private final BasicAuthenticator authenticator;
    private Javalin app;

    public WebServer(Skilling plugin, WebConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.authenticator = new BasicAuthenticator(config);
    }

    public void start() {
        if (!config.enabled()) {
            plugin.getLogger().info("Web GUI is disabled. Set web.enabled: true in config.yml to enable.");
            return;
        }

        try {
            app = Javalin.create(javalinConfig -> {
                javalinConfig.staticFiles.add("/web/frontend");
            });

            var routes = app.unsafe.routes;

            routes.before(ctx -> {
                ctx.res().setHeader("Access-Control-Allow-Origin", "*");
                ctx.res().setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                ctx.res().setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            });

            routes.before("/api/*", ctx -> {
                if (ctx.method().name().equals("OPTIONS")) return;
                if (ctx.path().equals("/api/auth/check")) return;
                if (ctx.path().equals("/api/health")) return;
                String auth = ctx.header("Authorization");
                if (auth == null || !authenticator.valid(auth)) {
                    ctx.status(401).json(Map.of(
                        "status", "error",
                        "message", "Invalid credentials"
                    ));
                }
            });

            routes.get("/api/health", ctx -> {
                ctx.json(Map.of("status", "ok", "plugin", "Skilling"));
            });

            routes.get("/api/auth/check", ctx -> {
                String auth = ctx.header("Authorization");
                if (auth == null || !authenticator.valid(auth)) {
                    ctx.status(401).json(Map.of(
                        "status", "error",
                        "message", "Invalid credentials"
                    ));
                } else {
                    ctx.json(Map.of(
                        "status", "ok",
                        "user", config.username()
                    ));
                }
            });

            app.start(config.port());
            plugin.getLogger().info("Web GUI started on port " + config.port());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to start Web GUI on port " + config.port(), e);
        }
    }

    public void stop() {
        if (app != null) {
            app.stop();
            plugin.getLogger().info("Web GUI stopped.");
        }
    }
}
