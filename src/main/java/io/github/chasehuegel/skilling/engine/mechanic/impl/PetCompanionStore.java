package io.github.chasehuegel.skilling.engine.mechanic.impl;

import com.google.gson.Gson;
import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

/**
 * Persists a player's bound companion(s) in the profile progress store so a
 * companion can be recollected from any distance without the source chunk being
 * loaded (the "snapshot + respawn" recall model).
 *
 * <p>Two persisted flags gate the species prerequisites ({@code has_tamed_wolf},
 * {@code has_tamed_horse}), and a per-species JSON snapshot copies the marked pet's
 * look and attributes so a respawned copy is functionally identical. Both ride the
 * profile's write-behind progress store, so no database schema changes are needed.
 */
public final class PetCompanionStore {

    private static final Gson GSON = new Gson();

    private static final String KEY_TAMED_WOLF = "husbandry.tamed.wolf";
    private static final String KEY_TAMED_HORSE = "husbandry.tamed.horse";
    private static final String KEY_BOUND_WOLF = "husbandry.bound.wolf";
    private static final String KEY_BOUND_HORSE = "husbandry.bound.horse";

    private PetCompanionStore() {}

    /** The species of companion a snapshot describes. */
    public enum Species {
        WOLF, HORSE
    }

    /**
     * Serializable snapshot of a marked companion: enough to reproduce its look
     * and combat-relevant attributes on a respawn. Stored as JSON in the profile
     * progress map.
     */
    record CompanionSnapshot(
            String species,
            String customName,
            double hpScale,          // current health / max health, applied to the respawn cap
            double movementSpeed,
            Horse.Color color,    // null for wolves
            Horse.Style style,    // null for wolves
            double jumpStrength,     // jump strength, 0 when unknown (wolf)
            String saddleItem,       // item key or null
            String armorItem,        // item key or null
            String boundUuid) {      // the currently active copy's UUID, for the dup-sweep

        static CompanionSnapshot capture(LivingEntity pet) {
            String species = pet instanceof Horse ? "HORSE" : "WOLF";
            String name = pet.getCustomName();
            double hpScale = 0.0;
            AttributeInstance maxHp = pet.getAttribute(Attribute.MAX_HEALTH);
            if (maxHp != null && maxHp.getBaseValue() > 0) {
                hpScale = pet.getHealth() / maxHp.getBaseValue();
            }
            double speed = attr(pet, Attribute.MOVEMENT_SPEED);
            if (pet instanceof Horse h) {
                return new CompanionSnapshot(species, name, hpScale, speed,
                        h.getColor(), h.getStyle(), attr(pet, Attribute.JUMP_STRENGTH),
                        itemKey(h.getInventory().getSaddle()),
                        itemKey(h.getInventory().getArmor()),
                        pet.getUniqueId().toString());
            }
            return new CompanionSnapshot(species, name, hpScale, speed,
                    null, null, 0.0, null, null, pet.getUniqueId().toString());
        }

        private static double attr(LivingEntity pet, Attribute attribute) {
            AttributeInstance inst = pet.getAttribute(attribute);
            return inst != null ? inst.getValue() : 0.0;
        }

        private static String itemKey(ItemStack item) {
            return item == null || item.getType().isAir() ? null : item.getType().getKey().asString();
        }
    }

    /**
     * Applies a snapshot to a freshly respawned companion: renames it, scales
     * health by the recorded ratio, restores movement speed and (for a horse) its
     * variant, jump strength, saddle, and armor.
     *
     * @param pet      the respawned companion
     * @param snapshot the captured snapshot
     */
    static void apply(LivingEntity pet, CompanionSnapshot snapshot) {
        if (snapshot == null) return;
        if (snapshot.customName() != null) pet.setCustomName(snapshot.customName());
        applyAttr(pet, Attribute.MOVEMENT_SPEED, snapshot.movementSpeed());
        if (pet instanceof Horse h && snapshot.color() != null) {
            h.setColor(snapshot.color());
            if (snapshot.style() != null) h.setStyle(snapshot.style());
            applyAttr(pet, Attribute.JUMP_STRENGTH, snapshot.jumpStrength());
            if (snapshot.saddleItem() != null) {
                h.getInventory().setSaddle(new ItemStack(parseMaterial(snapshot.saddleItem())));
            }
            if (snapshot.armorItem() != null) {
                h.getInventory().setArmor(new ItemStack(parseMaterial(snapshot.armorItem())));
            }
        }
        if (snapshot.hpScale() > 0) {
            AttributeInstance maxHp = pet.getAttribute(Attribute.MAX_HEALTH);
            if (maxHp != null) pet.setHealth(Math.min(maxHp.getValue(), maxHp.getValue() * snapshot.hpScale()));
        }
    }

