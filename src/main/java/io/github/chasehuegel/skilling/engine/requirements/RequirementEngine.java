package io.github.chasehuegel.skilling.engine.requirements;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

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
 */
public final class RequirementEngine {

    private final Map<String, Map<String, Long>> cooldowns = new HashMap<>();
    private final TagResolver tagResolver;

    public RequirementEngine(TagResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    /**
     * Evaluates all requirements for an ability without side effects.
     *
     * <p>Checks cooldowns, player state, and item possession in order.
     * Returns a {@link RequirementResult} — never throws.
     *
     * @param player       the player attempting the ability
     * @param requirements the ability's requirements definition
     * @return the result of the check
     */
    public RequirementResult check(Player player, SkillDefinition.Requirements requirements) {
        // Check cooldown
        if (requirements.cooldown() > 0) {
            var abilityCooldowns = cooldowns.get(player.getUniqueId().toString());
            if (abilityCooldowns != null) {
                long remaining = getRemainingCooldown(player, "global");
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

        // Check item possession
        for (var itemReq : requirements.items()) {
            if ("possession".equals(itemReq.action())) {
                if (!hasItem(player, itemReq.tag(), itemReq.slot())) {
                    return RequirementResult.failed(FailureReason.MISSING_ITEM, Map.of(
                            "item", itemReq.tag(),
                            "amount", String.valueOf(itemReq.amount())
                    ));
                }
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
     * @param requirements the ability's requirements definition
     */
    public void consume(Player player, SkillDefinition.Requirements requirements) {
        // Apply cooldown
        if (requirements.cooldown() > 0) {
            applyCooldown(player, "global", (long) (requirements.cooldown() * 1000));
        }

        // Consume items
        for (var itemReq : requirements.items()) {
            if ("cost".equals(itemReq.action())) {
                removeItems(player, itemReq.tag(), itemReq.amount());
            }
        }
    }

    private boolean checkState(Player player, String state) {
        return switch (state) {
            case "is_sneaking" -> player.isSneaking();
            case "is_sprinting" -> player.isSprinting();
            case "is_in_water" -> player.isInWater();
            case "is_on_ground" -> player.isOnGround();
            default -> true; // unknown states pass through
        };
    }

    private boolean hasItem(Player player, String tag, String slot) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (tag.startsWith("#")) {
                if (tagResolver.resolve(tag).contains(item.getType())) return true;
            } else {
                Material mat = Material.matchMaterial(tag);
                if (mat != null && item.getType() == mat) return true;
            }
        }
        return false;
    }

    private void removeItems(Player player, String tag, int amount) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || amount <= 0) continue;
            boolean match;
            if (tag.startsWith("#")) {
                match = tagResolver.resolve(tag).contains(item.getType());
            } else {
                Material mat = Material.matchMaterial(tag);
                match = mat != null && item.getType() == mat;
            }
            if (match) {
                int toRemove = Math.min(amount, item.getAmount());
                item.setAmount(item.getAmount() - toRemove);
                amount -= toRemove;
            }
        }
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
        cooldowns.computeIfAbsent(player.getUniqueId().toString(), k -> new HashMap<>())
                .put(abilityId, System.currentTimeMillis() + durationMs);
    }

    /**
     * Clears all cooldowns for a specific player.
     *
     * @param player the player to clear cooldowns for
     */
    public void clearCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId().toString());
    }
}