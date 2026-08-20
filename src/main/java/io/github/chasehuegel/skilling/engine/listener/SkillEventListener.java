package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;
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
 * except mechanic-required handlers: the {@code entity_damage_taken} and
 * {@code fall_damage} dispatches run at {@code LOWEST} (without
 * {@code ignoreCancelled}) so dodge/block/cancel abilities negate damage before
 * other plugins, and the firework/projectile handlers run at {@code HIGHEST}.
 */
public final class SkillEventListener implements Listener {

    /**
     * Minimum milliseconds between {@code chunk_load} dispatches per player.
     * New terrain generates many chunks at once across the player's view, so an
     * unthrottled dispatch turns exploration into a flood of XP and ability
     * refreshes; the window bounds it to a steady trickle.
     */
    static final long CHUNK_LOAD_THROTTLE_MS = 5000;

    private final Skilling plugin;
    private final SkillManager skillManager;
    private final ProfileManager profileManager;
    private TagResolver tagResolver;
    private final RequirementEngine requirementEngine;
    private final MechanicRegistry mechanicRegistry;
    private final FeedbackDebouncer feedbackDebouncer;
    private final BossBarPool bossBarPool;
    private io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry stateFilterRegistry;
    private final java.util.concurrent.ConcurrentMap<UUID, Long> chunkLoadLastDispatch =
            new java.util.concurrent.ConcurrentHashMap<>();

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
        // Drop the player_placed marker with the destroyed block — including
        // chained/harvested neighbors — so a block that regenerates in this spot
        // is not still treated as player-placed.
        if (event.getBlock().hasMetadata("player_placed")) {
            event.getBlock().removeMetadata("player_placed", plugin);
        }
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

    /**
     * Handles {@link EntityDamageByEntityEvent} and routes it as a
     * {@code left_click_entity} trigger when the player attacks an entity directly
     * with their hand. Projectile attacks are not left-clicks and stay on the
     * {@code entity_damage} and {@code shoot_bow} triggers.
     *
     * @param event the entity damage by entity event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeftClickEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            dispatch(player, event, "left_click_entity");
        }
    }

    private Player resolvePlayerDamager(EntityDamageByEntityEvent event) {
        return io.github.chasehuegel.skilling.engine.mechanic.impl.EntityDamageResolver.resolveDamagerPlayer(event);
    }

    /**
     * Handles {@link EntityDamageEvent} and routes it as an {@code entity_damage_taken}
     * trigger when the damaged entity is a player. Fall damage additionally dispatches
     * the {@code fall_damage} trigger so acrobatics-style sources bind precisely to
     * falls instead of proxying them through the {@code is_on_ground} state filter.
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
            // A dodge/block/cancel ability may have negated the fall while the
            // entity_damage_taken dispatch ran; do not fire fall_damage for it.
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !event.isCancelled()) {
                dispatch(player, event, "fall_damage");
            }
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrepareAnvil(org.bukkit.event.inventory.PrepareAnvilEvent event) {
        if (event.getView().getPlayer() instanceof Player player) {
            dispatch(player, event, "repair");
        }
    }

    private void dispatchToNearby(Event event, org.bukkit.Location location, String triggerKey) {
        dispatchToNearby(event, location, triggerKey, 5);
    }

    /**
     * Dispatches a trigger to every nearby player within the given radius.
     *
     * <p>Used for world events without an owning player (brewing, chunk
     * generation). The radius default of 5 suits block-scale events; exploration
     * uses a larger radius because new chunks generate around the player's
     * view distance rather than at their feet.
     *
     * @param event       the triggering event
     * @param location    the event location
     * @param triggerKey  the trigger key to dispatch
     * @param radius      the search radius in blocks
     */
    private void dispatchToNearby(Event event, org.bukkit.Location location, String triggerKey, double radius) {
        // BrewEvent.getContents().getLocation() can be null; nothing to do without a world.
        if (location == null || location.getWorld() == null) return;
        var players = location.getWorld().getNearbyPlayers(location, radius, p -> true);
        for (Player player : players) {
            dispatch(player, event, triggerKey);
        }
    }

