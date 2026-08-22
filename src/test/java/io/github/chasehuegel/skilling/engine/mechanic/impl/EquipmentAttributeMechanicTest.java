package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link EquipmentAttributeMechanic}: it grants a level-scaled attribute
 * only while the player wears the configured {@code equip_tag} armor set, strips
 * it the moment the armor comes off, and reconciles from captured state on
 * inventory changes.
 */
class EquipmentAttributeMechanicTest {

    private static final UUID UUID_1 = UUID.fromString("3f2b9c4a-1e5d-4a6b-8c7d-9e0f1a2b3c4d");
    private static final String ARMOR_TAG = "#c:heavy_armor";

    private static final EnumSet<Material> HEAVY_SET = EnumSet.of(
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS);

    @AfterEach
    void tearDown() {
        EquipmentAttributeMechanic.setTagResolver(null);
        EquipmentAttributeMechanic.stripAll();
    }

    private static Player armoredPlayer(List<AttributeModifier> active) {
        return playerWithArmor(active, HEAVY_SET);
    }

    private static Player playerWithArmor(List<AttributeModifier> active, EnumSet<Material> worn) {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);

        AttributeInstance inst = mock(AttributeInstance.class);
        when(player.getAttribute(any())).thenReturn(inst);
        when(inst.getModifier(any(Key.class))).thenAnswer(inv -> {
            Key key = inv.getArgument(0);
            return active.stream().filter(m -> m.getKey().equals(key)).findFirst().orElse(null);
        });
        when(inst.getModifiers()).thenReturn(active);
        org.mockito.Mockito.doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            active.removeIf(am -> am.getKey().equals(m.getKey()));
            active.add(m);
            return null;
        }).when(inst).addTransientModifier(any(AttributeModifier.class));
        org.mockito.Mockito.doAnswer(inv -> {
            Key key = inv.getArgument(0);
            active.removeIf(am -> am.getKey().equals(key));
            return null;
        }).when(inst).removeModifier(any(Key.class));
        org.mockito.Mockito.doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            active.removeIf(am -> am.getKey().equals(m.getKey()));
            return null;
        }).when(inst).removeModifier(any(AttributeModifier.class));

        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        String[] names = {"_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS"};
        ItemStack[] contents = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            contents[i] = mock(ItemStack.class);
            when(contents[i].getType()).thenReturn(wornPieceFor(worn, names[i]));
        }
        when(inv.getArmorContents()).thenReturn(contents);
        TagResolver resolver = mock(TagResolver.class);
        when(resolver.resolve(ARMOR_TAG)).thenReturn(HEAVY_SET);
        EquipmentAttributeMechanic.setTagResolver(resolver);
        return player;
    }

    private static Material wornPieceFor(EnumSet<Material> worn, String slotSuffix) {
        if (worn.isEmpty()) return Material.AIR;
        switch (slotSuffix) {
            case "_HELMET":     return Material.DIAMOND_HELMET;
            case "_CHESTPLATE": return Material.DIAMOND_CHESTPLATE;
            case "_LEGGINGS":   return Material.DIAMOND_LEGGINGS;
            default:            return Material.DIAMOND_BOOTS;
        }
    }

    @Test
    void executeGrantsModifierWhenArmored() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = armoredPlayer(active);

        assertTrue(new EquipmentAttributeMechanic().execute(
                player, Map.of("attribute", "minecraft:armor", "amount", 5.0,
                        "uuid", UUID_1.toString(), "equip_tag", ARMOR_TAG), null));
        assertEquals(1, active.size(), "an armored player must be granted the bonus");
        assertEquals(5.0, active.get(0).getAmount());
    }

    @Test
    void executeDoesNotGrantWhenUnarmored() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = playerWithArmor(active, EnumSet.noneOf(Material.class));

        new EquipmentAttributeMechanic().execute(
                player, Map.of("attribute", "minecraft:armor", "amount", 5.0,
                        "uuid", UUID_1.toString(), "equip_tag", ARMOR_TAG), null);
        assertTrue(active.isEmpty(), "a bare player must not be granted armor");
    }

    @Test
    void reevaluateStripsWhenArmorComesOff() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = armoredPlayer(active);
        new EquipmentAttributeMechanic().execute(
                player, Map.of("attribute", "minecraft:armor", "amount", 5.0,
                        "uuid", UUID_1.toString(), "equip_tag", ARMOR_TAG), null);
        assertEquals(1, active.size());

        // Swap the chestplate for a foreign material, then reconcile.
        when(player.getInventory().getArmorContents()[1].getType()).thenReturn(Material.IRON_CHESTPLATE);
        EquipmentAttributeMechanic.reevaluate(player);

        assertTrue(active.isEmpty(), "removing a heavy piece must strip the bonus");
    }

    @Test
    void bindingCountTracksCapturedMechanics() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = armoredPlayer(active);
        new EquipmentAttributeMechanic().execute(
                player, Map.of("attribute", "minecraft:armor", "amount", 5.0,
                        "uuid", UUID_1.toString(), "equip_tag", ARMOR_TAG), null);

        assertEquals(1, EquipmentAttributeMechanic.bindingCount(player.getUniqueId()));
        EquipmentAttributeMechanic.clear(player.getUniqueId());
        assertEquals(0, EquipmentAttributeMechanic.bindingCount(player.getUniqueId()));
    }

    @Test
    void missingResolverFailsClosed() {
        List<AttributeModifier> active = new ArrayList<>();
        Player player = playerWithArmor(active, HEAVY_SET);
        EquipmentAttributeMechanic.setTagResolver(null);

        new EquipmentAttributeMechanic().execute(
                player, Map.of("attribute", "minecraft:armor", "amount", 5.0,
                        "uuid", UUID_1.toString(), "equip_tag", ARMOR_TAG), null);
        assertTrue(active.isEmpty(), "a missing resolver must never grant");
    }

    @Test
    void missingUuidIsNoOp() {
        Player player = armoredPlayer(new ArrayList<>());
        assertFalse(new EquipmentAttributeMechanic().execute(
                player, Map.of("attribute", "minecraft:armor", "amount", 5.0, "equip_tag", ARMOR_TAG), null));
    }
}