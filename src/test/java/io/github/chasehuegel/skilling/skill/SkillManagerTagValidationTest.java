package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies filter/requirement tag and material references fail fast at load,
 * never throwing from inside an event handler at runtime.
 */
class SkillManagerTagValidationTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager(TagResolver resolver) {
        return TestSkillManager.newWith(reg -> {}, resolver);
    }

    private YamlConfiguration skillWithFilterTarget(String target) {
        var config = new YamlConfiguration();
        config.set("id", "test");
        config.set("max_level", 100);
        config.set("progression.curve", "constant");
        config.set("progression.base_xp", 100);
        config.set("xp_sources", java.util.List.of(Map.of(
                "trigger", "block_break",
                "filters", java.util.List.of(Map.of("target", target)),
                "reward", Map.of("constant", 1.0))));
        return config;
    }

    private YamlConfiguration skillWithFilterState(String state) {
        var config = new YamlConfiguration();
        config.set("id", "test");
        config.set("max_level", 100);
        config.set("progression.curve", "constant");
        config.set("progression.base_xp", 100);
        config.set("xp_sources", java.util.List.of(Map.of(
                "trigger", "block_break",
                "filters", java.util.List.of(Map.of("state", state)),
                "reward", Map.of("constant", 1.0))));
        return config;
    }

    @Test
    void unknownMaterialInFilterFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterTarget("minecraft:not_a_real_material")));
        assertTrue(ex.getMessage().contains("Unknown"));
    }

    @Test
    void missingCustomTagFailsLoad() throws Exception {
        var tagsFile = tempDir.resolve("tags.yml").toFile();
        Files.writeString(tagsFile.toPath(), "custom_tags:\n  ores:\n    - \"minecraft:diamond\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile);
        var manager = skillManager(new TagResolver(loader));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterTarget("#c:nonexistent")));
        assertTrue(ex.getMessage().contains("#c:nonexistent"));
    }

    @Test
    void unknownVanillaTagFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            // No tag registered for the key -> unknown.
            when(Bukkit.getTag(any(String.class), any(NamespacedKey.class), eq(Material.class)))
                    .thenReturn(null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> manager.parseSkill(skillWithFilterTarget("#minecraft:nonexistent_tag")));
            assertTrue(ex.getMessage().contains("#minecraft:nonexistent_tag"));
        }
    }

    @Test
    void knownCustomTagPassesLoad() throws Exception {
        var tagsFile = tempDir.resolve("tags.yml").toFile();
        Files.writeString(tagsFile.toPath(), "custom_tags:\n  ores:\n    - \"minecraft:diamond\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile);
        var manager = skillManager(new TagResolver(loader));

        // Parsing a skill filtering on the defined custom tag must succeed.
        var def = manager.parseSkill(skillWithFilterTarget("#c:ores"));
        org.junit.jupiter.api.Assertions.assertEquals(1, def.xpSources().size());
    }

    @Test
    void unknownStateKeyFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterState("is_sneakingg")));
        assertTrue(ex.getMessage().contains("unknown state 'is_sneakingg'"),
                "the typo must be named: " + ex.getMessage());
    }

    @Test
    void knownStateKeyPassesLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        var def = manager.parseSkill(skillWithFilterState("is_sneaking"));
        org.junit.jupiter.api.Assertions.assertEquals(1, def.xpSources().size());
    }

    @Test
    void invalidPlayerPlacedValueFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterState("player_placed:maybe")));
        assertTrue(ex.getMessage().contains("player_placed"),
                "the player_placed rule must be named: " + ex.getMessage());
    }

    @Test
    void invalidBiomeValueFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterState("biome:not!a!valid!key")));
        assertTrue(ex.getMessage().contains("biome"),
                "the biome rule must be named: " + ex.getMessage());
    }

    @Test
    void validPlayerPlacedStatePassesLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        var def = manager.parseSkill(skillWithFilterState("player_placed:false"));
        org.junit.jupiter.api.Assertions.assertEquals(1, def.xpSources().size());
    }

    @Test
    void unknownCauseValueFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterState("cause:explosion")));
        assertTrue(ex.getMessage().contains("cause"),
                "the cause rule must be named: " + ex.getMessage());
    }

    @Test
    void knownCauseValuePassesLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        var def = manager.parseSkill(skillWithFilterState("cause:burn"));
        org.junit.jupiter.api.Assertions.assertEquals(1, def.xpSources().size());
    }

    @Test
    void honeyLevelUnknownComparisonFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterState("honey_level:around:3")));
        assertTrue(ex.getMessage().contains("honey_level"),
                "the honey_level rule must be named: " + ex.getMessage());
    }

    @Test
    void honeyLevelMissingLevelFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterState("honey_level:above")));
        assertTrue(ex.getMessage().contains("honey_level"),
                "the honey_level rule must be named: " + ex.getMessage());
    }

    @Test
    void honeyLevelNonIntegerFailsLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> manager.parseSkill(skillWithFilterState("honey_level:below:lots")));
        assertTrue(ex.getMessage().contains("honey_level"),
                "the honey_level rule must be named: " + ex.getMessage());
    }

    @Test
    void honeyLevelValidPassesLoad() {
        var manager = skillManager(new TagResolver(new CustomTagLoader()));
        var def = manager.parseSkill(skillWithFilterState("honey_level:below:5"));
        org.junit.jupiter.api.Assertions.assertEquals(1, def.xpSources().size());
    }
}
