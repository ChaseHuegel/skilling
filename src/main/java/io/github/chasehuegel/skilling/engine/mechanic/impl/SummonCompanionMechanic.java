package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;
import java.util.UUID;

/**
 * Summons (or recalls) a player's marked tamed companion to their side from any
 * distance, on a right-click of the air holding the companion's treat (a bone for
 * a wolf, an apple for a horse).
 *
 * <p>This is the "snapshot + respawn" recall model: it never touches the original
 * pet's chunk, so it works when the pet is in an unloaded chunk or another
 * dimension. A live copy is despawned first, then a fresh vanilla tamed pet is
 * spawned from the persisted {@link PetCompanionStore} snapshot beside the player
 * and applied its recorded look and attributes. The snapshot's active UUID is
 * rebound to the new copy, so the {@code EntityLoad} dup-sweep later removes any
 * stray old copy that resurfaces rather than leaving two.
 *
 * <p>The {@code mob_type} parameter (wolf or horse) must match the held treat: a
 * bone summons a wolf and an apple summons a horse. A mismatched treat — which
 * happens when two sibling abilities share the {@code right_click_air} trigger —
 * is a no-op ({@code false}), so the wrong sibling spends no cost. The species
 * must have been tamed and marked first; otherwise the call is a no-op.
 *
 * <p>Per the {@link SkillMechanic} return contract, {@code false} is returned when
 * the mechanic could not act (non-air event, no matching treat, not tamed/marked,
 * no snapshot), so the ability's treat + exhaustion cost is only consumed on a
 * genuine summon.
 *
 * <p><b>YAML key:</b> {@code core:summon_companion}
 * <p><b>Required parameters:</b> {@code mob_type} (constant {@code wolf} or {@code horse})
 */
public final class SummonCompanionMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_AIR) return false;

        PetCompanionStore.Species species = resolveSpecies(params, player);
        if (species == null) return false;

        UUID playerId = player.getUniqueId();
        boolean tamed = species == PetCompanionStore.Species.WOLF
                ? PetCompanionStore.hasTamedWolf(playerId)
                : PetCompanionStore.hasTamedHorse(playerId);
        if (!tamed) return false;

        PetCompanionStore.CompanionSnapshot snapshot = PetCompanionStore.snapshot(playerId, species);
        if (snapshot == null) return false;

        // Despawn a currently-loaded bound copy so the fresh spawn is the only one.
        if (snapshot.boundUuid() != null) {
            org.bukkit.entity.Entity existing = Bukkit.getEntity(UUID.fromString(snapshot.boundUuid()));
            if (existing instanceof LivingEntity le && le.isValid()) {
                le.remove();
            }
        }

        Location spawn = safeSpawn(player);
        if (spawn == null || spawn.getWorld() == null) return false;

        Class<? extends Ageable> clazz = PetCompanionStore.entityClass(species);
        Ageable pet = spawn.getWorld().spawn(spawn, clazz, CreatureSpawnEvent.SpawnReason.CUSTOM, false,
                e -> tamedPet(e, player));
        PetCompanionStore.setSnapshot(playerId, species, PetCompanionStore.rebind(snapshot, pet.getUniqueId()));
        PetCompanionStore.apply(pet, snapshot);
        return true;
    }

    /**
     * Resolves the companion species from the {@code mob_type} parameter and
     * verifies the air-clicked treat matches: bishop bone summons a wolf, an apple
     * summons a horse. Returns null when unmatched or unconfigured.
     *
     * @param params the mechanic parameters
     * @param player the calling player
     * @return the resolved species, or null
     */
    private static PetCompanionStore.Species resolveSpecies(Map<String, Object> params, Player player) {
        String mobType = String.valueOf(params.getOrDefault("mob_type", "")).toLowerCase();
        Material treat = player.getInventory().getItemInMainHand().getType();
        if ("wolf".equals(mobType)) {
            return treat == Material.BONE ? PetCompanionStore.Species.WOLF : null;
        }
        if ("horse".equals(mobType)) {
            return treat == Material.APPLE ? PetCompanionStore.Species.HORSE : null;
        }
        return null;
    }

    /**
     * Tames the freshly spawned pet to the calling player inside the spawn
     * consumer (runs on creation, before the entity is added to the world).
     *
     * @param pet    the spawned pet
     * @param player the owner
     */
    private static void tamedPet(Ageable pet, Player player) {
        if (pet instanceof Tameable tame) {
            tame.setOwner(player);
            tame.setTamed(true);
        }
    }

    /**
     * Finds a non-colliding, grounded block near the player to spawn the pet,
     * scanning upward from the player's feet for the first air block with a solid
     * block beneath. Returns null when none is found within range.
     *
     * @param player the calling player
     * @return a safe spawn location, or null
     */
    private static Location safeSpawn(Player player) {
        Location base = player.getLocation();
        for (int dy = 0; dy < 8; dy++) {
            Location candidate = base.clone().add(0, dy, 0);
            Block at = candidate.getBlock();
            if (!at.getType().isCollidable()) {
                Block below = candidate.clone().subtract(0, 1, 0).getBlock();
                if (below.getType().isCollidable() || dy == 0) {
                    return candidate.add(0.5, 0, 0.5);
                }
            }
        }
        return null;
    }
}