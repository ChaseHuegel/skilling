package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.Map;

/**
 * Binds a player's own tamed pet as their "marked" companion so a later
 * {@code core:summon_companion} call can reproduce it from any distance.
 *
 * <p>On a sneaking right-click of a tamed, player-owned pet holding the pet's
 * matching treat (a bone for a wolf, an apple for a horse), the mechanic records
 * a persistent snapshot of the pet (look and attributes) and marks the species as
 * tamed. The interaction is cancelled so marking never mounts a horse or triggers
 * a vanilla feed.
 *
 * <p>The treat must match the species: sneaking with a bone marks a wolf, with an
 * apple marks a horse. A mismatched treat is a no-op ({@code false}), so the
 * marking ability consumes no cost for a wrong target.
 *
 * <p><b>YAML key:</b> {@code core:mark_companion}
 * <p><b>Parameters:</b> none
 */
public final class MarkCompanionMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEntityEvent interact)) return false;
        if (!player.isSneaking()) return false;
        if (!(interact.getRightClicked() instanceof Tameable tame)) return false;
        if (!tame.isTamed()) return false;
        if (!(tame.getOwner() instanceof Player owner) || !owner.getUniqueId().equals(player.getUniqueId())) {
            return false;
        }

        LivingEntity pet = (LivingEntity) tame;
        PetCompanionStore.Species species;
        Material treat = player.getInventory().getItemInMainHand().getType();
        if (pet instanceof Wolf) {
            if (treat != Material.BONE) return false;
            species = PetCompanionStore.Species.WOLF;
        } else if (pet instanceof Horse) {
            if (treat != Material.APPLE) return false;
            species = PetCompanionStore.Species.HORSE;
        } else {
            return false;
        }

        interact.setCancelled(true);
        PetCompanionStore.markTamed(player.getUniqueId(), species);
        PetCompanionStore.setSnapshot(player.getUniqueId(), species, PetCompanionStore.CompanionSnapshot.capture(pet));
        return true;
    }
}