    /**
     * Handles {@link PlayerInteractEvent} and routes it as a {@code player_interact} trigger.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Skip the off-hand duplicate of a two-handed interaction so a right-click
        // with items in both hands fires the ability / XP source once, not twice.
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        dispatch(event.getPlayer(), event, "player_interact");
    }

    /**
     * Handles {@link PlayerInteractEvent} and routes the four action-specific
     * click triggers ({@code right_click_air}, {@code right_click_block},
     * {@code left_click_air}, {@code left_click_block}) based on the click action.
     *
     * <p>The union {@code right_click} trigger is also fired for every right-click
     * (air or block) so an ability or XP source bound to "a right-click use" works
     * regardless of whether the cursor happened to hit a block. Left-clicks never
     * fire {@code right_click}.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClickAction(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        String triggerKey = switch (event.getAction()) {
            case RIGHT_CLICK_AIR -> "right_click_air";
            case RIGHT_CLICK_BLOCK -> "right_click_block";
            case LEFT_CLICK_AIR -> "left_click_air";
            case LEFT_CLICK_BLOCK -> "left_click_block";
            default -> null;
        };
        if (triggerKey != null) {
            dispatch(event.getPlayer(), event, triggerKey);
        }
        if (isRightClick(event.getAction())) {
            dispatch(event.getPlayer(), event, "right_click");
        }
    }

    private static boolean isRightClick(org.bukkit.event.block.Action action) {
        return action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
    }

    /**
     * Handles {@link PlayerInteractEvent} and routes it as a {@code jukebox_play}
     * trigger when a music disc is actually inserted into an empty jukebox.
     *
     * <p>The jukebox tile entity's record is not reliably committed by the time
     * a {@code MONITOR}-priority listener runs, so the state is re-read one tick
     * later. A jukebox that is holding a record then means a disc was placed;
     * an ejection or a click with no disc leaves it empty and is skipped, so the
     * trigger fires only on a real insertion.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJukeboxInsert(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        org.bukkit.block.Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.JUKEBOX) return;
        org.bukkit.World world = clicked.getWorld();
        org.bukkit.Location loc = clicked.getLocation().toBlockLocation();
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            org.bukkit.block.Block block = world.getBlockAt(loc);
            if (block.getType() != Material.JUKEBOX) return;
            if (!(block.getState() instanceof org.bukkit.block.Jukebox jukebox)) return;
            // A right-click that placed a disc leaves the jukebox holding a record.
            if (!jukebox.hasRecord()) return;
            dispatch(player, event, "jukebox_play");
        });
    }

    /**
     * Handles {@link PlayerEditBookEvent} and routes it as a {@code sign_book}
     * trigger when the player signs a book-and-quill into a written book.
     *
     * @param event the player edit book event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignBook(PlayerEditBookEvent event) {
        if (!event.isSigning()) return;
        dispatch(event.getPlayer(), event, "sign_book");
    }

    /**
     * Handles {@link PlayerInsertLecternBookEvent} and routes it as a
     * {@code lectern_place} trigger when a book is placed onto a lectern.
     *
     * @param event the player insert lectern book event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLecternInsert(io.papermc.paper.event.player.PlayerInsertLecternBookEvent event) {
        dispatch(event.getPlayer(), event, "lectern_place");
    }

    /**
     * Handles {@link PlayerInteractEntityEvent} and routes it as a
     * {@code right_click_entity} and {@code right_click} trigger.
     *
     * @param event the player interact entity event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRightClickEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        dispatch(event.getPlayer(), event, "right_click_entity");
        dispatch(event.getPlayer(), event, "right_click");
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
     * <p>The event fires once per state transition (cast, bite, reel, catch,
     * fail), so only the {@code CAUGHT_FISH} state represents a completed catch.
     * Gating on it ensures a single cast-and-catch grants exactly one XP reward
     * and fires fishing abilities exactly once; casts, bites, reels, and failed
     * attempts grant nothing.
     *
     * @param event the player fish event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
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
        // Fire only when sprinting starts, not on release.
        if (!event.isSprinting()) return;
        dispatch(event.getPlayer(), event, "sprint");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        // Fire only when sneaking starts, not on release.
        if (!event.isSneaking()) return;
        dispatch(event.getPlayer(), event, "sneak");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJump(com.destroystokyo.paper.event.player.PlayerJumpEvent event) {
        dispatch(event.getPlayer(), event, "jump");
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
            // Stamp the arrow with the firing stance so a "sneak-shot" ability can
            // be evaluated at impact time via the was_sneaking state filter. The
            // player-captured arrow carries the sneak state used to release it.
            if (event.getProjectile() != null) {
                event.getProjectile().getPersistentDataContainer().set(
                        Skilling.SHOT_SNEAK_KEY, PersistentDataType.BOOLEAN, player.isSneaking());
            }
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
     * Routes the {@code projectile_hit} trigger when a projectile (from any
     * shooter) lands on a block or entity. The mechanic must verify the shooter is
     * the dispatching player. Runs at MONITOR so it never conflicts with the
     * HIGHEST {@link #onProjectileHit} handler that applies {@code core:projectile}
     * damage.
     *
     * @param event the projectile hit event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHitTrigger(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            dispatch(player, event, "projectile_hit");
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
     * Handles {@link EntityTransformEvent} and routes it as a {@code cure_villager}
     * trigger when a zombie villager is cured (reason {@code CURED}).
     *
     * <p>The cure completes minutes after the player feeds the golden apple, so
     * Paper records the initiating player on the {@code ZombieVillager} via
     * {@code getConversionPlayer()}. The trigger only dispatches when that player
     * is still online; a cure finished while the initiator is offline grants no XP.
     *
     * @param event the entity transform event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCureVillager(org.bukkit.event.entity.EntityTransformEvent event) {
        if (event.getTransformReason() != org.bukkit.event.entity.EntityTransformEvent.TransformReason.CURED) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.ZombieVillager zombie)) return;
        org.bukkit.OfflinePlayer conversionPlayer = zombie.getConversionPlayer();
        if (conversionPlayer == null) return;
        Player player = conversionPlayer.getPlayer();
        if (player == null) return;
        dispatch(player, event, "cure_villager");
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

    /**
     * Handles {@link ChunkLoadEvent} and routes it as a {@code chunk_load} trigger
     * for players exploring freshly generated chunks, so exploring uncharted land
     * is the rewarded action rather than merely loading a chunk from disk.
     *
     * <p>New chunks generate at the edge of a player's view distance, not at the
     * player's feet, and their center sits at the chunk's ground plane, so the
     * reach must be horizontal and span the client view distance in blocks; a
     * block-scale 3D radius would never reach the exploring player.
     *
     * @param event the chunk load event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;
        org.bukkit.Chunk chunk = event.getChunk();
        org.bukkit.World world = chunk.getWorld();
        double chunkCenterX = (chunk.getX() << 4) + 8;
        double chunkCenterZ = (chunk.getZ() << 4) + 8;
        for (Player player : world.getPlayers()) {
            org.bukkit.Location loc = player.getLocation();
            double dx = loc.getX() - chunkCenterX;
            double dz = loc.getZ() - chunkCenterZ;
            // +1 chunk of margin so the leading-edge chunks that generate at the
            // boundary of the player's view are caught, not just the ones inside it.
            double reach = (Math.max(4, player.getClientViewDistance()) + 1) * 16.0;
            if (dx * dx + dz * dz > reach * reach) continue;
            // Throttle per player: new terrain generates many chunks at once, so
            // gate the dispatch to once per throttle window to bound the XP rate.
            long now = System.currentTimeMillis();
            Long last = chunkLoadLastDispatch.get(player.getUniqueId());
            if (last != null && now - last < CHUNK_LOAD_THROTTLE_MS) continue;
            chunkLoadLastDispatch.put(player.getUniqueId(), now);
            dispatch(player, event, "chunk_load");
        }
    }

    /**
     * Handles {@link PlayerDeepSleepEvent} and routes it as a {@code sleep} trigger
     * when a player passes the night, so only a genuine sleep (not checking into
     * and back out of a bed) is rewarded.
     *
     * @param event the deep sleep event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeepSleep(io.papermc.paper.event.player.PlayerDeepSleepEvent event) {
        dispatch(event.getPlayer(), event, "sleep");
    }

    /**
     * Handles {@link CompostItemEvent} and routes it as a {@code compost} trigger
     * for nearby players of the composter block.
     *
     * @param event the compost item event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCompost(io.papermc.paper.event.block.CompostItemEvent event) {
        dispatchToNearby(event, event.getBlock().getLocation(), "compost");
    }

    /**
     * Handles {@link BlockFertilizeEvent} and routes it as a {@code fertilize}
     * trigger to the player who used the bonemeal. Nested events raised by
     * {@code core:area_fertilize} spreading bonemeal are skipped so the spread
     * cannot re-enter the pipeline and cascade recursively.
     *
     * @param event the block fertilize event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFertilize(org.bukkit.event.block.BlockFertilizeEvent event) {
        if (io.github.chasehuegel.skilling.engine.mechanic.impl.AreaFertilizeMechanic
                .isFertilizeProcessing(event.getBlock())) {
            return;
        }
        Player player = event.getPlayer();
        if (player != null) {
            dispatch(player, event, "fertilize");
        }
    }

    /**
     * Handles {@link LootGenerateEvent} and routes it as a {@code loot} trigger
     * for nearby players of the loot location.
     *
     * @param event the loot generate event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLoot(org.bukkit.event.world.LootGenerateEvent event) {
        org.bukkit.loot.LootContext context = event.getLootContext();
        if (context == null) return;
        org.bukkit.Location location = context.getLocation();
        if (location == null || location.getWorld() == null) return;
        dispatchToNearby(event, location, "loot", 16);
    }

    /**
     * Handles {@link PlayerTradeEvent} and routes it as a {@code trade} trigger.
     *
     * @param event the player trade event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(io.papermc.paper.event.player.PlayerTradeEvent event) {
        dispatch(event.getPlayer(), event, "trade");
    }

    /**
     * Handles {@link PiglinBarterEvent} and routes it as a {@code barter} trigger
     * for nearby players of the bartering piglin.
     *
     * @param event the piglin barter event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBarter(org.bukkit.event.entity.PiglinBarterEvent event) {
        dispatchToNearby(event, event.getEntity().getLocation(), "barter");
    }

    /**
     * Handles {@link PlayerRecipeDiscoverEvent} and routes it as a
     * {@code recipe_discover} trigger.
     *
     * @param event the player recipe discover event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRecipeDiscover(org.bukkit.event.player.PlayerRecipeDiscoverEvent event) {
        dispatch(event.getPlayer(), event, "recipe_discover");
    }

    /**
     * Handles {@link SmithItemEvent} and routes it as a {@code smith} trigger to
     * the player viewing the smithing table.
     *
     * @param event the smith item event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmith(org.bukkit.event.inventory.SmithItemEvent event) {
        if (event.getView().getPlayer() instanceof Player player) {
            dispatch(player, event, "smith");
        }
    }

    /**
     * Handles {@link PlayerItemMendEvent} and routes it as a {@code mend} trigger.
     *
     * @param event the player item mend event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMend(org.bukkit.event.player.PlayerItemMendEvent event) {
        dispatch(event.getPlayer(), event, "mend");
    }

    /**
     * Handles {@link PlayerMapFilledEvent} and routes it as a {@code map_fill} trigger.
     *
     * @param event the player map filled event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMapFill(io.papermc.paper.event.player.PlayerMapFilledEvent event) {
        dispatch(event.getPlayer(), event, "map_fill");
    }

    /**
     * Handles {@link CartographyItemEvent} and routes it as a {@code cartography}
     * trigger to the player viewing the cartography table.
     *
     * @param event the cartography item event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCartography(io.papermc.paper.event.player.CartographyItemEvent event) {
        if (event.getView().getPlayer() instanceof Player player) {
            dispatch(player, event, "cartography");
        }
    }

    /**
     * Handles {@link VaultChangeStateEvent} and routes it as a
     * {@code vault_change} trigger to the triggering player when one is present.
     *
     * @param event the vault change state event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVaultChange(io.papermc.paper.event.block.VaultChangeStateEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            dispatch(player, event, "vault_change");
        }
    }

    /**
     * Handles {@link EntityFertilizeEggEvent} and routes it as a {@code sniffer}
     * trigger to the breeding player when the breeder is a player.
     *
     * @param event the entity fertilize egg event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSniffer(io.papermc.paper.event.entity.EntityFertilizeEggEvent event) {
        Player breeder = event.getBreeder();
        if (breeder != null) {
            dispatch(breeder, event, "sniffer");
        }
    }

    /**
     * Handles {@link PotionSplashEvent} and routes it as a {@code potion_splash}
     * trigger to the throwing player when the thrower is a player.
     *
     * @param event the potion splash event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplash(org.bukkit.event.entity.PotionSplashEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            dispatch(player, event, "potion_splash");
        }
    }

    /**
     * Handles {@link LingeringPotionSplashEvent} and routes it as a
     * {@code potion_splash} trigger to the throwing player when the thrower is a
     * player.
     *
     * @param event the lingering potion splash event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLingeringPotionSplash(org.bukkit.event.entity.LingeringPotionSplashEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            dispatch(player, event, "potion_splash");
        }
    }

    private void dispatch(Player player, Event event, String triggerKey) {
        dispatch(player, event, triggerKey, new java.util.HashSet<>());
    }

    /**
     * Dispatches a trigger to the XP and ability pipeline.
     *
     * <p>The {@code levelUpCascade} set carries the skills whose level-up has
     * already been dispatched in the current event chain. It is threaded through
     * the recursive {@code level_up} dispatch so a {@code level_up} XP reward that
     * crosses another threshold cannot re-enter {@code grantXp} for the same skill
     * forever; each skill's level-up feedback fires at most once per actual level-up.
     *
     * @param player          the player to dispatch for
     * @param event           the triggering event
     * @param triggerKey      the trigger key (e.g. {@code block_break})
     * @param levelUpCascade  skills already dispatched for {@code level_up} in this chain
     */
    private void dispatch(Player player, Event event, String triggerKey, Set<String> levelUpCascade) {
        if (plugin.isReloading()) return;
        debug("trigger fired: " + triggerKey + " for " + player.getName());
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) {
            debug("  -> no profile for " + player.getName() + ", skipping");
            return;
        }

