package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTransformEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code cure_villager} trigger handler: it dispatches only for
 * {@code EntityTransformEvent} with reason {@code CURED} on a
 * {@link ZombieVillager}, attributed to the online player recorded by
 * {@code getConversionPlayer()}. Offline initiators, unknown initiators,
 * non-cure transforms, and non-villager origins never dispatch.
 */
class SkillEventListenerCureVillagerTest {

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
    void setUp() throws Exception {
        CountingMechanic.EXECUTIONS.set(0);

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:count", CountingMechanic.class, java.util.List.of()));

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test_skill
                max_level: 100
                display: { name: "Test", color: "PURPLE", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: cured
                    display_name: "Cured"
                    unlock_level: 1
                    trigger: "cure_villager"
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

        var tagResolver = new TagResolver(new CustomTagLoader());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());

        listener = new SkillEventListener(mock(io.github.chasehuegel.skilling.Skilling.class),
                skillManager, profileManager, tagResolver, requirementEngine,
                mechReg, new FeedbackDebouncer(500), mock(BossBarPool.class), new StateFilterRegistry());
    }

    private EntityTransformEvent curedEvent(OfflinePlayer conversionPlayer) {
        EntityTransformEvent event = mock(EntityTransformEvent.class);
        ZombieVillager zombie = mock(ZombieVillager.class);
        when(zombie.getConversionPlayer()).thenReturn(conversionPlayer);
        when(event.getTransformReason()).thenReturn(EntityTransformEvent.TransformReason.CURED);
        when(event.getEntity()).thenReturn(zombie);
        return event;
    }

    private static OfflinePlayer online(Player onlinePlayer) {
        OfflinePlayer offline = mock(OfflinePlayer.class);
        when(offline.getPlayer()).thenReturn(onlinePlayer);
        return offline;
    }

    @Test
    void curedZombieVillagerWithOnlineInitiatorDispatchesCureAbility() {
        listener.onCureVillager(curedEvent(online(player)));
        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "a CURED transform with an online initiator must dispatch cure_villager");
    }

    @Test
    void nonCureTransformDoesNotDispatch() {
        EntityTransformEvent event = curedEvent(online(player));
        when(event.getTransformReason()).thenReturn(EntityTransformEvent.TransformReason.INFECTION);
        listener.onCureVillager(event);
        assertEquals(0, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void nonZombieVillagerOriginDoesNotDispatch() {
        EntityTransformEvent event = mock(EntityTransformEvent.class);
        when(event.getTransformReason()).thenReturn(EntityTransformEvent.TransformReason.CURED);
        when(event.getEntity()).thenReturn(mock(Entity.class));
        listener.onCureVillager(event);
        assertEquals(0, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void offlineConversionPlayerDoesNotDispatch() {
        listener.onCureVillager(curedEvent(offline(player)));
        // offline() below returns an OfflinePlayer whose getPlayer() is null.
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "a cure finished after the initiator logged off must grant nothing");
    }

    @Test
    void unknownConversionPlayerDoesNotDispatch() {
        listener.onCureVillager(curedEvent(null));
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "a cure with no recorded initiator must not throw or dispatch");
    }

    private static OfflinePlayer offline(Player target) {
        OfflinePlayer offline = mock(OfflinePlayer.class);
        when(offline.getPlayer()).thenReturn(null);
        return offline;
    }
}
