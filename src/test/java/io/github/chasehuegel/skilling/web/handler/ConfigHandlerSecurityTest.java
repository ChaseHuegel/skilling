package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigHandlerSecurityTest {

    @TempDir
    Path tempDir;

    private static final String SECRET = "hunter2-sekret";

    private File configFile() {
        return new File(tempDir.toFile(), "config.yml");
    }

    private void writeConfig(String password, String username, int port) {
        try {
            Files.writeString(configFile().toPath(),
                "web:\n"
                + "  enabled: true\n"
                + "  port: " + port + "\n"
                + "  username: \"" + username + "\"\n"
                + "  password: \"" + password + "\"\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonArg(Context ctx) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        return (Map<String, Object>) captor.getValue();
    }

    @Test
    void getDoesNotExposePlaintextPassword() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);

        new ConfigHandler(mock(StagingManager.class), configFile()).get(ctx);

        Map<String, Object> body = jsonArg(ctx);
        assertFalse(body.toString().contains(SECRET), "plaintext password leaked in response");
        Map<String, Object> web = (Map<String, Object>) body.get("web");
        assertNotNull(web);
        assertEquals("", web.get("password"));
    }

    @Test
    void updateRejectsPasswordChange() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "newpass")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("restart"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateRejectsPortChange() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "web", Map.of("enabled", true, "port", 9000, "username", "admin")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("restart"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateRejectsUsernameChange() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "web", Map.of("enabled", true, "port", 8082, "username", "root")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("restart"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateBlankPasswordPreservesCurrentPassword() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).json(Map.of("status", "ok"));
        ArgumentCaptor<String> yaml = ArgumentCaptor.forClass(String.class);
        verify(staging).stageConfigFile(yaml.capture());
        assertTrue(yaml.getValue().contains("password: " + SECRET), "staged config lost current password");
    }

    @Test
    void updateRoundTripPreservesKeysOutsideTheEditor() {
        // Keys the editor does not model (setup.first_run, an admin-added section)
        // must survive a save instead of being dropped by a whitelist rebuild.
        try {
            Files.writeString(configFile().toPath(),
                "setup:\n"
                + "  first_run: false\n"
                + "admin_custom:\n"
                + "  key: \"value\"\n"
                + "web:\n"
                + "  enabled: true\n"
                + "  port: 8082\n"
                + "  username: \"admin\"\n"
                + "  password: \"" + SECRET + "\"\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).json(Map.of("status", "ok"));
        ArgumentCaptor<String> yaml = ArgumentCaptor.forClass(String.class);
        verify(staging).stageConfigFile(yaml.capture());
        assertTrue(yaml.getValue().contains("first_run: false"), "setup.first_run was dropped");
        assertTrue(yaml.getValue().contains("admin_custom"), "admin-added section was dropped");
    }

    @Test
    void updateRejectsNegativeDebounceInterval() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "debouncer", Map.of("intervalMs", -5),
            "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("debouncer.intervalms"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateRejectsZeroPoolSize() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "database", Map.of("poolSize", 0),
            "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("database.poolsize"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateRejectsWrongTypedBoolean() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "database", Map.of("walMode", "yes"),
            "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("database.walmode"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateRejectsOutOfRangeGlobalXpModifier() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "globalXpModifier", 200.0,
            "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("globalxpmodifier"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void validInRangePayloadStillApplies() {
        writeConfig(SECRET, "admin", 8082);
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
            "database", Map.of("poolSize", 20, "walMode", true),
            "bossbar", Map.of("maxActive", 4, "fadeTicks", 60),
            "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).json(Map.of("status", "ok"));
        ArgumentCaptor<String> yaml = ArgumentCaptor.forClass(String.class);
        verify(staging).stageConfigFile(yaml.capture());
        assertTrue(yaml.getValue().contains("pool_size: 20"));
        assertTrue(yaml.getValue().contains("max_active: 4"));
    }
}
