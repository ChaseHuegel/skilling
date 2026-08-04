package io.github.chasehuegel.skilling.engine.requirements;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The execution gate that enforces the <b>Check, Execute, Consume</b> lifecycle
 * for ability activations.
 *
 * <p>Requirements are evaluated in three ordered phases:
 * <ol>
 *   <li>{@link #check(Player, SkillDefinition.Requirements)} — evaluate all conditions (cooldowns, items, states)</li>
 *   <li>Mechanic execution (external) — run the ability logic</li>
 *   <li>{@link #consume(Player, SkillDefinition.Requirements)} — deduct items and apply cooldowns</li>
 * </ol>
 *
 * <p>State conditions route through the shared {@link StateFilterRegistry} used
 * by XP/mechanic filters, so "must be sneaking" or "must be in the overworld"
 * behave identically whether they gate an ability requirement or an XP source.
 * Unknown states fail the check, matching the filter path and surfacing typos.
 *
 * <p>Item requirements resolve their material/tag reference once per requirement
 * evaluation (not per inventory slot); tag references reuse the cached
 * {@link TagResolver} sets pre-warmed at plugin load and plain material names use
 * a local parse cache.
 */
public final class RequirementEngine {

    private final Map<String, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Material> materialCache = new ConcurrentHashMap<>();
    private TagResolver tagResolver;
    private final StateFilterRegistry stateFilterRegistry;

    /**
     * Constructs a new requirement engine with the given tag resolver and state
     * filter registry for item matching and state gating.
     *
     * @param tagResolver        the tag resolver used to resolve namespace tags in item requirements
     * @param stateFilterRegistry the registry of player-state filters shared with XP/mechanic filters
     */
    public RequirementEngine(TagResolver tagResolver, StateFilterRegistry stateFilterRegistry) {
        this.tagResolver = tagResolver;
        this.stateFilterRegistry = stateFilterRegistry;
    }

    /**
     * Evaluates all requirements for an ability without side effects.
     *
     * <p>Checks cooldowns, player state, and item possession in order.
     * Returns a {@link RequirementResult} — never throws.
     *
     * @param player       the player attempting the ability
     * @param abilityId    the ability identifier used for cooldown tracking
     * @param requirements the ability's requirements definition
     * @param skillLevel   the player's current level in the relevant skill
     * @param unlockLevel  the level at which the ability is unlocked
     * @return the result of the check
     */
    public RequirementResult check(Player player, String abilityId, SkillDefinition.Requirements requirements,
                                   int skillLevel, int unlockLevel) {
        // Check cooldown
        double cdSec = requirements.cooldown().evaluate(skillLevel, unlockLevel);
        if (cdSec > 0) {
            var abilityCooldowns = cooldowns.get(player.getUniqueId().toString());
            if (abilityCooldowns != null) {
                long remaining = getRemainingCooldown(player, abilityId);
                if (remaining > 0) {
                    return RequirementResult.failed(FailureReason.COOLDOWN, Map.of(
                            "time", String.format("%.1f", remaining / 1000.0)
                    ));
                }
            }
        }

        // Check player states
        for (String state : requirements.state()) {
            if (!checkState(player, state)) {
                return RequirementResult.failed(FailureReason.MISSING_STATE, Map.of(
                        "state", state
                ));
            }
        }
        // Check item possession and cost availability
        for (var itemReq : requirements.items()) {
            switch (itemReq.action()) {
                case "possession", "cost" -> {
                    if (!hasItems(player, itemReq.tag(), itemReq.amount(), itemReq.slot())) {
                        return RequirementResult.failed(FailureReason.MISSING_ITEM, Map.of(
                                "item", itemReq.tag(),
                                "amount", String.valueOf(itemReq.amount())
                        ));
                    }
                }
            }
        }

        // Check exhaustion (hunger) requirement
        var exhaustion = requirements.exhaustion();
        if (exhaustion != null) {
            // The minimum is inclusive (a food level equal to the minimum passes).
            if (player.getFoodLevel() < exhaustion.minimum()) {
                return RequirementResult.failed(FailureReason.EXHAUSTION, Map.of(
                        "hunger", String.valueOf(player.getFoodLevel()),
                        "required", String.valueOf((int) Math.ceil(exhaustion.minimum()))
                ));
            }
        }

        return RequirementResult.PASSED;
    }

    /**
     * Deducts items and applies cooldowns after a successful ability execution.
     *
     * <p>Should only be called if {@link #check} returned a passing result.
     *
     * @param player       the player who activated the ability
     * @param abilityId    the ability identifier used for cooldown tracking
     * @param requirements the ability's requirements definition
     * @param skillLevel   the player's current level in the relevant skill
     * @param unlockLevel  the level at which the ability is unlocked
     */
    public void consume(Player player, String abilityId, SkillDefinition.Requirements requirements,
                        int skillLevel, int unlockLevel) {
        // Apply cooldown
        double cdSec = requirements.cooldown().evaluate(skillLevel, unlockLevel);
        if (cdSec > 0) {
            applyCooldown(player, abilityId, (long) (cdSec * 1000));
        }

        // Consume items
        for (var itemReq : requirements.items()) {
            if ("cost".equals(itemReq.action())) {
                removeItems(player, itemReq.tag(), itemReq.amount(), itemReq.slot());
            }
        }

        // Consume exhaustion (hunger)
        var exhaustion = requirements.exhaustion();
        if (exhaustion != null && exhaustion.amount() > 0) {
            int newFood = Math.max(0, player.getFoodLevel() - (int) Math.ceil(exhaustion.amount()));
            player.setFoodLevel(newFood);
        }
    }

    private boolean checkState(Player player, String state) {
        int colonIdx = state.indexOf(':');
        String key = colonIdx > 0 ? state.substring(0, colonIdx) : state;
        String value = colonIdx > 0 ? state.substring(colonIdx + 1) : "";
        // Route through the shared registry so requirement states and XP/mechanic
        // filter states share one implementation and one unknown-state default
        // (false). Requirement states are player conditions, so no event is used.
        return stateFilterRegistry.evaluate(key, player, null, value);
    }

    private boolean hasItems(Player player, String tag, int required, String slot) {
        return countItems(player, tag, slot) >= required;
    }

    private int countItems(Player player, String tag, String slot) {
        Set<Material> resolved = resolveMaterialSet(tag);
        int count = 0;
        for (ItemStack item : slotItems(player, slot)) {
            if (item == null) continue;
            if (resolved.contains(item.getType())) count += item.getAmount();
        }
        return count;
    }

    private void removeItems(Player player, String tag, int amount, String slot) {
        Set<Material> resolved = resolveMaterialSet(tag);
        for (ItemStack item : slotItems(player, slot)) {
            if (item == null || amount <= 0) continue;
            if (resolved.contains(item.getType())) {
                int toRemove = Math.min(amount, item.getAmount());
                item.setAmount(item.getAmount() - toRemove);
                amount -= toRemove;
            }
        }
    }

    /**
     * Resolves an item-requirement reference once into a material set so the
     * per-slot loop never re-resolves tags. Tag references route through the
     * cached {@link TagResolver} (pre-warmed at load); plain material names use a
     * cached name parse.
     */
    private Set<Material> resolveMaterialSet(String tag) {
        if (tag.startsWith("#")) {
            return tagResolver.resolve(tag);
        }
        Material mat = materialCache.computeIfAbsent(tag, Material::matchMaterial);
        return mat == null ? Set.of() : Set.of(mat);
    }

    /**
     * Returns the inventory items scoped to the given slot. {@code HAND}/{@code ANY}
     * (the legacy default) scans the entire inventory; a concrete slot is resolved to
     * that single stack. Throws on malformed slot names (fail-fast).
     */
    private Iterable<ItemStack> slotItems(Player player, String slot) {
        org.bukkit.inventory.EquipmentSlot resolved = resolveSlot(slot);
        if (resolved == null) {
            return java.util.Arrays.asList(player.getInventory().getContents());
        }
        ItemStack item = player.getInventory().getItem(resolved);
        return item == null ? java.util.List.of() : java.util.List.of(item);
    }

    private static org.bukkit.inventory.EquipmentSlot resolveSlot(String slot) {
        if (slot == null || slot.isBlank()) return null;
        return switch (slot.toUpperCase()) {
            case "HAND", "ANY", "ALL" -> null;
            case "MAIN_HAND" -> org.bukkit.inventory.EquipmentSlot.HAND;
            case "OFF_HAND" -> org.bukkit.inventory.EquipmentSlot.OFF_HAND;
            case "HEAD", "HELMET" -> org.bukkit.inventory.EquipmentSlot.HEAD;
            case "CHEST" -> org.bukkit.inventory.EquipmentSlot.CHEST;
            case "LEGS" -> org.bukkit.inventory.EquipmentSlot.LEGS;
            case "FEET", "BOOTS" -> org.bukkit.inventory.EquipmentSlot.FEET;
            default -> throw new IllegalArgumentException("Unknown item requirement slot: " + slot);
        };
    }

    private long getRemainingCooldown(Player player, String abilityId) {
        var abilityCooldowns = cooldowns.get(player.getUniqueId().toString());
        if (abilityCooldowns == null) return 0;
        Long expiresAt = abilityCooldowns.get(abilityId);
        if (expiresAt == null) return 0;
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    private void applyCooldown(Player player, String abilityId, long durationMs) {
        cooldowns.computeIfAbsent(player.getUniqueId().toString(), k -> new ConcurrentHashMap<>())
                .put(abilityId, System.currentTimeMillis() + durationMs);
    }

    /**
     * Replaces the tag resolver used for item matching.
     *
     * @param tagResolver the new tag resolver
     */
    public void setTagResolver(TagResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    /**
     * Clears all cooldowns for a specific player.
     *
     * @param player the player to clear cooldowns for
     */
    public void clearCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId().toString());
    }

    /**
     * Removes cooldown entries that have already expired, plus empty per-player
     * maps, so cooldown state does not grow unboundedly across players who quit
     * and never return. Called periodically on the write-behind worker thread.
     */
    public void pruneExpiredCooldowns() {
        long now = System.currentTimeMillis();
        for (var playerEntry : cooldowns.entrySet()) {
            Map<String, Long> abilityCooldowns = playerEntry.getValue();
            if (abilityCooldowns == null) continue;
            abilityCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
            if (abilityCooldowns.isEmpty()) {
                cooldowns.remove(playerEntry.getKey(), abilityCooldowns);
            }
        }
    }
}