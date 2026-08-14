package io.github.chasehuegel.skilling.command;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.command.SkillsCommand;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies the offline {@code addxp} command does not double-grant XP when the
 * target logs in while the command runs, and that a fully-offline target still
 * receives the full grant on next login.
 */
class SkillsCommandOfflineAddXpTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;
    private DatabaseManager db;
    private SkillsCommand command;
    private final UUID uuid = UUID.randomUUID();

    private void setUp() throws Exception {
        skillManager = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("mining.yml"), """
                id: mining
                max_level: 100
                display: { name: "Mining", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        db = new DatabaseManager(tempDir.toFile());
        var config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("database.wal_mode", false);
        db.initialize(config);
    }

    private SkillsCommand newCommand(ProfileManager profileManager) {
        Skilling plugin = mock(Skilling.class);
        when(plugin.getDatabaseManager()).thenReturn(db);
        return new SkillsCommand(plugin, skillManager, profileManager,
                mock(SkillMenuBuilder.class), mock(LockdownManager.class),
                new BossBarPool(2, 40));
    }

    private void seedDb(long xp) throws Exception {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO player_skills (player_uuid, skill_id, xp, fanfare_pending) VALUES (?, ?, ?, 0)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, "mining");
            stmt.setLong(3, xp);
            stmt.executeUpdate();
        }
    }

    private long readDb() throws Exception {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT xp FROM player_skills WHERE player_uuid = ? AND skill_id = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, "mining");
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    private void invokeOfflineAddXp(CommandSender sender, String playerName, String skillId, int amount) throws Exception {
        Method m = SkillsCommand.class.getDeclaredMethod(
                "handleOfflineAddXp", CommandSender.class, String.class, String.class, int.class);
        m.setAccessible(true);
        m.invoke(command, sender, playerName, skillId, amount);
    }

    private MockedStatic<Bukkit> mockBukkit(String playerName) {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        OfflinePlayer offline = mock(OfflinePlayer.class);
        when(offline.hasPlayedBefore()).thenReturn(true);
        when(offline.getUniqueId()).thenReturn(uuid);
        when(Bukkit.getOfflinePlayer(playerName)).thenReturn(offline);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        // Run both the async body and the main-thread reply synchronously so the
        // test can observe the full command flow.
        when(scheduler.runTaskAsynchronously(any(org.bukkit.plugin.Plugin.class), any(Runnable.class))).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return mock(BukkitTask.class);
        });
        when(scheduler.runTask(any(org.bukkit.plugin.Plugin.class), any(Runnable.class))).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return mock(BukkitTask.class);
        });
        when(Bukkit.getScheduler()).thenReturn(scheduler);
        return bukkit;
    }

    @Test
    void offlineAddXpFoldsIntoLiveProfileWithoutDoubleGrant() throws Exception {
        setUp();
        seedDb(100);

        // The target logs in while the command runs: the first getProfile call
        // (pre-write) sees no profile, the second (post-write) sees the freshly
        // hydrated profile whose baseline already includes the grant.
        ProfileManager profileManager = mock(ProfileManager.class);
        PlayerProfile joined = new PlayerProfile(uuid);
        joined.setXp("mining", 150);
        when(profileManager.getProfile(uuid)).thenReturn(null, joined);
        command = newCommand(profileManager);

        try (MockedStatic<Bukkit> bukkit = mockBukkit("chase")) {
            invokeOfflineAddXp(mock(CommandSender.class), "chase", "mining", 50);
        }

        assertEquals(150L, readDb(), "the DB must hold exactly old + grant");
        assertEquals(150L, joined.getXp("mining"),
                "the live profile must not receive the grant twice");
    }

    @Test
    void offlineAddXpGrantsInFullWhenPlayerStaysOffline() throws Exception {
        setUp();
        seedDb(100);

        ProfileManager profileManager = new ProfileManager(db);
        command = newCommand(profileManager);

        try (MockedStatic<Bukkit> bukkit = mockBukkit("chase")) {
            invokeOfflineAddXp(mock(CommandSender.class), "chase", "mining", 50);
        }

        assertEquals(150L, readDb(), "the offline grant must persist the full amount");
        // On next login the hydration reads the persisted grant.
        PlayerProfile hydrated = profileManager.loadProfile(uuid).join();
        assertEquals(150L, hydrated.getXp("mining"));
    }
}
