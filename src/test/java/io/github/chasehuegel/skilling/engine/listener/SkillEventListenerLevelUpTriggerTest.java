package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.event.SkillingLevelUpEvent;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.github.chasehuegel.skilling.engine.trigger.impl.LevelUpTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SkillEventListenerLevelUpTriggerTest {

    public static class CountingMechanic implements SkillMechanic {
        public static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EXECUTIONS.incrementAndGet();
            return true;
        }
    }

    @TempDir
    Path tempDir;

    private SkillEventListener listener;
    private PlayerProfile profile;
    private Player player;

    @BeforeEach
    void setUp() throws IOException {
        CountingMechanic.EXECUTIONS.set(0);

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:count", CountingMechanic.class, java.util.List.of()));

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test_skill
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: level_up_ability
                    display_name: "Level Up Ability"
                    unlock_level: 1
                    trigger: "level_up"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();
        profile.setXp("test_skill", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:count", CountingMechanic.class, java.util.List.of());

        listener = new SkillEventListener(mock(io.github.chasehuegel.skilling.Skilling.class),
                skillManager, profileManager, new TagResolver(new CustomTagLoader()),
                new io.github.chasehuegel.skilling.engine.requirements.RequirementEngine(
                        new TagResolver(new CustomTagLoader()), new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry());
    }

    @Test
    void levelUpTriggerDeclaresSkillingLevelUpEvent() {
        assertEquals(SkillingLevelUpEvent.class, new LevelUpTrigger().getEventClass());
    }

    @Test
    void skillingLevelUpEventDispatchesLevelUpAbility() {
        var levelUpEvent = new SkillingLevelUpEvent(player, "test_skill", 5);
        listener.fireAbilities(player, profile, levelUpEvent, "level_up");
        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "a trigger: level_up ability must fire on a Skilling level-up event");
    }

    @Test
    void vanillaLevelChangeNoLongerDispatchesLevelUp() throws Exception {
        // The level_up key must not be bound to vanilla Minecraft level changes.
        assertThrows(NoSuchMethodException.class,
                () -> SkillEventListener.class.getMethod("onLevelUp", PlayerLevelChangeEvent.class));
    }

    @Test
    void levelUpXpSourceCannotCascadeBeyondOneLevelPerEvent() throws Exception {
        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:count", CountingMechanic.class, java.util.List.of()));

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("cascade.yml"), """
                id: cascade_skill
                max_level: 100
                display: { name: "Cascade", color: "GREEN", style: "SOLID" }
                progression: { curve: "linear", base_xp: 100.0 }
                xp_sources:
                  - trigger: "block_break"
                    reward: { constant: 50.0 }
                  - trigger: "level_up"
                    reward: { constant: 100000.0 }
                abilities:
                  - id: level_up_ability
                    display_name: "Level Up Ability"
                    unlock_level: 1
                    trigger: "level_up"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();
        // Linear base_xp 100 / step 10: 150 XP is level 6 (thresholds 100..150),
        // and +50 (block_break) lands at level 11 in one grant.
        profile.setXp("cascade_skill", 150);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(new org.bukkit.Location(mock(org.bukkit.World.class), 0, 0, 0));

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:count", CountingMechanic.class, java.util.List.of());

        io.github.chasehuegel.skilling.Skilling plugin = mock(io.github.chasehuegel.skilling.Skilling.class);
        when(plugin.getGlobalXpModifier()).thenReturn(1.0);
        when(plugin.getServer()).thenReturn(mock(org.bukkit.Server.class));
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("level-up-cascade-test"));

        listener = new SkillEventListener(plugin, skillManager, profileManager,
                new TagResolver(new CustomTagLoader()),
                new io.github.chasehuegel.skilling.engine.requirements.RequirementEngine(
                        new TagResolver(new CustomTagLoader()), new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry());

        var block = mock(org.bukkit.block.Block.class);
        when(block.getType()).thenReturn(org.bukkit.Material.STONE);
        when(block.hasMetadata(any())).thenReturn(false);
        var event = mock(org.bukkit.event.block.BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher> dispatcher =
                     mockStatic(io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher.class)) {
            when(Bukkit.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            when(Bukkit.getScheduler()).thenReturn(mock(org.bukkit.scheduler.BukkitScheduler.class));

            listener.onBlockBreak(event);
        }

        // The +50 block_break reward takes the skill 6 -> 11 and fires one
        // level_up event. The +100000 level_up reward would leap to max level;
        // it must instead advance exactly one more level (11 -> 12) and never
        // cascade. 219 XP is the largest total that still resolves to level 12
        // on a 100/10 linear curve.
        assertEquals(219, profile.getXp("cascade_skill"),
                "the level_up reward must be clamped to a single level advancement");
        assertEquals(12, skillManager.getSkill("cascade_skill").getLevelForXp(profile.getXp("cascade_skill")));
        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "the level_up trigger must dispatch exactly once per actual level-up");
    }
}
