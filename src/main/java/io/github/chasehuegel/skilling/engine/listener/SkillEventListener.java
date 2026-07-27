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
import org.bukkit.event.inventory.BrewEvent;
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

public final class SkillEventListener implements Listener {

    private final Skilling plugin;
    private final SkillManager skillManager;
    private final ProfileManager profileManager;
    private final TagResolver tagResolver;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        debug("block_break fired for " + event.getPlayer().getName()
                + " breaking " + event.getBlock().getType());
        dispatch(event.getPlayer(), event, "block_break");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        event.getBlockPlaced().setMetadata("player_placed", new FixedMetadataValue(plugin, true));
        dispatch(event.getPlayer(), event, "block_place");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            dispatch(player, event, "entity_damage");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageTaken(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            dispatch(player, event, "entity_damage_taken");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player player) {
            dispatch(player, event, "entity_kill");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            dispatch(player, event, "craft_item");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        dispatch(event.getPlayer(), event, "furnace_extract");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewPotion(BrewEvent event) {
        if (event.getContents().getHolder() instanceof org.bukkit.block.BrewingStand stand) {
            var location = stand.getLocation();
            if (location.getWorld() != null) {
                var players = location.getWorld().getNearbyPlayers(location, 5, p -> true);
                for (Player player : players) {
                    dispatch(player, event, "brew_potion");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        dispatch(event.getPlayer(), event, "player_interact");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsumeItem(PlayerItemConsumeEvent event) {
        dispatch(event.getPlayer(), event, "consume_item");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        dispatch(event.getPlayer(), event, "fishing");
    }

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreedAnimals(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            dispatch(player, event, "breed_animals");
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
                double xp = source.reward().evaluate(1, 1) * plugin.getGlobalXpModifier();
                if (xp > 0) {
                    long rounded = Math.round(xp);
                    int oldLevel = getLevelForXp(skill, profile.getXp(skill.id()));
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
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(abilityMsg));
                    }
                    if (ability.feedback().chat() && hasMsg && !ability.feedback().actionBar()) {
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(abilityMsg));
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByFirework(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Firework fw
                && fw.getPersistentDataContainer().has(Skilling.FIREWORK_KEY, PersistentDataType.BOOLEAN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getHitEntity() == null) return;
        if (!(event.getEntity() instanceof Projectile projectile)) return;
        if (!projectile.hasMetadata("skilling_damage")) return;
        double damage = projectile.getMetadata("skilling_damage").get(0).asDouble();
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
