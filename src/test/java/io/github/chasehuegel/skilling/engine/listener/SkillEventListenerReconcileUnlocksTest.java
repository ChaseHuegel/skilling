package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.mechanic.UnlockMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies join/reload reconciliation of milestone unlocks: persistent unlock
 * mechanics of {@code level_up} abilities whose owning-skill level meets
 * {@code unlock_level} execute exactly once, plain mechanics never run outside
 * their event, and non-{@code level_up} triggers are never reconciled.
 */
class SkillEventListenerReconcileUnlocksTest {

    public static class CountingUnlockMechanic implements UnlockMechanic {
        public static final AtomicInteger EXECUTIONS = new AtomicInteger();
        public static volatile Map<String, Object> LAST_PARAMS;

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EXECUTIONS.incrementAndGet();
            LAST_PARAMS = params;
            return true;
        }
    }

    public static class CountingPlainMechanic implements SkillMechanic {
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
        CountingUnlockMechanic.EXECUTIONS.set(0);
        CountingPlainMechanic.EXECUTIONS.set(0);

        SkillManager skillManager = TestSkillManager.newWith(reg -> {
            reg.register("test:unlock", CountingUnlockMechanic.class, List.of("recipe"));
            reg.register("test:plain", CountingPlainMechanic.class, List.of());
        });

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test_skill
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: milestone_unlock
                    display_name: "Milestone Unlock"
                    unlock_level: 50
                    trigger: "level_up"
                    mechanics:
                      - type: "test:unlock"
                        parameters:
                          recipe: { constant: "minecraft:netherite_pickaxe" }
                    feedback: { notify: { action_bar: false } }
                  - id: milestone_plain
                    display_name: "Milestone Plain"
                    unlock_level: 50
                    trigger: "level_up"
                    mechanics:
                      - type: "test:plain"
                    feedback: { notify: { action_bar: false } }
                """);
        Files.writeString(skillsDir.resolve("trigger.yml"), """
                id: trigger_skill
                max_level: 100
                display: { name: "Trigger", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: block_break_unlock
                    display_name: "Block Break Unlock"
                    unlock_level: 1
                    trigger: "block_break"
                    mechanics:
                      - type: "test:unlock"
                        parameters:
                          recipe: { constant: "minecraft:shield" }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:unlock", CountingUnlockMechanic.class, List.of("recipe"));
        mechReg.register("test:plain", CountingPlainMechanic.class, List.of());

        listener = new SkillEventListener(mock(io.github.chasehuegel.skilling.Skilling.class),
                skillManager, profileManager, new TagResolver(new CustomTagLoader()),
                new RequirementEngine(new TagResolver(new CustomTagLoader()), new StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new StateFilterRegistry());
    }

    @Test
    void pastMilestoneExecutesUnlockButNotPlainMechanics() {
        // constant curve: any XP >= base_xp (100) is level 100 >= 50.
        profile.setXp("test_skill", 10000);
        listener.reconcileMilestoneUnlocks(player, profile);

        assertEquals(1, CountingUnlockMechanic.EXECUTIONS.get(),
                "a satisfied level_up milestone must run its unlock mechanic once");
        assertEquals(Map.of("recipe", "minecraft:netherite_pickaxe"), CountingUnlockMechanic.LAST_PARAMS,
                "reconciliation must evaluate the mechanic's constant parameters");
        assertEquals(0, CountingPlainMechanic.EXECUTIONS.get(),
                "plain mechanics must never run outside their event dispatch");
    }

    @Test
    void belowMilestoneReconcilesNothing() {
        profile.setXp("test_skill", 0); // level 0 < 50
        listener.reconcileMilestoneUnlocks(player, profile);

        assertEquals(0, CountingUnlockMechanic.EXECUTIONS.get());
        assertEquals(0, CountingPlainMechanic.EXECUTIONS.get());
    }

    @Test
    void nonLevelUpTriggerUnlocksAreNotReconciled() {
        // Both skills are past their milestones, but only the level_up-triggered
        // unlock of test_skill qualifies; trigger_skill's block_break unlock must
        // stay untouched until a block_break event actually dispatches it.
        profile.setXp("test_skill", 10000);
        profile.setXp("trigger_skill", 10000); // level 100 >= 1
        listener.reconcileMilestoneUnlocks(player, profile);

        assertEquals(1, CountingUnlockMechanic.EXECUTIONS.get());
    }
}
