package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
 * Verifies {@code core:mark_companion} binds a player-owned tamed wolf with a
 * bone on a sneaking right-click, and that every prerequisite miss is a silent
 * no-op (so the actor never spends cost for a wrong gesture).
 */
class MarkCompanionMechanicTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    private final PlayerProfile profile = new PlayerProfile(PLAYER_ID);
    private final ProfileManager manager = mock(ProfileManager.class);
    private final Skilling plugin = plugin(manager);

    private Player setupPlayer(boolean sneaking, Material held) {
        when(manager.getProfile(PLAYER_ID)).thenReturn(profile);
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(PLAYER_ID);
        when(p.isSneaking()).thenReturn(sneaking);
        ItemStack heldStack = mock(ItemStack.class);
        when(heldStack.getType()).thenReturn(held);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(inv.getItemInMainHand()).thenReturn(heldStack);
        when(p.getInventory()).thenReturn(inv);
        return p;
    }

    private static PlayerInteractEntityEvent interact(org.bukkit.entity.LivingEntity target) {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(target);
        return event;
    }

    private static Skilling plugin(ProfileManager manager) {
        Skilling plugin = mock(Skilling.class);
        when(plugin.getProfileManager()).thenReturn(manager);
        return plugin;
    }

    private static Wolf tamedWolf(Player owner) {
        Wolf wolf = mock(Wolf.class);
        when(wolf.isTamed()).thenReturn(true);
        when(wolf.getOwner()).thenReturn(owner);
        when(wolf.getUniqueId()).thenReturn(UUID.randomUUID());
        when(wolf.getAttribute(any(Attribute.class)))
                .thenReturn(mock(AttributeInstance.class));
        return wolf;
    }

    @Test
    void bindsOwnedTamedWolfWithBone() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(true, Material.BONE);
            Wolf wolf = tamedWolf(p);

            assertTrue(new MarkCompanionMechanic().execute(p, Map.of(), interact(wolf)));
            assertTrue(profile.getProgress().containsKey("husbandry.tamed.wolf"),
                    "binding a wolf must set the tamed-wolf flag");
        }
    }

    @Test
    void notSneakingIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(false, Material.BONE);
            Wolf wolf = tamedWolf(p);

            assertFalse(new MarkCompanionMechanic().execute(p, Map.of(), interact(wolf)));
            assertFalse(profile.getProgress().containsKey("husbandry.tamed.wolf"));
        }
    }

    @Test
    void untamedAnimalIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(true, Material.BONE);
            Wolf wolf = mock(Wolf.class);
            when(wolf.isTamed()).thenReturn(false);

            assertFalse(new MarkCompanionMechanic().execute(p, Map.of(), interact(wolf)));
            assertFalse(profile.getProgress().containsKey("husbandry.tamed.wolf"));
        }
    }

    @Test
    void wrongTreatIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            // An apple on a tamed wolf must not mark it.
            Player p = setupPlayer(true, Material.APPLE);
            Wolf wolf = tamedWolf(p);

            assertFalse(new MarkCompanionMechanic().execute(p, Map.of(), interact(wolf)));
            assertFalse(profile.getProgress().containsKey("husbandry.tamed.wolf"));
        }
    }

    @Test
    void nonOwnedTamedAnimalIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(true, Material.BONE);
            Player other = mock(Player.class);
            when(other.getUniqueId()).thenReturn(UUID.randomUUID());
            Wolf wolf = tamedWolf(other);

            assertFalse(new MarkCompanionMechanic().execute(p, Map.of(), interact(wolf)));
            assertFalse(profile.getProgress().containsKey("husbandry.tamed.wolf"));
        }
    }

    @Test
    void nonWolfNonHorseTargetIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(true, Material.BONE);
            org.bukkit.entity.Cow cow = mock(org.bukkit.entity.Cow.class);

            assertFalse(new MarkCompanionMechanic().execute(p, Map.of(), interact(cow)));
        }
    }

    @Test
    void nullOrWrongEventIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(true, Material.BONE);
            assertFalse(new MarkCompanionMechanic().execute(p, Map.of(), null));
            assertFalse(new MarkCompanionMechanic().execute(
                    p, Map.of(), mock(org.bukkit.event.Event.class)));
        }
    }

    @Test
    void successLogsThroughPlugin() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(true, Material.BONE);
            Wolf wolf = tamedWolf(p);

            new MarkCompanionMechanic().execute(p, Map.of(), interact(wolf));
            verify(plugin).debug(ArgumentMatchers.contains("[mark_companion]"));
        }
    }
}