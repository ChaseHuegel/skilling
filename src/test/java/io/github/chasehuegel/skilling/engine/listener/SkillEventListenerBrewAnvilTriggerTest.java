package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
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
import io.github.chasehuegel.skilling.engine.trigger.impl.BrewPotionTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BrewStartTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RepairTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillEventListenerBrewAnvilTriggerTest {

    /** Records the runtime event class of every execution. */
    public static class RecordingMechanic implements SkillMechanic {
        public static final java.util.List<Class<? extends Event>> EVENTS =
                new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EVENTS.add(event.getClass());
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
        RecordingMechanic.EVENTS.clear();

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:record", RecordingMechanic.class, java.util.List.of()));

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test_skill
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: brew_ability
                    display_name: "Brew"
                    unlock_level: 1
                    trigger: "brew_potion"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: repair_ability
                    display_name: "Repair"
                    unlock_level: 1
                    trigger: "repair"
                    mechanics:
                      - { type: "test:record" }
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
        mechReg.register("test:record", RecordingMechanic.class, java.util.List.of());

        listener = new SkillEventListener(mock(io.github.chasehuegel.skilling.Skilling.class),
                skillManager, profileManager, new TagResolver(new CustomTagLoader()),
                new io.github.chasehuegel.skilling.engine.requirements.RequirementEngine(
                        new TagResolver(new CustomTagLoader()), new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry());
    }

    @Test
    void brewEventReachesBrewPotionMechanic() {
        listener.fireAbilities(player, profile, mock(BrewEvent.class), "brew_potion");
        assertEquals(1, RecordingMechanic.EVENTS.size());
        assertEquals(BrewEvent.class, RecordingMechanic.EVENTS.get(0));
    }

    @Test
    void prepareAnvilEventReachesRepairMechanic() {
        listener.fireAbilities(player, profile, mock(PrepareAnvilEvent.class), "repair");
        assertEquals(1, RecordingMechanic.EVENTS.size());
        assertEquals(PrepareAnvilEvent.class, RecordingMechanic.EVENTS.get(0));
    }

    @Test
    void triggersDeclareTheDispatchedEvents() {
        assertEquals(BrewEvent.class, new BrewPotionTrigger().getEventClass());
        assertEquals(BrewingStartEvent.class, new BrewStartTrigger().getEventClass());
        assertEquals(PrepareAnvilEvent.class, new RepairTrigger().getEventClass());
    }

    @Test
    void onPrepareAnvilIgnoresCancelledEvents() throws Exception {
        var method = SkillEventListener.class.getDeclaredMethod("onPrepareAnvil",
                org.bukkit.event.inventory.PrepareAnvilEvent.class);
        var handler = method.getAnnotation(org.bukkit.event.EventHandler.class);
        assertTrue(handler.ignoreCancelled(),
                "onPrepareAnvil must ignore cancelled events so a cancelled anvil interaction never triggers repair");
        assertEquals(org.bukkit.event.EventPriority.MONITOR, handler.priority());
    }
}
