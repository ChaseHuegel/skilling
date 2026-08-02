package io.github.chasehuegel.skilling.command;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.command.SkillsCommand;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SkillsCommandPageCacheTest {

    @TempDir
    Path tempDir;

    private ProfileManager profileManager;
    private PlayerProfile profile;
    private Player player;
    private final UUID uuid = UUID.randomUUID();
    private SkillsCommand command;

    @BeforeEach
    void setUp() throws IOException {
        var evaluatorRegistry = new EvaluatorRegistry();
        evaluatorRegistry.register("linear", new LinearEvaluator(0, 1, 0, Double.MAX_VALUE));
        evaluatorRegistry.register("constant", new ConstantEvaluator(0));
        evaluatorRegistry.register("milestone", new MilestoneEvaluator(new TreeMap<>()));
        evaluatorRegistry.register("polynomial", new PolynomialEvaluator(50, 2.5));

        SkillManager skillManager = new SkillManager(
                evaluatorRegistry, new MechanicRegistry(), new TriggerRegistry(),
                new TagResolver(new CustomTagLoader()));

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("mining.yml"), """
            id: mining
            max_level: 100
            display: { name: "Mining", color: "GREEN", style: "SOLID" }
            progression: { curve: "constant", base_xp: 100.0 }
            """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        profileManager = new ProfileManager(db);
        profile = profileManager.loadProfile(uuid).join();

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        command = new SkillsCommand(mock(Skilling.class), skillManager, profileManager,
                mock(SkillMenuBuilder.class), mock(LockdownManager.class), new BossBarPool(2, 40));
    }

    private void setCachedPages() {
        profile.setCachedPageInventories(Map.of(0, mock(Inventory.class)));
        assertNotNull(profile.getCachedPageInventories());
    }

    @Test
    void setLevelInvalidatesPageCache() throws Exception {
        // Pre-level to max so the command's level-up broadcast path is skipped.
        profile.setXp("mining", 10000);
        setCachedPages();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPlayer("chase")).thenReturn(player);
            Method m = SkillsCommand.class.getDeclaredMethod(
                    "setLevel", CommandSender.class, String.class, String.class, int.class);
            m.setAccessible(true);
            m.invoke(command, mock(CommandSender.class), "chase", "mining", 1);
        }

        assertNull(profile.getCachedPageInventories(), "setlevel must invalidate the page cache");
    }

    @Test
    void resetInvalidatesPageCache() throws Exception {
        profile.setXp("mining", 5000);
        setCachedPages();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getPlayer("chase")).thenReturn(player);
            Method m = SkillsCommand.class.getDeclaredMethod(
                    "reset", CommandSender.class, String.class, String.class);
            m.setAccessible(true);
            m.invoke(command, mock(CommandSender.class), "chase", "mining");
        }

        assertNull(profile.getCachedPageInventories(), "reset must invalidate the page cache");
    }
}
