package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantValueEvaluator;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
 * Handlers are registered at {@code MONITOR} priority as read-only observers,
 * except mechanic-required handlers: the {@code entity_damage_taken} dispatch runs
 * at {@code LOWEST} (without {@code ignoreCancelled}) so dodge/block/cancel
 * abilities negate damage before other plugins, and the firework/projectile
 * handlers run at {@code HIGHEST}.
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
    private io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry stateFilterRegistry;

    public SkillEventListener(Skilling plugin, SkillManager skillManager, ProfileManager profileManager,
                              TagResolver tagResolver, RequirementEngine requirementEngine,
                              MechanicRegistry mechanicRegistry, FeedbackDebouncer feedbackDebouncer,
                              BossBarPool bossBarPool,
                              io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry stateFilterRegistry) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.profileManager = profileManager;
        this.tagResolver = tagResolver;
        this.requirementEngine = requirementEngine;
        this.mechanicRegistry = mechanicRegistry;
        this.feedbackDebouncer = feedbackDebouncer;
        this.bossBarPool = bossBarPool;
        this.stateFilterRegistry = stateFilterRegistry;
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
        // Chained/harvested blocks broken by ChainBreakMechanic or
        // AreaHarvestMechanic are handled by the origin event; do not grant XP or
        // fire abilities again per block.
        if (io.github.chasehuegel.skilling.engine.mechanic.impl.ChainBreakMechanic.isChainProcessing(event.getBlock())
                || io.github.chasehuegel.skilling.engine.mechanic.impl.AreaHarvestMechanic.isChainProcessing(event.getBlock())) {
            debug("block_break skipped: chained/harvested break");
            return;
        }
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
        Player player = resolvePlayerDamager(event);
        if (player != null) {
            dispatch(player, event, "entity_damage");
        }
    }

    private Player resolvePlayerDamager(EntityDamageByEntityEvent event) {
        return io.github.chasehuegel.skilling.engine.mechanic.impl.EntityDamageResolver.resolveDamagerPlayer(event);
    }

    /**
     * Handles {@link EntityDamageEvent} and routes it as an {@code entity_damage_taken} trigger
     * when the damaged entity is a player.
     *
     * <p>Runs at {@code LOWEST} without {@code ignoreCancelled} so dodge/block/cancel
     * abilities negate the damage before other plugins act on it.
     *
     * @param event the entity damage event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageTaken(EntityDamageEvent event) {
        // An even earlier handler already negated the damage; do not dispatch.
        if (event.isCancelled()) return;
        // Visual-only Skilling fireworks carry no real damage; skip abilities.
        if (event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof org.bukkit.entity.Firework firework
                && firework.getPersistentDataContainer().has(Skilling.FIREWORK_KEY, PersistentDataType.BOOLEAN)) {
            return;
        }
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
     * Handles {@link BrewingStartEvent} and routes it as a {@code brew_start} trigger
     * for nearby players. Fires when a new brewing cycle begins, the correct timing
     * for {@code modify_brew_time}.
     *
     * @param event the brewing start event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewStart(org.bukkit.event.block.BrewingStartEvent event) {
        dispatchToNearby(event, event.getBlock().getLocation(), "brew_start");
    }

    /**
     * Handles {@link BrewEvent} and routes it as a {@code brew_potion} trigger for
     * nearby players. Fires when the brewing stand finishes a batch, the correct
     * timing for {@code modify_potion_duration}.
     *
     * @param event the brew finish event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewPotion(org.bukkit.event.inventory.BrewEvent event) {
        dispatchToNearby(event, event.getContents().getLocation(), "brew_potion");
    }

    /**
     * Handles {@link PrepareAnvilEvent} and routes it as a {@code repair} trigger,
     * so {@code repair_discount} can adjust the anvil cost.
     *
     * @param event the prepare anvil event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareAnvil(org.bukkit.event.inventory.PrepareAnvilEvent event) {
        if (event.getView().getPlayer() instanceof Player player) {
            dispatch(player, event, "repair");
        }
    }

    private void dispatchToNearby(Event event, org.bukkit.Location location, String triggerKey) {
        if (location.getWorld() != null) {
            var players = location.getWorld().getNearbyPlayers(location, 5, p -> true);
            for (Player player : players) {
                dispatch(player, event, triggerKey);
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
        if (plugin.isReloading()) return;
        var location = event.getBlock().getLocation();
        if (location.getWorld() != null) {
            int radius = plugin.getCropGrowRadius();
            var players = location.getWorld().getNearbyPlayers(location, radius, p -> true);
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
    public void onEnchantItem(org.bukkit.event.enchantment.EnchantItemEvent event) {
        if (event.getEnchanter() instanceof Player player) {
            dispatch(player, event, "enchant_item");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            dispatch(player, event, "shoot_bow");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(org.bukkit.event.player.PlayerItemDamageEvent event) {
        dispatch(event.getPlayer(), event, "item_damage");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShearEntity(org.bukkit.event.player.PlayerShearEntityEvent event) {
        dispatch(event.getPlayer(), event, "player_shear");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTameEntity(org.bukkit.event.entity.EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) {
            dispatch(player, event, "player_tame");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunchProjectile(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            dispatch(player, event, "launch_projectile");
        }
    }

    /**
     * Handles {@link EntityResurrectEvent} and routes it as a {@code resurrect} trigger
     * when the resurrected entity is a player.
     *
     * @param event the entity resurrect event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResurrect(org.bukkit.event.entity.EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            dispatch(player, event, "resurrect");
        }
    }

    /**
     * Handles {@link EntityToggleGlideEvent} and routes it as an {@code elytra_glide} trigger
     * when a player starts gliding.
     *
     * @param event the entity toggle glide event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onElytraGlide(org.bukkit.event.entity.EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player && event.isGliding()) {
            dispatch(player, event, "elytra_glide");
        }
    }

    private void dispatch(Player player, Event event, String triggerKey) {
        if (plugin.isReloading()) return;
        debug("trigger fired: " + triggerKey + " for " + player.getName());
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) {
            debug("  -> no profile for " + player.getName() + ", skipping");
            return;
        }

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
                int oldLevel = skill.getLevelForXp(profile.getXp(skill.id()));
                double reward = source.reward().evaluate(oldLevel, 1);
                double scalar = resolveEventBulkScalar(event);
                double global = plugin.getGlobalXpModifier();
                double xpBonus = io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic
                        .getMultiplier(player.getUniqueId());
                long rounded = computeXpGain(reward, scalar, global, player.getUniqueId());
                if (rounded > 0) {
                    profile.addXp(skill.id(), rounded);
                    int newLevel = skill.getLevelForXp(profile.getXp(skill.id()));
                    showXpBossBar(player, skill, profile);
                    if (profile.getPreferences().logXp()) {
                        String displayName = skill.display() != null && skill.display().name() != null
                                ? skill.display().name() : skill.id();
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                                .deserialize("&a+" + rounded + " &7XP in &a" + displayName + " &7(" + triggerKey + ")"));
                    }
                    if (newLevel > oldLevel) {
                        profile.invalidatePageCache();
                        var levelUpEvent = new io.github.chasehuegel.skilling.engine.event.SkillingLevelUpEvent(
                                player, skill.id(), newLevel);
                        Bukkit.getPluginManager().callEvent(levelUpEvent);
                        // Route the Skilling level-up through the trigger pipeline so
                        // trigger: level_up abilities and XP sources fire.
                        dispatch(player, levelUpEvent, "level_up");
                        broadcastLevelUp(player, skill, newLevel);
                    }
                    debug("  [" + skill.id() + "] granted " + rounded + " XP (" + triggerKey
                            + ") base=" + reward + " scalar=" + scalar + " global=" + global
                            + " xpBonus=" + xpBonus);
                    plugin.getLogger().info(player.getName() + " earned " + rounded
                            + " XP in " + skill.id() + " (" + triggerKey + ")");
                }
            }
        }
    }

    private void showXpBossBar(Player player, SkillDefinition skill, PlayerProfile profile) {
        LevelUpDispatcher.showXpBossBar(player, skill, profile, bossBarPool, plugin);
    }

    void fireAbilities(Player player, PlayerProfile profile, Event event, String triggerKey) {
        debug("fireAbilities for " + player.getName() + " on " + triggerKey);
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            for (SkillDefinition.Ability ability : skill.abilities()) {
                int skillLevel = skill.getLevelForXp(profile.getXp(skill.id()));
                debug("  ability=" + ability.id() + " skillLevel=" + skillLevel
                        + " unlockLevel=" + ability.unlockLevel());
                if (skillLevel < ability.unlockLevel()) {
                    debug("    -> locked, skipping");
                    continue;
                }

                if (!ability.trigger().equals(triggerKey)) {
                    debug("    -> trigger '" + ability.trigger() + "' != '" + triggerKey + "', skipping");
                    continue;
                }

                // Check requirements ONCE per ability, before iterating mechanics.
                // A cooldown or missing cost gates the whole ability — checking per
                // mechanic meant the first mechanic's consume applied the cooldown,
                // blocking every later mechanic, and item costs were deducted per
                // executing mechanic.
                RequirementResult check = requirementEngine.check(player, ability.id(), ability.requirements(),
                        skillLevel, ability.unlockLevel());
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

                // Execute each mechanic, preserving per-mechanic filter matching and
                // per-mechanic parameter evaluation.
                boolean anyExecuted = false;
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

                    Map<String, Object> evaluatedParams = evaluateParams(entry, skillLevel, ability.unlockLevel());
                    debug("    executing mechanic with params=" + evaluatedParams);
                    boolean executed = mechanic.execute(player, evaluatedParams, event);
                    if (executed) {
                        anyExecuted = true;
                    } else {
                        debug("    -> mechanic returned false (no-op), skipping");
                    }
                }

                // Consume exactly once per activation, only when at least one mechanic
                // performed an action; a no-op ability must not spend its cost.
                if (!anyExecuted) {
                    debug("    -> no mechanic executed, skipping consume and feedback");
                    continue;
                }
                requirementEngine.consume(player, ability.id(), ability.requirements(),
                        skillLevel, ability.unlockLevel());

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

                double cdSec = ability.requirements().cooldown().evaluate(skillLevel, ability.unlockLevel());
                if (cdSec > 0) {
                    long delayTicks = (long) (cdSec * 20);
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
            try {
                matched = tagResolver.resolve(filter.target()).contains(targetMaterial);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown material in filter target: " + filter.target());
                return false;
            }
            if (!matched) return false;
        }

        if (filter.state() != null && !filter.state().isBlank()) {
            String state = filter.state();
            int colonIdx = state.indexOf(':');
            String key = colonIdx > 0 ? state.substring(0, colonIdx) : state;
            String value = colonIdx > 0 ? state.substring(colonIdx + 1) : "";
            if (!stateFilterRegistry.evaluate(key, player, event, value)) return false;
        }

        if (filter.tool() != null && !filter.tool().isBlank()) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            Material handType = hand.getType();
            if (handType == Material.AIR) return false;
            boolean toolMatch;
            try {
                toolMatch = tagResolver.resolve(filter.tool()).contains(handType);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown material in filter tool: " + filter.tool());
                return false;
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
        // The hit entity can be a Hanging (item frame, painting) rather than a
        // LivingEntity; guard so the HIGHEST handler never throws for those hits.
        if (!(event.getHitEntity() instanceof org.bukkit.entity.LivingEntity target)) return;
        if (projectile.getShooter() instanceof org.bukkit.entity.LivingEntity shooter) {
            target.damage(damage, shooter);
        } else {
            target.damage(damage);
        }
    }

    private void broadcastLevelUp(Player player, SkillDefinition skill, int newLevel) {
        LevelUpDispatcher.broadcastLevelUp(player, skill, newLevel, plugin, bossBarPool);
    }

    /**
     * Resolves the bulk-operation scalar for an event. Bulk triggers
     * ({@code collect_xp}, {@code craft_item}, {@code furnace_extract}) scale XP
     * rewards by the magnitude of the operation (orbs collected, items crafted,
     * items extracted from a furnace); all other events return a scalar of
     * {@code 1}. {@code consume_item} is intentionally not bulk-scaled: vanilla
     * consumption removes exactly one item from the stack per event.
     *
     * <p>The raw value is returned so a bulk of {@code 0} yields {@code 0} XP
     * rather than rounding up to a positive reward.
     *
     * @param event the event to inspect
     * @return the bulk scalar, or {@code 1} for non-bulk events
     */
    static double resolveEventBulkScalar(Event event) {
        if (event instanceof org.bukkit.event.player.PlayerExpChangeEvent e) {
            return e.getAmount();
        }
        if (event instanceof org.bukkit.event.inventory.CraftItemEvent e) {
            ItemStack current = e.getCurrentItem();
            if (current != null && !current.isEmpty()) return current.getAmount();
            org.bukkit.inventory.Recipe recipe = e.getRecipe();
            if (recipe != null && recipe.getResult() != null) return recipe.getResult().getAmount();
            return 1;
        }
        if (event instanceof org.bukkit.event.inventory.FurnaceExtractEvent e) {
            // Scale by the number of items extracted, not the vanilla XP orbs.
            return e.getItemAmount();
        }
        return 1;
    }

    /**
     * Computes the rounded XP gain for a single trigger firing: the configured
     * reward is scaled by the bulk-operation scalar and the global XP modifier,
     * then multiplied by the active {@link XpBonusMechanic} multiplier. Rounding
     * happens once, after all scaling. Non-positive gains return {@code 0} so
     * they never grant or round up to a positive amount.
     *
     * @param reward         the configured base reward
     * @param scalar         the bulk-operation scalar (1 for non-bulk events)
     * @param globalModifier the global XP modifier from config
     * @param playerId       the player's UUID (for the session XP bonus)
     * @return the rounded XP gain, or {@code 0} if non-positive
     */
    static long computeXpGain(double reward, double scalar, double globalModifier, UUID playerId) {
        double xp = reward * scalar * globalModifier;
        xp *= io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic.getMultiplier(playerId);
        // Non-finite inputs (NaN/Infinity) must never round to 0 or Long.MAX_VALUE.
        if (!Double.isFinite(xp) || xp <= 0) return 0;
        return Math.round(xp);
    }

    private Material resolveEventMaterial(Event event) {
        if (event instanceof BlockBreakEvent be) return be.getBlock().getType();
        if (event instanceof BlockPlaceEvent pe) return pe.getBlockPlaced().getType();
        if (event instanceof EntityDamageByEntityEvent de) {
            return projectileToMaterial(de.getDamager());
        }
        if (event instanceof EntityDeathEvent ede) {
            var lastDamage = ede.getEntity().getLastDamageCause();
            if (lastDamage instanceof EntityDamageByEntityEvent de) {
                return projectileToMaterial(de.getDamager());
            }
        }
        if (event instanceof org.bukkit.event.inventory.CraftItemEvent ce) {
            return ce.getRecipe().getResult().getType();
        }
        if (event instanceof org.bukkit.event.inventory.FurnaceExtractEvent fe) {
            return fe.getItemType();
        }
        if (event instanceof org.bukkit.event.player.PlayerItemConsumeEvent ce) {
            return ce.getItem().getType();
        }
        if (event instanceof org.bukkit.event.enchantment.EnchantItemEvent ee) {
            return ee.getItem().getType();
        }
        if (event instanceof org.bukkit.event.player.PlayerItemDamageEvent ide) {
            return ide.getItem().getType();
        }
        if (event instanceof org.bukkit.event.entity.ProjectileLaunchEvent ple) {
            return projectileToMaterial(ple.getEntity());
        }
        return null;
    }

    private Material projectileToMaterial(org.bukkit.entity.Entity damager) {
        if (damager instanceof Projectile proj) {
            return switch (proj.getType()) {
                case ARROW -> Material.ARROW;
                case SPECTRAL_ARROW -> Material.SPECTRAL_ARROW;
                case SNOWBALL -> Material.SNOWBALL;
                case EGG -> Material.EGG;
                case TRIDENT -> Material.TRIDENT;
                case FIREBALL -> Material.FIRE_CHARGE;
                case SMALL_FIREBALL -> Material.FIRE_CHARGE;
                case SHULKER_BULLET -> Material.SHULKER_SHELL;
                case LLAMA_SPIT -> null;
                case WITHER_SKULL -> Material.WITHER_SKELETON_SKULL;
                case DRAGON_FIREBALL -> Material.DRAGON_BREATH;
                case FISHING_BOBBER -> Material.FISHING_ROD;
                default -> null;
            };
        }
        return null;
    }

    static Map<String, Object> evaluateParams(SkillDefinition.MechanicEntry entry, int level, int unlockLevel) {
        Map<String, Object> result = new HashMap<>();
        for (var paramEntry : entry.parameters().entrySet()) {
            ParameterEvaluator evaluator = paramEntry.getValue();
            if (evaluator instanceof ConstantValueEvaluator raw) {
                result.put(paramEntry.getKey(), raw.value());
            } else {
                result.put(paramEntry.getKey(), evaluator.evaluate(level, unlockLevel));
            }
        }
        return result;
    }



    private void debug(String msg) {
        plugin.debug(msg);
    }

}
