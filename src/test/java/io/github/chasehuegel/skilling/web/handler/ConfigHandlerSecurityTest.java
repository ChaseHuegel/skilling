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
}
