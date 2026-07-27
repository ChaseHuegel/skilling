package io.github.chasehuegel.skilling.web;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.web.auth.BasicAuthenticator;
import io.github.chasehuegel.skilling.web.config.WebConfig;
import io.github.chasehuegel.skilling.web.handler.ConfigHandler;
import io.github.chasehuegel.skilling.web.handler.ReloadHandler;
import io.github.chasehuegel.skilling.web.handler.SkillHandler;
import io.github.chasehuegel.skilling.web.handler.TagHandler;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.Javalin;
import java.io.File;
import java.util.Map;
import java.util.logging.Level;

public final class WebServer {

    private final Skilling plugin;
    private final WebConfig config;
    private final BasicAuthenticator authenticator;
    private final SkillManager skillManager;
    private final StagingManager stagingManager;
    private final LockdownManager lockdownManager;
    private Javalin app;

    public WebServer(Skilling plugin, WebConfig config, SkillManager skillManager, StagingManager stagingManager, LockdownManager lockdownManager) {
        this.plugin = plugin;
        this.config = config;
        this.skillManager = skillManager;
        this.stagingManager = stagingManager;
        this.lockdownManager = lockdownManager;
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
            File skillsDir = new File(plugin.getDataFolder(), "skills");
            var skillHandler = new SkillHandler(skillManager, stagingManager, skillsDir);
            var tagHandler = new TagHandler(stagingManager, new File(plugin.getDataFolder(), "tags.yml"));
            var configHandler = new ConfigHandler(stagingManager, new File(plugin.getDataFolder(), "config.yml"));
            var reloadHandler = new ReloadHandler(stagingManager, lockdownManager);

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

            // Phase 2: Skills CRUD
            routes.get("/api/skills", skillHandler::list);
            routes.get("/api/skills/{id}", skillHandler::get);
            routes.post("/api/skills", skillHandler::create);
            routes.put("/api/skills/{id}", skillHandler::update);
            routes.delete("/api/skills/{id}", skillHandler::delete);

            // Phase 3: Tags, Config, Reload
            routes.get("/api/tags", tagHandler::get);
            routes.put("/api/tags", tagHandler::update);
            routes.get("/api/config", configHandler::get);
            routes.put("/api/config", configHandler::update);
            routes.post("/api/reload", reloadHandler::reload);

            // Staging endpoints
            routes.get("/api/staging/status", ctx -> {
                var status = stagingManager.status();
                ctx.json(Map.of(
                    "hasPendingChanges", status.hasPendingChanges(),
                    "fileCount", status.fileCount(),
                    "files", status.files(),
                    "lastModified", status.lastModified()
                ));
            });

            routes.delete("/api/staging", ctx -> {
                stagingManager.clear();
                ctx.json(Map.of("status", "ok"));
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
