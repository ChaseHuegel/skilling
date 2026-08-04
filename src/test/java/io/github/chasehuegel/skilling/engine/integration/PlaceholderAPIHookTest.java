package io.github.chasehuegel.skilling.engine.integration;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.integration.PlaceholderAPIHook;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceholderAPIHookTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;
    private ProfileManager profileManager;
    private Player player;
    private PlaceholderAPIHook hook;

    @BeforeEach
    void setUp() throws Exception {
        skillManager = TestSkillManager.newBuiltIn();
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("mining.yml"), """
            id: mining
            max_level: 100
            progression: { curve: linear, base_xp: 100 }
            """);
        Files.writeString(skillsDir.resolve("woodcutting.yml"), """
            id: woodcutting
            max_level: 100
            progression: { curve: linear, base_xp: 100 }
            """);
        Files.writeString(skillsDir.resolve("heavy_weapons.yml"), """
            id: heavy_weapons
            max_level: 100
            progression: { curve: linear, base_xp: 100 }
            abilities:
              - id: double_strike
                unlock_level: 1
                trigger: "player_interact"
                mechanics:
                  - type: "core:modify_damage"
                    parameters:
                      multiplier: { constant: 1.5 }
            """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        PlayerProfile profile = profileManager.loadProfile(uuid).join();
        profile.setXp("mining", 150);
        profile.setXp("woodcutting", 115);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        Skilling plugin = mock(Skilling.class);
        when(plugin.getProfileManager()).thenReturn(profileManager);
        when(plugin.getSkillManager()).thenReturn(skillManager);
        hook = new PlaceholderAPIHook(plugin);
    }

    @Test
    void totalLevelsSumsAllSkillLevels() {
        int miningLevel = skillManager.getSkill("mining").getLevelForXp(150);
        int woodcuttingLevel = skillManager.getSkill("woodcutting").getLevelForXp(115);
        assertEquals(String.valueOf(miningLevel + woodcuttingLevel),
                hook.onRequest(player, "total_levels"));
    }

    @Test
    void singleSkillExpansionUnchanged() {
        int miningLevel = skillManager.getSkill("mining").getLevelForXp(150);
        assertEquals(String.valueOf(miningLevel), hook.onRequest(player, "level_mining"));
        assertEquals("150", hook.onRequest(player, "xp_mining"));
    }

    @Test
    void evaluatorPlaceholderResolvesUnderscoreSkillId() {
        assertEquals("1.50",
                hook.onRequest(player, "evaluator_heavy_weapons_double_strike_multiplier"));
    }
}
