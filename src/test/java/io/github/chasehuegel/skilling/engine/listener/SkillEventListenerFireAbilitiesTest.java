package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillEventListenerFireAbilitiesTest {

    /** No-op test mechanic that counts executions; a static counter is reset per test. */
    public static class CountingMechanic implements SkillMechanic {
        public static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EXECUTIONS.incrementAndGet();
            return true;
        }
    }

    /** Never-activating test mechanic: simulates a mechanic that could not act. */
    public static class NoOpMechanic implements SkillMechanic {
        public static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EXECUTIONS.incrementAndGet();
            return false;
        }
    }

    /** Throwing test mechanic: simulates a mechanic that fails during execution. */
    public static class ThrowingMechanic implements SkillMechanic {
        public static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EXECUTIONS.incrementAndGet();
            throw new IllegalStateException("simulated mechanic failure");
        }
    }

    @TempDir
    Path tempDir;

    private final UUID uuid = UUID.randomUUID();
    private SkillEventListener listener;
    private ProfileManager profileManager;
    private Player player;

    @BeforeEach
    void setUp() {
        CountingMechanic.EXECUTIONS.set(0);
        NoOpMechanic.EXECUTIONS.set(0);
        ThrowingMechanic.EXECUTIONS.set(0);
    }

    private void buildSkillWithAbility(String abilityRequirementsBlock) throws IOException {
        buildSkillWithMechanics(abilityRequirementsBlock, """
                  - { type: "test:count" }
                  - { type: "test:count" }
            """);
    }

    private void buildSkillWithMechanics(String abilityRequirementsBlock, String mechanicsBlock) throws IOException {
        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> {
                    reg.register("test:count", CountingMechanic.class, java.util.List.of());
                    reg.register("test:noop", NoOpMechanic.class, java.util.List.of());
                    reg.register("test:throw", ThrowingMechanic.class, java.util.List.of());
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
                  - id: test_ability
                    display_name: "Test Ability"
                    unlock_level: 1
                    trigger: "block_break"
                """ + abilityRequirementsBlock + """
                    mechanics:
                """ + mechanicsBlock + """
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        profileManager = new ProfileManager(db);
        PlayerProfile profile = profileManager.loadProfile(uuid).join();
        profile.setXp("test_skill", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:count", CountingMechanic.class, java.util.List.of());
        mechReg.register("test:noop", NoOpMechanic.class, java.util.List.of());
        mechReg.register("test:throw", ThrowingMechanic.class, java.util.List.of());

        var tagResolver = new TagResolver(new CustomTagLoader());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());

        Skilling plugin = mock(Skilling.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(mock(org.bukkit.scheduler.BukkitScheduler.class));
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("fire-abilities-test"));

        listener = new SkillEventListener(plugin, skillManager, profileManager, tagResolver,
                requirementEngine, mechReg, new FeedbackDebouncer(500), mock(BossBarPool.class),
                new StateFilterRegistry());
    }

    @Test
    void onBlockBreakClearsStalePlayerPlacedMetadata() throws IOException {
        buildSkillWithAbility("""
                    requirements:
                      cooldown: 10.0
                """);

        var block = mock(org.bukkit.block.Block.class);
        when(block.hasMetadata("player_placed")).thenReturn(true);
        var event = mock(BlockBreakEvent.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);

        listener.onBlockBreak(event);

        // The marker is dropped with the destroyed block so a block regenerating
        // in this spot is not still treated as player-placed.
        verify(block).removeMetadata(eq("player_placed"), any(org.bukkit.plugin.Plugin.class));
    }

    @Test
    void cooldownAbilityExecutesAllMechanicsOnceAndSetsCooldownOnce() throws IOException {
        buildSkillWithAbility("""
                    requirements:
                      cooldown: 10.0
                """);

        BlockBreakEvent event = mock(BlockBreakEvent.class);

        // First activation: cooldown free, both mechanics run.
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");
        assertEquals(2, CountingMechanic.EXECUTIONS.get(), "all mechanics must execute once per activation");

        // Immediate re-fire: the ability is now on cooldown, so nothing runs.
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");
        assertEquals(2, CountingMechanic.EXECUTIONS.get(),
                "a cooldown must gate the whole ability, not just the first mechanic");
    }

    @Test
    void itemCostAbilityDeductsCostExactlyOnce() throws IOException {
        buildSkillWithAbility("""
                    requirements:
                      items:
                        - { action: "cost", tag: "minecraft:coal", amount: 1 }
                """);

        // Give the player 5 coal (mocked, matching RequirementEngineTest).
        org.bukkit.inventory.PlayerInventory inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        ItemStack coal = mock(ItemStack.class);
        when(coal.getType()).thenReturn(Material.COAL);
        when(coal.getAmount()).thenReturn(5);
        ItemStack[] contents = new ItemStack[36];
        contents[0] = coal;
        when(inventory.getContents()).thenReturn(contents);
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = mock(BlockBreakEvent.class);
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");

        assertEquals(2, CountingMechanic.EXECUTIONS.get(), "both mechanics must execute");
        verify(coal, times(1)).setAmount(4);
    }

    @Test
    void noOpMechanicDoesNotConsumeCostOrCooldown() throws IOException {
        buildSkillWithMechanics("""
                    requirements:
                      cooldown: 10.0
                      items:
                        - { action: "cost", tag: "minecraft:coal", amount: 1 }
                """, """
                  - { type: "test:noop" }
            """);

        // Give the player 5 coal (mocked, matching RequirementEngineTest).
        org.bukkit.inventory.PlayerInventory inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        ItemStack coal = mock(ItemStack.class);
        when(coal.getType()).thenReturn(Material.COAL);
        when(coal.getAmount()).thenReturn(5);
        ItemStack[] contents = new ItemStack[36];
        contents[0] = coal;
        when(inventory.getContents()).thenReturn(contents);
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = mock(BlockBreakEvent.class);

        // A mechanic that could not act (returns false) must not spend the
        // cost or apply the cooldown, so the ability stays eligible to fire.
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");

        assertEquals(2, NoOpMechanic.EXECUTIONS.get(),
                "a no-op must not consume, so the ability fires again on the next event");
        verify(coal, never()).setAmount(anyInt());
    }

    @Test
    void throwingMechanicDoesNotAbortDispatchOrConsume() throws IOException {
        buildSkillWithMechanics("""
                    requirements:
                      cooldown: 10.0
                      items:
                        - { action: "cost", tag: "minecraft:coal", amount: 1 }
                """, """
                  - { type: "test:throw" }
            """);

        org.bukkit.inventory.PlayerInventory inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        ItemStack coal = mock(ItemStack.class);
        when(coal.getType()).thenReturn(Material.COAL);
        when(coal.getAmount()).thenReturn(5);
        ItemStack[] contents = new ItemStack[36];
        contents[0] = coal;
        when(inventory.getContents()).thenReturn(contents);
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = mock(BlockBreakEvent.class);

        // A throwing mechanic produces no effect, so the dispatch must complete
        // without spending cost/cooldown, leaving the ability re-triggerable.
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");

        assertEquals(2, ThrowingMechanic.EXECUTIONS.get(),
                "a throwing mechanic must not abort the dispatch");
        verify(coal, never()).setAmount(anyInt());
    }

    @Test
    void throwingMechanicAfterSuccessfulMechanicStillConsumes() throws IOException {
        buildSkillWithMechanics("""
                    requirements:
                      cooldown: 10.0
                      items:
                        - { action: "cost", tag: "minecraft:coal", amount: 1 }
                """, """
                  - { type: "test:count" }
                  - { type: "test:throw" }
            """);

        org.bukkit.inventory.PlayerInventory inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        ItemStack coal = mock(ItemStack.class);
        when(coal.getType()).thenReturn(Material.COAL);
        when(coal.getAmount()).thenReturn(5);
        ItemStack[] contents = new ItemStack[36];
        contents[0] = coal;
        when(inventory.getContents()).thenReturn(contents);
        when(player.getInventory()).thenReturn(inventory);

        BlockBreakEvent event = mock(BlockBreakEvent.class);

        // The throwing mechanic is isolated, but the earlier mechanic DID execute,
        // so the cost and cooldown must still be consumed exactly once.
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");
        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "the successful mechanic must still run");
        assertEquals(1, ThrowingMechanic.EXECUTIONS.get(),
                "the throwing mechanic must be reached and isolated");
        verify(coal, times(1)).setAmount(4);

        // The cooldown was applied by consume, so a second fire does nothing.
        listener.fireAbilities(player, profileManager.getProfile(uuid), event, "block_break");
        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "consume must not be skipped because a later mechanic threw");
    }
}
