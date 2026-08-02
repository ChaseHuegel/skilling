package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
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
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
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
 * Verifies ISSUE-135's core guarantee: every filter tag/material reference is
 * flattened into the {@link TagResolver} cache at plugin load, so a single event
 * dispatch performs zero tag/material re-resolution (O(1) {@code EnumSet}
 * membership checks only).
 */
class SkillEventListenerNoReResolveTest {

    /** No-op test mechanic that counts executions. */
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

    @Test
    void blockBreakDispatchDoesNotReResolveTags() throws Exception {
        // Custom tag defined with plain materials so resolution works without a server.
        Path tagsFile = tempDir.resolve("tags.yml");
        Files.writeString(tagsFile, "custom_tags:\n  ores:\n    - \"minecraft:coal\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        TagResolver tagResolver = new TagResolver(loader);

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:count", CountingMechanic.class, java.util.List.of()),
                tagResolver);

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test_skill
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: test_ability
                    display_name: "Test Ability"
                    unlock_level: 1
                    trigger: "block_break"
                    requirements: { cooldown: 0 }
                    mechanics:
                      - type: "test:count"
                        filters:
                          - target: "#c:ores"
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        UUID uuid = UUID.randomUUID();
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        PlayerProfile profile = profileManager.loadProfile(uuid).join();
        profile.setXp("test_skill", 10000);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:count", CountingMechanic.class, java.util.List.of());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());

        Skilling plugin = mock(Skilling.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(mock(org.bukkit.scheduler.BukkitScheduler.class));

        SkillEventListener listener = new SkillEventListener(plugin, skillManager, profileManager, tagResolver,
                requirementEngine, mechReg, new FeedbackDebouncer(500), mock(BossBarPool.class),
                new StateFilterRegistry());

        BlockBreakEvent event = mock(BlockBreakEvent.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.COAL);
        when(event.getBlock()).thenReturn(block);

        // Parse-time warmup already flattened the filter reference; a dispatch must
        // only perform O(1) cache lookups.
        long resolutionsBefore = tagResolver.resolutionCount();
        CountingMechanic.EXECUTIONS.set(0);

        listener.fireAbilities(player, profile, event, "block_break");

        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "the #c:ores filter must match the broken coal block");
        assertEquals(resolutionsBefore, tagResolver.resolutionCount(),
                "a dispatch must never re-resolve tags; all flattening happened at load");
    }
}
