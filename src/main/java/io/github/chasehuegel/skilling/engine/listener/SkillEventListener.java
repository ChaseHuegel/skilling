package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.requirements.RequirementResult;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FanfareDispatcher;
import io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.logging.Level;

/**
 * Central event listener that intercepts Minecraft events and routes them
 * through the Skilling engine for XP granting and ability execution.
 *
 * <p>Each event handler maps to a trigger key used in YAML skill definitions.
 * Handlers are registered at {@code MONITOR} priority as read-only observers
 * (except for mechanic-required handlers at {@code HIGHEST}).
 */
public final class SkillEventListener implements Listener {

    private final Skilling plugin;
    private final SkillManager skillManager;
    private final ProfileManager profileManager;
    private TagResolver tagResolver;
    private final RequirementEngine requirementEngine;
    private final MechanicRegistry mechanicRegistry;
    private final FeedbackDebouncer feedbackDebouncer;
    private final BossBarPool bossBarPool;

    public SkillEventListener(Skilling plugin, SkillManager skillManager, ProfileManager profileManager,
                              TagResolver tagResolver, RequirementEngine requirementEngine,
                              MechanicRegistry mechanicRegistry, FeedbackDebouncer feedbackDebouncer,
                              BossBarPool bossBarPool) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.profileManager = profileManager;
        this.tagResolver = tagResolver;
        this.requirementEngine = requirementEngine;
        this.mechanicRegistry = mechanicRegistry;
        this.feedbackDebouncer = feedbackDebouncer;
        this.bossBarPool = bossBarPool;
    }

    /**
     * Replaces the tag resolver used for filter matching.
     *
     * @param tagResolver the new tag resolver
     */
    public void setTagResolver(TagResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    /**
     * Handles {@link BlockBreakEvent} and routes it as a {@code block_break} trigger.
     *
     * @param event the block break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        debug("block_break fired for " + event.getPlayer().getName()
                + " breaking " + event.getBlock().getType());
        dispatch(event.getPlayer(), event, "block_break");
    }

    /**
     * Handles {@link BlockPlaceEvent} and routes it as a {@code block_place} trigger.
     * Tags the placed block as player-placed for filter matching.
     *
     * @param event the block place event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        event.getBlockPlaced().setMetadata("player_placed", new FixedMetadataValue(plugin, true));
        dispatch(event.getPlayer(), event, "block_place");
    }

    /**
     * Handles {@link EntityDamageByEntityEvent} and routes it as an {@code entity_damage} trigger
     * when the damager is a player.
     *
     * @param event the entity damage by entity event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            dispatch(player, event, "entity_damage");
        }
    }

    /**
     * Handles {@link EntityDamageEvent} and routes it as an {@code entity_damage_taken} trigger
     * when the damaged entity is a player.
     *
     * @param event the entity damage event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageTaken(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            dispatch(player, event, "entity_damage_taken");
        }
    }

    /**
     * Handles {@link EntityDeathEvent} and routes it as an {@code entity_kill} trigger
     * when the killer is a player.
     *
     * @param event the entity death event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player player) {
            dispatch(player, event, "entity_kill");
        }
    }

    /**
     * Handles {@link CraftItemEvent} and routes it as a {@code craft_item} trigger.
     *
     * @param event the craft item event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            dispatch(player, event, "craft_item");
        }
    }

    /**
     * Handles {@link FurnaceExtractEvent} and routes it as a {@code furnace_extract} trigger.
     *
     * @param event the furnace extract event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        dispatch(event.getPlayer(), event, "furnace_extract");
    }

    /**
     * Handles {@link BrewingStartEvent} and routes it as a {@code brew_potion} trigger
     * for nearby players. Fires when a new brewing cycle begins, which is the correct
     * timing for mechanics that modify the current batch.
     *
     * @param event the brewing start event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewPotion(org.bukkit.event.block.BrewingStartEvent event) {
        var location = event.getBlock().getLocation();
        if (location.getWorld() != null) {
            var players = location.getWorld().getNearbyPlayers(location, 5, p -> true);
            for (Player player : players) {
                dispatch(player, event, "brew_potion");
            }
        }
    }

    /**
     * Handles {@link PlayerInteractEvent} and routes it as a {@code player_interact} trigger.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        dispatch(event.getPlayer(), event, "player_interact");
    }

    /**
     * Handles {@link PlayerItemConsumeEvent} and routes it as a {@code consume_item} trigger.
     *
     * @param event the player item consume event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsumeItem(PlayerItemConsumeEvent event) {
        dispatch(event.getPlayer(), event, "consume_item");
    }

    /**
     * Handles {@link PlayerFishEvent} and routes it as a {@code fishing} trigger.
     *
     * @param event the player fish event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        dispatch(event.getPlayer(), event, "fishing");
    }

    /**
     * Handles {@link BlockGrowEvent} and routes it as a {@code crop_grow} trigger
     * for nearby players.
     *
     * @param event the block grow event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        var location = event.getBlock().getLocation();
        if (location.getWorld() != null) {
            var players = location.getWorld().getNearbyPlayers(location, 5, p -> true);
            for (Player player : players) {
                dispatch(player, event, "crop_grow");
            }
        }
    }

    /**
     * Handles {@link EntityBreedEvent} and routes it as a {@code breed_animals} trigger
     * when the breeder is a player.
     *
     * @param event the entity breed event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreedAnimals(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            dispatch(player, event, "breed_animals");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSprint(org.bukkit.event.player.PlayerToggleSprintEvent event) {
        dispatch(event.getPlayer(), event, "sprint");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        dispatch(event.getPlayer(), event, "sneak");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRideHorse(org.bukkit.event.vehicle.VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            dispatch(player, event, "ride_horse");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCollectXp(org.bukkit.event.player.PlayerExpChangeEvent event) {
        dispatch(event.getPlayer(), event, "collect_xp");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(org.bukkit.event.player.PlayerLevelChangeEvent event) {
        dispatch(event.getPlayer(), event, "level_up");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(org.bukkit.event.enchantment.EnchantItemEvent event) {
        if (event.getEnchanter() instanceof Player player) {
            dispatch(player, event, "enchant_item");
        }
    }

    private void dispatch(Player player, Event event, String triggerKey) {
        if (plugin.isReloading()) return;
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) return;

        grantXp(player, profile, event, triggerKey);
        fireAbilities(player, profile, event, triggerKey);
    }

    private void grantXp(Player player, PlayerProfile profile, Event event, String triggerKey) {
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            for (SkillDefinition.XpSource source : skill.xpSources()) {
                if (!source.trigger().equals(triggerKey)) {
                    debug("  [" + skill.id() + "] XP source trigger '" + source.trigger()
                            + "' != '" + triggerKey + "', skipping");
                    continue;
                }
                if (!matchesFilters(player, event, source.filters())) {
                    debug("  [" + skill.id() + "] XP source filters failed, skipping");
                    continue;
                }
                int oldLevel = getLevelForXp(skill, profile.getXp(skill.id()));
                double xp = source.reward().evaluate(oldLevel, 1) * plugin.getGlobalXpModifier();
                xp *= io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic.getMultiplier(player.getUniqueId());
                if (xp > 0) {
                    long rounded = Math.round(xp);
                    profile.addXp(skill.id(), rounded);
                    int newLevel = getLevelForXp(skill, profile.getXp(skill.id()));
                    showXpBossBar(player, skill, profile);
                    if (newLevel > oldLevel) {
                        broadcastLevelUp(player, skill, newLevel);
                    }
                    debug("  [" + skill.id() + "] granted " + rounded + " XP (" + triggerKey + ")");
                    plugin.getLogger().info(player.getName() + " earned " + rounded
                            + " XP in " + skill.id() + " (" + triggerKey + ")");
                }
            }
        }
    }

    private void showXpBossBar(Player player, SkillDefinition skill, PlayerProfile profile) {
        LevelUpDispatcher.showXpBossBar(player, skill, profile, bossBarPool, plugin);
    }

    private void fireAbilities(Player player, PlayerProfile profile, Event event, String triggerKey) {
        debug("fireAbilities for " + player.getName() + " on " + triggerKey);
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            for (SkillDefinition.Ability ability : skill.abilities()) {
                int skillLevel = getLevelForXp(skill, profile.getXp(skill.id()));
                debug("  ability=" + ability.id() + " skillLevel=" + skillLevel
                        + " unlockLevel=" + ability.unlockLevel());
                if (skillLevel < ability.unlockLevel()) {
                    debug("    -> locked, skipping");
                    continue;
                }

                for (SkillDefinition.MechanicEntry entry : ability.mechanics()) {
                    debug("    mechanic=" + entry.type() + " skill=" + skill.id());
                    Object raw = mechanicRegistry.create(entry.type());
                    if (!(raw instanceof SkillMechanic mechanic)) {
                        debug("    -> mechanic not found in registry, skipping");
                        continue;
                    }

                    if (!matchesFilters(player, event, entry.filters())) {
                        debug("    -> filters failed, skipping");
                        continue;
                    }

                    RequirementResult check = requirementEngine.check(player, ability.id(), ability.requirements());
                    debug("    requirement check=" + (check.success() ? "PASS" : "FAIL"));
                    if (!check.success()) {
                        if (feedbackDebouncer.tryDebounce(player, ability.id())) {
                            var failure = ability.onFailure().reasons().get(check.failureReason().name().toLowerCase());
                            if (failure != null && !failure.actionBar().isBlank()) {
                                String msg = failure.actionBar();
                                for (var ph : check.placeholders().entrySet()) {
                                    msg = msg.replace("{" + ph.getKey() + "}", ph.getValue());
                                }
                                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                            }
                        }
                        continue;
                    }

                    Map<String, Object> evaluatedParams = evaluateParams(entry, skillLevel, ability.unlockLevel());
                    debug("    executing mechanic with params=" + evaluatedParams);
                    boolean executed = mechanic.execute(player, evaluatedParams, event);
                    if (!executed) {
                        debug("    -> mechanic returned false (no-op), skipping consume and feedback");
                        continue;
                    }
                    requirementEngine.consume(player, ability.id(), ability.requirements());

                    String abilityMsg = ability.feedback().message();
                    boolean hasMsg = !abilityMsg.isBlank();
                    if (ability.feedback().actionBar() && hasMsg) {
                        FanfareDispatcher.sendActionBar(player, abilityMsg);
                        if (profile.getPreferences().logAbilities()) {
                            player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                                    .deserialize(abilityMsg));
                        }
                    }
                    if (ability.feedback().chat() && hasMsg && !ability.feedback().actionBar()) {
                        if (profile.getPreferences().logAbilities()) {
                            player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                                    .deserialize(abilityMsg));
                        }
                    }

                    if (ability.requirements().cooldown() > 0) {
                        long delayTicks = (long) (ability.requirements().cooldown() * 20);
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                String readyMsg = "<green>✦ " + ability.displayName() + " is ready!</green>";
                                player.sendMessage(MiniMessage.miniMessage().deserialize(readyMsg));
                                player.sendActionBar(net.kyori.adventure.text.Component.text(
                                        "✦ " + ability.displayName() + " is ready!",
                                        NamedTextColor.GREEN));
                            }
                        }, delayTicks);
                    }
                    if (!ability.feedback().particles().isEmpty()) {
                        FanfareDispatcher.dispatchParticles(player, null, ability.feedback().particles());
                    }
                    if (!ability.feedback().sounds().isEmpty()) {
                        FanfareDispatcher.dispatchSounds(player, null, ability.feedback().sounds());
                    }
                    debug("    -> done");
                }
            }
        }
    }

    private boolean matchesFilters(Player player, Event event, List<SkillDefinition.Filter> filters) {
        if (filters == null || filters.isEmpty()) return true;
        for (SkillDefinition.Filter filter : filters) {
            boolean passed = matchFilter(player, event, filter);
            debug("  filter target=" + filter.target() + " state=" + filter.state()
                    + " tool=" + filter.tool() + " -> " + (passed ? "PASS" : "FAIL"));
            if (!passed) return false;
        }
        return true;
    }

    private boolean matchFilter(Player player, Event event, SkillDefinition.Filter filter) {
        if (filter.target() != null && !filter.target().isBlank()) {
            Material targetMaterial = resolveEventMaterial(event);
            if (targetMaterial == null) return false;
            boolean matched;
            if (filter.target().startsWith("#")) {
                matched = tagResolver.resolve(filter.target()).contains(targetMaterial);
            } else {
                Material filterMat = Material.matchMaterial(filter.target());
                if (filterMat == null) {
                    plugin.getLogger().warning("Unknown material in filter target: " + filter.target());
                    return false;
                }
                matched = targetMaterial == filterMat;
            }
            if (!matched) return false;
        }

        if (filter.state() != null && !filter.state().isBlank()) {
            boolean passed = switch (filter.state()) {
                case "is_sneaking" -> player.isSneaking();
                case "is_sprinting" -> player.isSprinting();
                case "is_in_water" -> player.isInWater();
                case "is_on_ground" -> player.isOnGround();
                case "player_placed:false" -> {
                    if (event instanceof BlockBreakEvent be) {
                        yield !be.getBlock().hasMetadata("player_placed");
                    }
                    yield true;
                }
                default -> true;
            };
            if (!passed) return false;
        }

        if (filter.tool() != null && !filter.tool().isBlank()) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            Material handType = hand.getType();
            if (handType == Material.AIR) return false;
            boolean toolMatch;
            if (filter.tool().startsWith("#")) {
                toolMatch = tagResolver.resolve(filter.tool()).contains(handType);
            } else {
                Material toolMat = Material.matchMaterial(filter.tool());
                if (toolMat == null) {
                    plugin.getLogger().warning("Unknown material in filter tool: " + filter.tool());
                    return false;
                }
                toolMatch = handType == toolMat;
            }
            if (!toolMatch) return false;
        }

        return true;
    }

    /**
     * Cancels damage from Skilling-launched fireworks to prevent unintended harm.
     *
     * @param event the entity damage by entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByFirework(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Firework fw
                && fw.getPersistentDataContainer().has(Skilling.FIREWORK_KEY, PersistentDataType.BOOLEAN)) {
            event.setCancelled(true);
        }
    }

    /**
     * Applies custom projectile damage from {@link ProjectileMechanic} when a Skilling snowball hits an entity.
     * Damage is stored via PersistentDataContainer on the snowball entity.
     *
     * @param event the projectile hit event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getHitEntity() == null) return;
        if (!(event.getEntity() instanceof Projectile projectile)) return;
        var pdc = projectile.getPersistentDataContainer();
        if (!pdc.has(io.github.chasehuegel.skilling.engine.mechanic.impl.ProjectileMechanic.DAMAGE_KEY, PersistentDataType.DOUBLE)) return;
        double damage = pdc.get(io.github.chasehuegel.skilling.engine.mechanic.impl.ProjectileMechanic.DAMAGE_KEY, PersistentDataType.DOUBLE);
        if (damage <= 0) return;
        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) event.getHitEntity();
        if (projectile.getShooter() instanceof org.bukkit.entity.LivingEntity shooter) {
            target.damage(damage, shooter);
        } else {
            target.damage(damage);
        }
    }

    private void broadcastLevelUp(Player player, SkillDefinition skill, int newLevel) {
        LevelUpDispatcher.broadcastLevelUp(player, skill, newLevel, plugin, bossBarPool);
    }

    private Material resolveEventMaterial(Event event) {
        if (event instanceof BlockBreakEvent be) return be.getBlock().getType();
        if (event instanceof BlockPlaceEvent pe) return pe.getBlockPlaced().getType();
        if (event instanceof EntityDamageByEntityEvent de) {
            if (de.getEntity() instanceof org.bukkit.entity.LivingEntity le) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> evaluateParams(SkillDefinition.MechanicEntry entry, int level, int unlockLevel) {
        Map<String, Object> result = new HashMap<>();
        for (var paramEntry : entry.parameters().entrySet()) {
            result.put(paramEntry.getKey(), paramEntry.getValue().evaluate(level, unlockLevel));
        }
        return result;
    }

    private int getLevelForXp(SkillDefinition skill, long xp) {
        for (int level = 1; level <= skill.maxLevel(); level++) {
            double required = skill.progression().evaluator().evaluate(level, 0);
            if (xp < (long) required) return level - 1;
        }
        return skill.maxLevel();
    }

    private void debug(String msg) {
        if (plugin.isDebugLogging()) {
            plugin.getLogger().info("[DEBUG] " + msg);
        }
    }

}