    private static void applyAttr(LivingEntity pet, Attribute attribute, double value) {
        if (value <= 0) return;
        AttributeInstance inst = pet.getAttribute(attribute);
        if (inst != null) inst.setBaseValue(Math.max(0, value));
    }

    private static Material parseMaterial(String key) {
        Material material = org.bukkit.Registry.MATERIAL.get(new org.bukkit.NamespacedKey("minecraft", key.split(":", 2)[1]));
        return material != null ? material : Material.AIR;
    }

    // ---- Profile access ----

    private static PlayerProfile profile(UUID playerId) {
        return Skilling.getInstance().getProfileManager().getProfile(playerId);
    }

    /** Whether the player has tamed at least one wolf. */
    public static boolean hasTamedWolf(UUID playerId) {
        PlayerProfile p = profile(playerId);
        return p != null && p.getProgress().containsKey(KEY_TAMED_WOLF);
    }

    /** Whether the player has tamed at least one horse. */
    public static boolean hasTamedHorse(UUID playerId) {
        PlayerProfile p = profile(playerId);
        return p != null && p.getProgress().containsKey(KEY_TAMED_HORSE);
    }

    /** Records that the player has tamed the given species and tracks dirty. */
    static void markTamed(UUID playerId, Species species) {
        PlayerProfile p = profile(playerId);
        if (p == null) return;
        String key = species == Species.WOLF ? KEY_TAMED_WOLF : KEY_TAMED_HORSE;
        if (p.getProgress().putIfAbsent(key, "1") == null) {
            p.markProgressDirty();
        }
    }

    /** Stores the snapshot for the given species. */
    static void setSnapshot(UUID playerId, Species species, CompanionSnapshot snapshot) {
        PlayerProfile p = profile(playerId);
        if (p == null) return;
        String key = species == Species.WOLF ? KEY_BOUND_WOLF : KEY_BOUND_HORSE;
        p.getProgress().put(key, GSON.toJson(snapshot));
        p.markProgressDirty();
    }

    /** Reads the snapshot for the given species, or null if none was captured. */
    public static CompanionSnapshot snapshot(UUID playerId, Species species) {
        PlayerProfile p = profile(playerId);
        if (p == null) return null;
        String key = species == Species.WOLF ? KEY_BOUND_WOLF : KEY_BOUND_HORSE;
        String json = p.getProgress().get(key);
        if (json == null) return null;
        try {
            return GSON.fromJson(json, CompanionSnapshot.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns a copy of the snapshot bound to the given (fresh) UUID, so the
     * dup-sweep recognizes the respawned copy as the canonical pet.
     *
     * @param s     the stored snapshot
     * @param uuid  the freshly spawned copy's UUID
     * @return the rebound snapshot
     */
    static CompanionSnapshot rebind(CompanionSnapshot s, UUID uuid) {
        return new CompanionSnapshot(s.species(), s.customName(), s.hpScale(), s.movementSpeed(),
                s.color(), s.style(), s.jumpStrength(), s.saddleItem(), s.armorItem(), uuid.toString());
    }

    /**
     * Whether the given entity is the currently bound companion for the species,
     * i.e. its UUID matches the stored snapshot's active UUID.
     *
     * @param playerId   the player UUID
     * @param species    the companion species
     * @param entityUuid the loaded entity's UUID
     * @return true when the entity is the canonical bound pet
     */
    public static boolean isCurrent(UUID playerId, Species species, UUID entityUuid) {
        CompanionSnapshot s = snapshot(playerId, species);
        return s != null && s.boundUuid() != null && s.boundUuid().equals(entityUuid.toString());
    }

    /** The {@link Ageable} type class rendered for a species. */
    static Class<? extends Ageable> entityClass(Species species) {
        return species == Species.WOLF ? org.bukkit.entity.Wolf.class : org.bukkit.entity.Horse.class;
    }
}