        grantXp(player, profile, event, triggerKey, levelUpCascade);
        fireAbilities(player, profile, event, triggerKey);
    }

    private void grantXp(Player player, PlayerProfile profile, Event event, String triggerKey,
                         Set<String> levelUpCascade) {
        // A skill's level is computed once per dispatch and shared by its XP
        // sources; level-ups advance it in place so later sources in the same
        // event see the updated level.
        Map<SkillDefinition, Integer> levelBySkill = new java.util.IdentityHashMap<>();
        for (SkillManager.XpSourceRef ref : skillManager.xpSourcesFor(triggerKey)) {
            SkillDefinition skill = ref.skill();
            SkillDefinition.XpSource source = ref.source();
            if (!matchesFilters(player, event, triggerKey, source.filters())) {
                debug("  [" + skill.id() + "] XP source filters failed, skipping");
                continue;
            }
            int skillLevel = levelBySkill.computeIfAbsent(skill, s -> s.getLevelForXp(profile.getXp(s.id())));
            int oldLevel = skillLevel;
            double reward = source.reward().evaluate(oldLevel, 1);
            double scalar = resolveSourceScalar(source, event);
            double global = plugin.getGlobalXpModifier();
            double xpBonus = io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic
                    .getMultiplier(player.getUniqueId());
            long rounded = computeXpGain(reward, scalar, global, player.getUniqueId());
            if (rounded > 0) {
                if ("level_up".equals(triggerKey)) {
                    // A level_up reward must advance the skill at most one level
                    // per level-up event; without this clamp a large reward would
                    // leap several thresholds in one grant and re-enter grantXp
                    // through the nested level_up dispatch.
                    int targetLevel = Math.min(oldLevel + 1, skill.maxLevel());
                    rounded = clampGrantToLevel(skill, profile.getXp(skill.id()), rounded, targetLevel);
                    if (rounded <= 0) {
                        continue;
                    }
                }
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
                    skillLevel = newLevel;
                    levelBySkill.put(skill, newLevel);
                    profile.invalidatePageCache();
                    // Fire level-up feedback exactly once per actual level-up. The
                    // cascade set makes the nested dispatch a no-op for a skill that
                    // already leveled up in this chain, so a level_up XP reward that
                    // crosses another threshold cannot recursively pump the skill.
                    if (levelUpCascade.add(skill.id())) {
                        var levelUpEvent = new io.github.chasehuegel.skilling.engine.event.SkillingLevelUpEvent(
                                player, skill.id(), newLevel);
                        Bukkit.getPluginManager().callEvent(levelUpEvent);
                        // Route the Skilling level-up through the trigger pipeline so
                        // trigger: level_up abilities and XP sources fire.
                        dispatch(player, levelUpEvent, "level_up", levelUpCascade);
                        broadcastLevelUp(player, skill, newLevel);
                    }
                }
                debug("  [" + skill.id() + "] granted " + rounded + " XP (" + triggerKey
                        + ") base=" + reward + " scalar=" + scalar + " global=" + global
                        + " xpBonus=" + xpBonus);
            }
        }
    }

    private void showXpBossBar(Player player, SkillDefinition skill, PlayerProfile profile) {
        LevelUpDispatcher.showXpBossBar(player, skill, profile, bossBarPool, plugin);
    }

    void fireAbilities(Player player, PlayerProfile profile, Event event, String triggerKey) {
        debug("fireAbilities for " + player.getName() + " on " + triggerKey);
        // Level does not change during a dispatch (mechanics do not grant XP),
        // so it is computed once per skill and shared by its abilities.
        Map<SkillDefinition, Integer> levelBySkill = new java.util.IdentityHashMap<>();
        for (SkillManager.AbilityRef ref : skillManager.abilitiesFor(triggerKey)) {
            SkillDefinition skill = ref.skill();
            SkillDefinition.Ability ability = ref.ability();
            int skillLevel = levelBySkill.computeIfAbsent(skill, s -> s.getLevelForXp(profile.getXp(s.id())));
            debug("  ability=" + ability.id() + " skillLevel=" + skillLevel
                    + " unlockLevel=" + ability.unlockLevel());
            if (skillLevel < ability.unlockLevel()) {
                debug("    -> locked, skipping");
                continue;
            }

            // Check requirements ONCE per ability, before iterating mechanics.
            // A cooldown or missing cost gates the whole ability — checking per
            // mechanic meant the first mechanic's consume applied the cooldown,
            // blocking every later mechanic, and item costs were deducted per
            // executing mechanic.
            RequirementResult check;
            try {
                check = requirementEngine.check(player, skill.id(), ability.id(), ability.requirements(),
                        skillLevel, ability.unlockLevel(), event);
            } catch (RuntimeException ex) {
                // A malformed requirement that slipped past load validation must
                // not abort the dispatch: log it and treat the ability as failed
                // so the remaining abilities and XP sources still fire.
                plugin.getLogger().log(Level.WARNING, "Requirement check failed for ability "
                        + ability.id() + " in skill " + skill.id(), ex);
                continue;
            }
            debug("    requirement check=" + (check.success() ? "PASS" : "FAIL"));
            if (!check.success()) {
                if (feedbackDebouncer.tryDebounce(player, skill.id(), ability.id())) {
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
            boolean procAwareRan = false;
            boolean procSucceeded = false;
            for (SkillDefinition.MechanicEntry entry : ability.mechanics()) {
                debug("    mechanic=" + entry.type() + " skill=" + skill.id());
                try {
                    SkillMechanic mechanic = mechanicRegistry.create(entry.type());
                    if (mechanic == null) {
                        debug("    -> mechanic not found in registry, skipping");
                        continue;
                    }

                    if (!matchesFilters(player, event, triggerKey, entry.filters())) {
                        debug("    -> filters failed, skipping");
                        continue;
                    }

                    Map<String, Object> evaluatedParams = evaluateParams(entry, skillLevel, ability.unlockLevel());
                    debug("    executing mechanic with params=" + evaluatedParams);
                    boolean executed = mechanic.execute(player, evaluatedParams, event);
                    if (executed) {
                        anyExecuted = true;
                        // A proc-aware mechanic whose roll landed gates
                        // feedback.success_only to the proc actually happening.
                        if (mechanic instanceof ProcAwareMechanic procAware) {
                            procAwareRan = true;
                            if (procAware.didProc()) {
                                procSucceeded = true;
                            }
                        }
                    } else {
                        debug("    -> mechanic returned false (no-op), skipping");
                    }
                } catch (Exception ex) {
                    // Isolate per-mechanic failures: a mechanic that throws during
                    // construction, filter matching, parameter evaluation, or
                    // execution must not abort the dispatch, must not skip the
                    // remaining mechanics/abilities, and must not count as an
                    // executed activation (which would spend the ability's cost and
                    // cooldown for an effect that did not happen).
                    plugin.getLogger().log(Level.WARNING, "Mechanic " + entry.type()
                            + " for ability " + ability.id() + " in skill " + skill.id()
                            + " failed during execution", ex);
                }
            }

            // Consume exactly once per activation, only when at least one
            // mechanic performed an activation attempt; a no-op ability
            // (wrong event type, missing target) must not spend its cost.
            // Chance-based mechanics return true on a failed roll, so the
            // cost/cooldown is consumed once per attempt, never per retry.
            if (!anyExecuted) {
                debug("    -> no mechanic executed, skipping consume and feedback");
                continue;
            }
            requirementEngine.consume(player, skill.id(), ability.id(), ability.requirements(),
                    skillLevel, ability.unlockLevel());

            // Success-gated feedback: when feedback.success_only is set and a
            // proc-aware mechanic ran, the cues fire only when the roll actually
            // landed. An ability with no proc-aware mechanic is unaffected, so a
            // mistaken success_only flag cannot silence every activation.
            boolean successGatePassed = !ability.feedback().successOnly()
                    || !procAwareRan
                    || procSucceeded;

            String abilityMsg = ability.feedback().message();
            boolean hasMsg = !abilityMsg.isBlank();
            if (successGatePassed && ability.feedback().actionBar() && hasMsg) {
                FanfareDispatcher.sendActionBar(player, abilityMsg);
                if (profile.getPreferences().logAbilities()) {
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(abilityMsg));
                }
            }
            if (successGatePassed && ability.feedback().chat() && hasMsg && !ability.feedback().actionBar()) {
                if (profile.getPreferences().logAbilities()) {
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(abilityMsg));
                }
            }

            double cdSec = ability.requirements().cooldown().evaluate(skillLevel, ability.unlockLevel());
            if (cdSec > 0) {
                // Clamp so a huge level-scaled cooldown cannot overflow the
                // scheduler delay and fire the ready message immediately.
                long delayTicks = (long) (io.github.chasehuegel.skilling.engine.requirements.RequirementEngine
                        .clampCooldownSeconds(cdSec) * 20);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        var branding = plugin.getBranding();
                        String color = "&f";
                        try {
                            String skillColor = skill.display() != null ? skill.display().color() : null;
                            String code = io.github.chasehuegel.skilling.engine.ui.branding.SkillColorCode.toLegacyCode(skillColor);
                            if (code != null) color = code;
                        } catch (IllegalArgumentException ignored) {}
                        String ready = io.github.chasehuegel.skilling.engine.ui.branding.TemplateRenderer.renderLine(
                                branding.abilityFeedback().readyMessage(),
                                Map.of("name", ability.displayName(), "color", color));
                        Component readyComponent = io.github.chasehuegel.skilling.engine.ui.branding.TemplateRenderer.toComponent(ready);
                        player.sendMessage(readyComponent);
                        player.sendActionBar(readyComponent);
                    }
                }, delayTicks);
            }
            if (successGatePassed && !ability.feedback().particles().isEmpty()) {
                FanfareDispatcher.dispatchParticles(player, resolveEventTargetLocation(event), ability.feedback().particles());
            }
            if (successGatePassed && !ability.feedback().sounds().isEmpty()) {
                FanfareDispatcher.dispatchSounds(player, resolveEventTargetLocation(event), ability.feedback().sounds());
            }
            debug("    -> done");
        }
    }

    private boolean matchesFilters(Player player, Event event, String triggerKey,
            List<SkillDefinition.Filter> filters) {
        if (filters == null || filters.isEmpty()) return true;
        for (SkillDefinition.Filter filter : filters) {
            boolean passed = matchFilter(player, event, triggerKey, filter);
            debug("  filter target=" + filter.target() + " state=" + filter.state()
                    + " tool=" + filter.tool() + " -> " + (passed ? "PASS" : "FAIL"));
            if (!passed) return false;
        }
        return true;
    }

    private boolean matchFilter(Player player, Event event, String triggerKey, SkillDefinition.Filter filter) {
        if (filter.target() != null && !filter.target().isBlank()) {
            Material targetMaterial = resolveEventMaterial(event, triggerKey);
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
     * Resolves the XP scalar for a source at grant time: flat sources use the
     * bulk-operation scalar (or {@code 1}), while {@code scaling: damage} sources
     * use the event's raw base damage.
     *
     * @param source the XP source
     * @param event  the triggering event
     * @return the scalar to multiply the configured reward by
     */
    static double resolveSourceScalar(SkillDefinition.XpSource source, Event event) {
        return switch (source.scaling()) {
            case NONE -> resolveEventBulkScalar(event);
            case DAMAGE -> resolveEventDamage(event);
        };
    }

    /**
     * Resolves the damage scalar for {@code scaling: damage} XP sources: the raw
     * base damage of the triggering {@link EntityDamageEvent} before mitigation
     * ({@code getDamage()}, in half-hearts). The value is clamped at {@code 0} so
     * a non-damage event (or a fully-negated hit) can never yield negative XP.
     * Applies to both incoming ({@code entity_damage_taken}, {@code fall_damage})
     * and outgoing ({@code entity_damage}) damage, since both extend
     * {@link EntityDamageEvent}.
     *
     * @param event the event to inspect
     * @return the raw base damage, or {@code 0} for non-damage events
     */
    static double resolveEventDamage(Event event) {
        if (event instanceof EntityDamageEvent e) {
            return Math.max(0, e.getDamage());
        }
        return 0;
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

    /**
     * Clamps an XP grant so the resulting total resolves to at most
     * {@code targetLevel}. Used for {@code level_up} rewards so a single level-up
     * event advances a skill by exactly one level, breaking the self-triggering
     * cascade. Binary-searches the largest total whose level does not exceed the
     * target via the public {@link SkillDefinition#getLevelForXp} (no access to
     * the internal threshold table from this package). Runs only on the level_up
     * path, never on the hot block/item loops.
     *
     * @param skill      the skill receiving the grant
     * @param currentXp  the skill's XP before the grant
     * @param requested  the raw grant amount
     * @param targetLevel the maximum level the grant may reach
     * @return the clamped grant amount (may be 0 or negative)
     */
    static long clampGrantToLevel(SkillDefinition skill, long currentXp, long requested, int targetLevel) {
        long lo = currentXp;
        long hi = currentXp + requested;
        while (lo < hi) {
            long mid = lo + (hi - lo + 1) / 2;
            if (skill.getLevelForXp(mid) > targetLevel) {
                hi = mid - 1;
            } else {
                lo = mid;
            }
        }
        return lo - currentXp;
    }

    /**
     * Resolves the target material an event carries for {@code target} filter
     * matching. Block- and item-carrying events map directly to a material;
     * air interactions carry none. The trigger key disambiguates the shared
     * {@link PlayerInteractEvent}: the legacy {@code player_interact} trigger and
     * {@code right_click_block} match a right-clicked block, {@code left_click_block}
     * matches a left-clicked block, and the air triggers never match a block.
     *
     * @param event      the triggering event
     * @param triggerKey the trigger key the event was dispatched for
     * @return the target material, or null if the event carries none
     */
    static Material resolveEventMaterial(Event event, String triggerKey) {
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
            org.bukkit.inventory.Recipe recipe = ce.getRecipe();
            if (recipe == null || recipe.getResult() == null) return null;
            return recipe.getResult().getType();
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
        if (event instanceof org.bukkit.event.entity.ProjectileHitEvent phe) {
            return projectileToMaterial(phe.getEntity());
        }
        if (event instanceof org.bukkit.event.player.PlayerInteractEvent ie) {
            // Only a block click carries a target block to filter on; air clicks
            // never match a block target. The legacy player_interact trigger and
            // right_click_block match a right-clicked block; left_click_block
            // matches a left-clicked block.
            var action = ie.getAction();
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                    && ("right_click_block".equals(triggerKey) || "player_interact".equals(triggerKey))
                    && ie.getClickedBlock() != null) {
                return ie.getClickedBlock().getType();
            }
            if (action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
                    && "left_click_block".equals(triggerKey)
                    && ie.getClickedBlock() != null) {
                return ie.getClickedBlock().getType();
            }
            return null;
        }
        if (event instanceof org.bukkit.event.block.BlockFertilizeEvent fe) {
            return fe.getBlock().getType();
        }
        return null;
    }

    /**
     * Resolves the location for ability feedback targeting, so {@code target: "target"}
     * particles and sounds fire at the interaction's block rather than the player.
     *
     * @param event the triggering event
     * @return the target block location, or null for non-block events
     */
    static org.bukkit.Location resolveEventTargetLocation(Event event) {
        return io.github.chasehuegel.skilling.engine.mechanic.impl.BlockParticlesMechanic.resolveBlockLocation(event);
    }

    /**
     * Maps a projectile entity type to the material the {@code target} filter
     * matches for projectile kills. Immutable engine plumbing (design decision,
     * ISSUE-301): it connects the entity pipeline to the material-based filter
     * system and is not author-facing content.
     */
    private static Material projectileToMaterial(org.bukkit.entity.Entity damager) {
        if (damager instanceof Projectile proj) {
            return switch (proj.getType()) {
                case ARROW -> Material.ARROW;
                case SPECTRAL_ARROW -> Material.SPECTRAL_ARROW;
                case SNOWBALL -> Material.SNOWBALL;
                case EGG -> Material.EGG;
                case TRIDENT -> Material.TRIDENT;
                case ENDER_PEARL -> Material.ENDER_PEARL;
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
            if (evaluator instanceof ConstantEvaluator constant) {
                // A string-valued constant (e.g. an effect key) is emitted
                // verbatim; a numeric constant carries its number.
                result.put(paramEntry.getKey(), constant.rawValue());
            } else {
                result.put(paramEntry.getKey(), evaluator.evaluate(level, unlockLevel));
            }
        }
        return result;
    }

    /**
     * Retroactively executes the persistent unlock mechanics of every milestone
     * ability the player has already satisfied.
     *
     * <p>Runs on player join and after {@code /skills reload} so a player who
     * reached a {@code level_up} milestone before it was configured — or whose
     * level was set by an offline admin command — is caught up without needing a
     * live level-up. Only {@code level_up}-triggered abilities whose owning-skill
     * level meets {@code unlock_level} are considered, and only mechanics marked
     * as {@code UnlockMechanic} run: costed/cooldown abilities must never fire
     * outside their event. The vanilla recipe book guard makes each unlock a
     * no-op once granted, so re-running is safe.
     *
     * <p>The {@code event} passed to the unlock mechanic is {@code null}, per the
     * {@code UnlockMechanic} contract. Unlocks are silent here; in-session
     * milestone feedback flows through the normal ability dispatch instead.
     *
     * @param player  the online player to reconcile
     * @param profile the player's live profile (XP already hydrated)
     */
    public void reconcileMilestoneUnlocks(Player player, PlayerProfile profile) {
        // Recompute persistent effects from scratch: strip every leftover
        // persistent attribute modifier before re-applying the active ones, so a
        // de-level, a reset, or a removed/renamed skill cannot leave a stale
        // bonus (e.g. extra max hearts) on the player.
        io.github.chasehuegel.skilling.engine.mechanic.impl.PersistentAttributeMechanic.stripPersistentModifiers(player);
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            int skillLevel = skill.getLevelForXp(profile.getXp(skill.id()));
            for (SkillDefinition.Ability ability : skill.abilities()) {
                if (!"level_up".equals(ability.trigger())) continue;
                if (skillLevel < ability.unlockLevel()) continue;
                for (SkillDefinition.MechanicEntry entry : ability.mechanics()) {
                    if (!mechanicRegistry.isUnlock(entry.type())) continue;
                    try {
                        SkillMechanic mechanic = mechanicRegistry.create(entry.type());
                        if (mechanic == null) continue;
                        Map<String, Object> params = evaluateParams(entry, skillLevel, ability.unlockLevel());
                        mechanic.execute(player, params, null);
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.WARNING, "Unlock mechanic " + entry.type()
                                + " for ability " + ability.id() + " in skill " + skill.id()
                                + " failed during join/reload reconciliation", ex);
                    }
                }
            }
        }
    }



    private void debug(String msg) {
        plugin.debug(msg);
    }

}
