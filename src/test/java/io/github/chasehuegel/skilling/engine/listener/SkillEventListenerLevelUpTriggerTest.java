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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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

        var evalReg = new EvaluatorRegistry();
        evalReg.register("linear", new LinearEvaluator(0, 1, 0, Double.MAX_VALUE));
        evalReg.register("constant", new ConstantEvaluator(0));
        evalReg.register("milestone", new MilestoneEvaluator(new java.util.TreeMap<>()));
        evalReg.register("polynomial", new PolynomialEvaluator(50, 2.5));

        SkillManager skillManager = new SkillManager(evalReg, new MechanicRegistry(),
                new TriggerRegistry(), new TagResolver(new CustomTagLoader()));

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
}
