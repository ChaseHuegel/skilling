package io.github.chasehuegel.skilling.web;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.web.auth.AuthRateLimiter;
import io.github.chasehuegel.skilling.web.auth.BasicAuthenticator;
import io.github.chasehuegel.skilling.web.config.WebConfig;
import io.github.chasehuegel.skilling.web.handler.ConfigHandler;
import io.github.chasehuegel.skilling.web.handler.GuiLayoutHandler;
import io.github.chasehuegel.skilling.web.handler.ReloadHandler;
import io.github.chasehuegel.skilling.web.handler.SkillHandler;
import io.github.chasehuegel.skilling.web.handler.StateFilterHandler;
import io.github.chasehuegel.skilling.web.handler.TagHandler;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;

public final class WebServer {

    private static final int MAX_AUTH_FAILURES = 10;
    private static final Duration AUTH_WINDOW = Duration.ofMinutes(15);
    private static final Duration AUTH_BLOCK = Duration.ofMinutes(15);

    private final Skilling plugin;
    private final WebConfig config;
    private final BasicAuthenticator authenticator;
    private final AuthRateLimiter rateLimiter;
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
        this.rateLimiter = new AuthRateLimiter(MAX_AUTH_FAILURES, AUTH_WINDOW, AUTH_BLOCK);
    }

    public void start() {
        if (!config.enabled()) {
            plugin.getLogger().info("Web GUI is disabled. Set web.enabled: true in config.yml to enable.");
            return;
        }

        boolean hasFrontend = getClass().getResource("/web/frontend") != null;
        if (!hasFrontend) {
            plugin.getLogger().info("Web GUI frontend not bundled — API-only mode. Build with: cd web/frontend && npm install && npm run build");
        }

        try {
            app = Javalin.create(javalinConfig -> {
                if (hasFrontend) {
                    javalinConfig.staticFiles.add("/web/frontend");
                }
            });

            var routes = app.unsafe.routes;
            File skillsDir = new File(plugin.getDataFolder(), "skills");
            var skillHandler = new SkillHandler(skillManager, stagingManager, skillsDir);
            var tagHandler = new TagHandler(stagingManager, new File(plugin.getDataFolder(), "tags.yml"));
            var configHandler = new ConfigHandler(stagingManager, new File(plugin.getDataFolder(), "config.yml"));
            var reloadHandler = new ReloadHandler(plugin, stagingManager, lockdownManager);

            routes.before(ctx -> {
                ctx.res().setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                ctx.res().setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
                ctx.res().setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                ctx.res().setHeader("Pragma", "no-cache");
                ctx.res().setHeader("Expires", "0");
                // CORS: only reflect an explicitly allowed origin. The frontend is
                // served same-origin, so same-origin requests work without any CORS
                // header; a blanket "*" would let any cross-origin page read admin
                // API responses. Basic auth credentials are origin-scoped and are
                // never attached to cross-origin requests, so refusing the header
                // also prevents cross-origin state changes from being read.
                String origin = ctx.header("Origin");
                if (origin != null && originAllowed(origin, ctx)) {
                    ctx.res().setHeader("Access-Control-Allow-Origin", origin);
                    ctx.res().setHeader("Vary", "Origin");
                }
            });

            routes.before("/api/*", ctx -> {
                if (ctx.method().name().equals("OPTIONS")) return;
                if (ctx.path().equals("/api/auth/check")) return;
                if (ctx.path().equals("/api/health")) return;
                String ip = clientIp(ctx);
                if (rateLimiter.isBlocked(ip)) {
                    ctx.status(429).json(Map.of(
                        "status", "error",
                        "message", "Too many failed attempts. Try again later."
                    ));
                    ctx.skipRemainingHandlers();
                    return;
                }
                String auth = ctx.header("Authorization");
                if (auth == null || !authenticator.valid(auth)) {
                    rateLimiter.recordFailure(ip);
                    ctx.status(401).json(Map.of(
                        "status", "error",
                        "message", "Invalid credentials"
                    ));
                    ctx.skipRemainingHandlers();
                } else {
                    rateLimiter.recordSuccess(ip);
                }
            });

            routes.get("/api/health", ctx -> {
                ctx.json(Map.of("status", "ok", "plugin", "Skilling"));
            });

            routes.get("/api/auth/check", ctx -> {
                String ip = clientIp(ctx);
                if (rateLimiter.isBlocked(ip)) {
                    ctx.status(429).json(Map.of(
                        "status", "error",
                        "message", "Too many failed attempts. Try again later."
                    ));
                    return;
                }
                String auth = ctx.header("Authorization");
                if (auth == null || !authenticator.valid(auth)) {
                    rateLimiter.recordFailure(ip);
                    ctx.status(401).json(Map.of(
                        "status", "error",
                        "message", "Invalid credentials"
                    ));
                } else {
                    rateLimiter.recordSuccess(ip);
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

            // Phase 4: GUI Layout
            var guiLayoutHandler = new GuiLayoutHandler(stagingManager, plugin.getDataFolder());
            routes.get("/api/gui-layout", guiLayoutHandler::get);
            routes.put("/api/gui-layout", guiLayoutHandler::update);

            // Staging endpoints
            routes.get("/api/staging/status", ctx -> {
                var status = stagingManager.status();
                var result = new java.util.LinkedHashMap<String, Object>();
                result.put("hasPendingChanges", status.hasPendingChanges());
                result.put("fileCount", status.fileCount());
                result.put("files", status.files());
                result.put("lastModified", status.lastModified() != null ? status.lastModified() : "");
                ctx.json(result);
            });

            routes.delete("/api/staging", ctx -> {
                stagingManager.clear();
                ctx.json(Map.of("status", "ok"));
            });

            routes.get("/api/mechanics", ctx -> {
                var reg = plugin.getRegistries().getMechanicRegistry();
                var paramMap = reg.getAllParameterNames();
                ctx.json(Map.of("mechanics", paramMap));
            });

            routes.get("/api/triggers", ctx -> {
                var keys = plugin.getRegistries().getTriggerRegistry().keys();
                ctx.json(Map.of("triggers", keys));
            });

            var stateFilterHandler = new StateFilterHandler(plugin);
            routes.get("/api/state-filters", stateFilterHandler::list);

            app.start(config.bindAddress(), config.port());
            plugin.getLogger().info("Web GUI started on " + config.bindAddress() + ":" + config.port());
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

    /**
     * Resolves the client identity for rate limiting. Behind a trusted reverse
     * proxy every socket appears as the proxy, so one client's failures would
     * otherwise lock out everyone; the real client IP is read from the right-most
     * {@code X-Forwarded-For} entry (the address the trusted proxy appended) when
     * {@code behind_proxy} is enabled.
     *
     * @param ctx the current request context
     * @return the client IP to rate-limit on
     */
    private String clientIp(Context ctx) {
        return resolveClientIp(ctx.ip(), ctx.header("X-Forwarded-For"), config.behindProxy());
    }

    /**
     * Resolves the rate-limit identity from the socket IP and optional forwarded
     * header. Package-private and pure so the proxy/IP logic is unit-testable
     * without a live server.
     *
     * @param socketIp     the TCP peer address
     * @param forwardedFor the {@code X-Forwarded-For} header, or null
     * @param behindProxy  whether a trusted reverse proxy is in front
     * @return the identity to rate-limit on
     */
    static String resolveClientIp(String socketIp, String forwardedFor, boolean behindProxy) {
        if (!behindProxy || forwardedFor == null || forwardedFor.isBlank()) return socketIp;
        String[] parts = forwardedFor.split(",");
        String last = parts[parts.length - 1].trim();
        return last.isBlank() ? socketIp : last;
    }

    /**
     * Whether a cross-origin request's Origin header may be reflected back. The
     * API's own origin (the request's Host) is always allowed, plus any origin
     * explicitly listed in {@code web.allowed_origins}. Anything else gets no
     * {@code Access-Control-Allow-Origin} header, so the browser blocks reading
     * the response.
     *
     * @param origin the request's Origin header
     * @param ctx    the current request context
     * @return true if the origin may read the response
     */
    private boolean originAllowed(String origin, Context ctx) {
        if (config.allowedOrigins().contains(origin)) return true;
        try {
            var uri = new java.net.URI(origin);
            String host = uri.getHost();
            if (host == null) return false;
            String hostPort = uri.getPort() == -1 ? host : host + ":" + uri.getPort();
            String requestHost = ctx.header("Host");
            return hostPort.equalsIgnoreCase(requestHost);
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }
}
