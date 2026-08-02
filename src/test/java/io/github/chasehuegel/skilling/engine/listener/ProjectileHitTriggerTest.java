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
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code projectile_hit} trigger dispatches the throwing skill's
 * {@code return_chance} ability on impact, so the {@code core:projectile_return}
 * mechanic actually runs (it is a no-op on {@code launch_projectile}).
 */
class ProjectileHitTriggerTest {

    @TempDir
    Path tempDir;

    private final UUID uuid = UUID.randomUUID();
    private SkillEventListener listener;
    private ProfileManager profileManager;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("throwing.yml"), """
                id: throwing
                max_level: 100
                display: { name: "Throwing", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: return_chance
                    display_name: "Return Chance"
                    unlock_level: 15
                    trigger: "projectile_hit"
                    display: { lore: [ "&a{chance}%&7 chance to recover thrown tridents." ] }
                    mechanics:
                      - type: "core:projectile_return"
                        parameters:
                          chance: { constant: 100.0 }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        profileManager = new ProfileManager(db);
        PlayerProfile profile = profileManager.loadProfile(uuid).join();
        profile.setXp("throwing", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(mock(Location.class));

        var tagResolver = new TagResolver(new CustomTagLoader());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());

        MechanicRegistry mechanicRegistry = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechanicRegistry);

        Skilling plugin = mock(Skilling.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(mock(org.bukkit.scheduler.BukkitScheduler.class));

        listener = new SkillEventListener(plugin, skillManager, profileManager, tagResolver,
                requirementEngine, mechanicRegistry, new FeedbackDebouncer(500),
                mock(BossBarPool.class), new StateFilterRegistry());
    }

    @Test
    void projectileHitDispatchReturnsThrownTrident() {
        var trident = mock(Trident.class);
        when(trident.getShooter()).thenReturn(player);
        var tridentItem = mock(ItemStack.class);
        when(tridentItem.isEmpty()).thenReturn(false);
        var returned = mock(ItemStack.class);
        when(tridentItem.clone()).thenReturn(returned);
        when(trident.getItemStack()).thenReturn(tridentItem);

        var event = mock(ProjectileHitEvent.class);
        when(event.getEntity()).thenReturn(trident);

        listener.onProjectileHitTrigger(event);

        verify(trident).remove();
        verify(player.getWorld()).dropItemNaturally(any(Location.class), eq(returned));
    }

    @Test
    void projectileHitIgnoredForNonPlayerShooter() {
        var trident = mock(Trident.class);
        when(trident.getShooter()).thenReturn(mock(org.bukkit.entity.Zombie.class));

        var event = mock(ProjectileHitEvent.class);
        when(event.getEntity()).thenReturn(trident);

        listener.onProjectileHitTrigger(event);

        verify(trident, org.mockito.Mockito.never()).remove();
    }

    @Test
    void resolveEventMaterialReturnsProjectileMaterialOnHit() {
        var trident = mock(Trident.class);
        when(trident.getType()).thenReturn(org.bukkit.entity.EntityType.TRIDENT);
        var event = mock(ProjectileHitEvent.class);
        when(event.getEntity()).thenReturn(trident);
        assertEquals(org.bukkit.Material.TRIDENT, listener.resolveEventMaterial(event));
    }
}
