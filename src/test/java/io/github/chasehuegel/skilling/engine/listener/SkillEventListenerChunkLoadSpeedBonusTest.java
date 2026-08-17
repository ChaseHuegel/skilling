package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the Exploration Pathfinder path end-to-end: a {@code chunk_load}
 * dispatch to an unlocked player applies the {@code core:speed_bonus} movement
 * modifier, proving the ability fires through the same dispatch that grants
 * Survival's chunk XP.
 */
class SkillEventListenerChunkLoadSpeedBonusTest {

    @TempDir
    Path tempDir;

    private SkillEventListener listener;
    private PlayerProfile profile;
    private Player player;
    private World world;
    private Chunk chunk;
    private List<AttributeModifier> activeModifiers;

    @BeforeEach
    void setUp() throws Exception {
        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(reg -> {});
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("explore.yml"), """
                id: explore
                max_level: 100
                display: { name: "Explore", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: pathfinder
                    display_name: "Pathfinder"
                    unlock_level: 1
                    trigger: "chunk_load"
                    mechanics:
                      - type: "core:speed_bonus"
                        parameters:
                          multiplier: { linear: { base: 1.05, step: 0.001, max: 1.15 } }
                          uuid: { constant: "8d96db25-38ce-5f64-a2ca-60a4bc6bcb4e" }
                          duration: { linear: { base: 20.0, step: 1.0, max: 120.0 } }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();
        profile.setXp("explore", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getClientViewDistance()).thenReturn(10);

        // Record every transient modifier applied to the movement-speed instance.
        activeModifiers = new ArrayList<>();
        AttributeInstance inst = mock(AttributeInstance.class);
        when(inst.getBaseValue()).thenReturn(0.1);
        doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            activeModifiers.removeIf(am -> am.getUniqueId().equals(m.getUniqueId()));
            activeModifiers.add(m);
            return null;
        }).when(inst).addTransientModifier(any(AttributeModifier.class));
        when(player.getAttribute(Attribute.MOVEMENT_SPEED)).thenReturn(inst);
        EntityScheduler scheduler = mock(EntityScheduler.class);
        when(scheduler.runDelayed(any(), any(), any(), anyLong())).thenReturn(mock(ScheduledTask.class));
        when(player.getScheduler()).thenReturn(scheduler);

        world = mock(World.class);
        chunk = mock(Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);
        when(player.getLocation()).thenReturn(new Location(world, 8, 64, 8));
        when(world.getPlayers()).thenReturn(List.of(player));

        MechanicRegistry mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        Skilling plugin = mock(Skilling.class);
        when(plugin.getGlobalXpModifier()).thenReturn(1.0);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("chunk-load-speed-test"));

        listener = new SkillEventListener(plugin, skillManager, profileManager,
                new TagResolver(new CustomTagLoader()),
                new RequirementEngine(new TagResolver(new CustomTagLoader()), new StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new StateFilterRegistry());
    }

    private ChunkLoadEvent chunkEvent() {
        var event = mock(ChunkLoadEvent.class);
        when(event.isNewChunk()).thenReturn(true);
        when(event.getChunk()).thenReturn(chunk);
        return event;
    }

    @Test
    void unlockedPathfinderAppliesSpeedModifierOnChunkLoad() {
        listener.onChunkLoad(chunkEvent());
        assertFalse(activeModifiers.isEmpty(),
                "chunk_load must apply the speed bonus to an unlocked player");
        assertTrue(activeModifiers.stream().allMatch(m ->
                        m.getOperation() == AttributeModifier.Operation.ADD_NUMBER),
                "the speed bonus must be an additive movement modifier");
    }
}
