package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the piety bury-bones flow end to end through the ability pipeline:
 * a right-click on a dirt-like block with a bone in hand runs the
 * {@code core:block_particles} mechanic and consumes exactly one bone, while a
 * left-click (no block target) never consumes.
 */
class SkillEventListenerBuryBonesTest {

    @TempDir
    Path tempDir;

    private SkillEventListener listener;
    private PlayerProfile profile;
    private Player player;
    private ItemStack bone;

    @BeforeEach
    void setUp() throws Exception {
        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test_skill
                max_level: 100
                display: { name: "Test", color: "PURPLE", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: bury_bones
                    display_name: "Bury Bones"
                    unlock_level: 1
                    trigger: "player_interact"
                    requirements:
                      cooldown: 0
                      items:
                        - { action: "cost", tag: "minecraft:bone", amount: 1 }
                    mechanics:
                      - type: "core:block_particles"
                        filters:
                          - target: "#minecraft:dirt"
                          - tool: "minecraft:bone"
                        parameters:
                          particle: { constant: "HAPPY_VILLAGER" }
                          count: { constant: 8 }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        UUID uuid = UUID.randomUUID();
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        profile = profileManager.loadProfile(uuid).join();
        profile.setXp("test_skill", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        PlayerInventory inventory = mock(PlayerInventory.class);
        bone = mock(ItemStack.class);
        when(bone.getType()).thenReturn(Material.BONE);
        when(bone.getAmount()).thenReturn(3);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(bone);
        when(inventory.getItemInMainHand()).thenReturn(bone);
        when(inventory.getContents()).thenReturn(new ItemStack[]{bone});
        when(player.getInventory()).thenReturn(inventory);

        var tagResolver = new TagResolver(new CustomTagLoader());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("core:block_particles",
                io.github.chasehuegel.skilling.engine.mechanic.impl.BlockParticlesMechanic.class,
                java.util.List.of());

        Skilling plugin = mock(Skilling.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(mock(org.bukkit.scheduler.BukkitScheduler.class));

        listener = new SkillEventListener(plugin, skillManager, profileManager, tagResolver,
                requirementEngine, mechReg, new FeedbackDebouncer(500),
                mock(BossBarPool.class), new StateFilterRegistry());
    }

    private PlayerInteractEvent interactWithDirt(Action action) {
        var world = mock(World.class);
        var block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(world, 1, 2, 3));
        when(block.getType()).thenReturn(Material.DIRT);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        when(event.getClickedBlock()).thenReturn(block);
        return event;
    }

    @Test
    void rightClickOnDirtConsumesExactlyOneBone() {
        var event = interactWithDirt(Action.RIGHT_CLICK_BLOCK);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            @SuppressWarnings("unchecked")
            Tag<Material> dirtTag = mock(Tag.class);
            when(dirtTag.getValues()).thenReturn(Set.of(Material.DIRT));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(Material.class)))
                    .thenReturn(dirtTag);
            listener.fireAbilities(player, profile, event, "player_interact");
        }
        verify(bone).setAmount(2);
    }

    @Test
    void leftClickDoesNotConsumeBone() {
        var event = interactWithDirt(Action.LEFT_CLICK_BLOCK);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            @SuppressWarnings("unchecked")
            Tag<Material> dirtTag = mock(Tag.class);
            when(dirtTag.getValues()).thenReturn(Set.of(Material.DIRT));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(Material.class)))
                    .thenReturn(dirtTag);
            listener.fireAbilities(player, profile, event, "player_interact");
        }
        // The block target filter fails for a left-click, so no mechanic runs and
        // the cost is not consumed.
        verify(bone, never()).setAmount(anyInt());
    }
}
