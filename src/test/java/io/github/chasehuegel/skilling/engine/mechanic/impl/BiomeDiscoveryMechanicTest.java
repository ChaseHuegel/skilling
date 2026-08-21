package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link BiomeDiscoveryMechanic} persistence via the join/reload
 * reconcile path (a null event, which re-applies the movement-speed modifier
 * from the persisted discovered-biome count without recording a new biome). The
 * on-discover player path requires a real {@link Biome} instance, which the
 * plain-JUnit registry bootstrap cannot provide, so it is covered deployment-side.
 */
class BiomeDiscoveryMechanicTest {

    private static final String UUID_STR = "3a9c7e14-1b5d-4a6f-8c2e-6d9b0f1a4c87";

    @Test
    void reconcileWithNullEventReappliesFromPersistedCount() {
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID());
        // A previously persisted discovery (three biomes) survives reload.
        profile.putAllProgress(Map.of(BiomeDiscoveryMechanic.PROGRESS_KEY,
                "minecraft:plains,minecraft:desert,minecraft:ocean"));

        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(profile.getPlayerId());
        AttributeInstance speed = mock(AttributeInstance.class);
        // The mechanic resolves movement_speed through the (faked) attribute
        // registry, so stub any requested attribute to the same instance.
        when(p.getAttribute(any(Attribute.class))).thenReturn(speed);
        when(p.getWorld()).thenReturn(mock(World.class));

        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            var plugin = mock(Skilling.class);
            sk.when(Skilling::getInstance).thenReturn(plugin);
            ProfileManager manager = mock(ProfileManager.class);
            when(manager.getProfile(profile.getPlayerId())).thenReturn(profile);
            when(plugin.getProfileManager()).thenReturn(manager);

            // Null event = join/reload reconcile: apply from the stored set.
            assertTrue(new BiomeDiscoveryMechanic().execute(p, Map.of(
                    "attribute", "minecraft:movement_speed",
                    "amount", 0.001,
                    "max_biomes", 25.0,
                    "uuid", UUID_STR), null));
verify(speed).addTransientModifier(any(AttributeModifier.class));
            // A reconcile run (null event) never records the current biome, so the
            // persisted set stays exactly as seeded.
            assertTrue("minecraft:plains,minecraft:desert,minecraft:ocean"
                            .equals(profile.getProgress().get(BiomeDiscoveryMechanic.PROGRESS_KEY)),
                    "a reconcile run must not add a new biome to progress");
    }
    }
}