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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigHandlerBrandingTest {

    @TempDir
    Path tempDir;

    private File configFile() {
        return new File(tempDir.toFile(), "config.yml");
    }

    private void writeConfig(String content) {
        try {
            Files.writeString(configFile().toPath(), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBaseConfig() {
        writeConfig("web:\n  enabled: true\n  port: 8082\n  username: \"admin\"\n  password: \"sekret\"\n");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonArg(Context ctx) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        return (Map<String, Object>) captor.getValue();
    }

    @Test
    void getReturnsBrandingWithEngineDefaults() {
        writeBaseConfig();
        Context ctx = mock(Context.class, RETURNS_SELF);

        new ConfigHandler(mock(StagingManager.class), configFile()).get(ctx);

        Map<String, Object> branding = (Map<String, Object>) jsonArg(ctx).get("branding");
        assertEquals(List.of("&aLevel {level} / {max_level}", "{bar}", "&aXP: {xp_into} / {xp_needed}",
                "{color}Total XP: {xp_total}", "&7▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔", "{lore}", "", "{abilities}"),
                branding.get("skillTemplate"));
        assertEquals(20, ((Map<String, Object>) branding.get("barTemplate")).get("width"));
        assertEquals("&a█", ((Map<String, Object>) branding.get("barTemplate")).get("filled"));
        assertEquals("&6Level up!", ((Map<String, Object>) branding.get("levelUp")).get("title"));
        assertEquals("white", ((Map<String, Object>) branding.get("bossBar")).get("defaultColor"));
    }

    @Test
    void updatePersistsBrandingKeys() {
        writeBaseConfig();
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
                "branding", Map.of(
                        "skillTemplate", List.of("Custom", "Lines"),
                        "barTemplate", Map.of("width", 10, "filled", "&b#", "empty", "&8.", "start", "<", "end", ">"),
                        "levelUp", Map.of("title", "Nice!")),
                "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).json(Map.of("status", "ok"));
        ArgumentCaptor<String> yaml = ArgumentCaptor.forClass(String.class);
        verify(staging).stageConfigFile(yaml.capture());
        assertTrue(yaml.getValue().contains("skill_template"), yaml.getValue());
        assertTrue(yaml.getValue().contains("width: 10"), yaml.getValue());
        assertTrue(yaml.getValue().contains("filled: '&b#'"), yaml.getValue());
        assertTrue(yaml.getValue().contains("title: Nice!"), yaml.getValue());
    }

    @Test
    void updateRejectsOutOfRangeBarWidth() {
        writeBaseConfig();
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
                "branding", Map.of("barTemplate", Map.of("width", 0)),
                "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("bartemplate.width"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateRejectsScalarTemplateList() {
        writeBaseConfig();
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
                "branding", Map.of("skillTemplate", "&aLevel {level}"),
                "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateRejectsInvalidBarColor() {
        writeBaseConfig();
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
                "branding", Map.of("bossBar", Map.of("defaultColor", "not_a_color")),
                "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).status(400);
        assertTrue(jsonArg(ctx).toString().toLowerCase().contains("barcolor"));
        verify(staging, never()).stageConfigFile(anyString());
    }

    @Test
    void updateWithoutBrandingPreservesExistingBranding() {
        writeConfig("branding:\n  skill_template:\n    - \"&aLevel {level} / {max_level}\"\n  bar_template:\n    width: 5\nweb:\n  enabled: true\n  port: 8082\n  username: \"admin\"\n  password: \"sekret\"\n");
        Context ctx = mock(Context.class, RETURNS_SELF);
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(
                "web", Map.of("enabled", true, "port", 8082, "username", "admin", "password", "")));
        StagingManager staging = mock(StagingManager.class);

        new ConfigHandler(staging, configFile()).update(ctx);

        verify(ctx).json(Map.of("status", "ok"));
        ArgumentCaptor<String> yaml = ArgumentCaptor.forClass(String.class);
        verify(staging).stageConfigFile(yaml.capture());
        assertTrue(yaml.getValue().contains("width: 5"), "un-edited branding must survive the merge");
    }
}
