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
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
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
                double xp = source.reward().evaluate(1, 1);
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
        String skillId = skill.id();
        long totalXp = profile.getXp(skillId);
        int level = getLevelForXp(skill, totalXp);
        int maxLevel = skill.maxLevel();
        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skillId;

        BossBar bar = bossBarPool.getOrCreate(player, skillId);

        TextColor textColor = resolveBarColor(skill.display() != null ? skill.display().color() : null);
        Component title;

        if (level >= maxLevel) {
            title = Component.text(displayName + " - Maxed!", NamedTextColor.GOLD);
        } else {
            long xpForCurrent = (long) skill.progression().evaluator().evaluate(level, 0);
            long xpForNext = (long) skill.progression().evaluator().evaluate(level + 1, 0);
            long intoLevel = totalXp - xpForCurrent;
            long needed = xpForNext - xpForCurrent;
            double progress = needed > 0 ? Math.min((double) intoLevel / needed, 1.0) : 0;
            bar.setProgress(progress);

            Component nameComp = Component.text(displayName,
                    textColor != null ? textColor : NamedTextColor.WHITE);
            title = nameComp
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(level), NamedTextColor.WHITE));
            if (plugin.isDebugLogging()) {
                title = title
                        .append(Component.text(" (", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(intoLevel), NamedTextColor.WHITE))
                        .append(Component.text("/", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(needed), NamedTextColor.WHITE))
                        .append(Component.text(")", NamedTextColor.GRAY));
            }
        }

        bar.setTitle(LegacyComponentSerializer.legacySection().serialize(title));

        if (skill.display() != null) {
            try {
                bar.setColor(BarColor.valueOf(skill.display().color()));
            } catch (IllegalArgumentException ignored) {}
            try {
                bar.setStyle(BarStyle.valueOf(skill.display().style()));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static TextColor resolveBarColor(String colorName) {
        if (colorName == null || colorName.isBlank()) return null;
        return switch (colorName.toUpperCase()) {
            case "PINK" -> NamedTextColor.LIGHT_PURPLE;
            case "PURPLE" -> NamedTextColor.DARK_PURPLE;
            case "RED" -> NamedTextColor.RED;
            case "GREEN" -> NamedTextColor.GREEN;
            case "BLUE" -> NamedTextColor.BLUE;
            case "WHITE" -> NamedTextColor.WHITE;
            case "YELLOW" -> NamedTextColor.YELLOW;
            default -> null;
        };
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

                    RequirementResult check = requirementEngine.check(player, ability.requirements());
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
                    requirementEngine.consume(player, ability.requirements());

                    if (ability.feedback().actionBar() && !ability.feedback().message().isBlank()) {
                        FanfareDispatcher.sendActionBar(player, ability.feedback().message());
                    }
                    if (ability.feedback().chat() && !ability.feedback().message().isBlank()) {
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(ability.feedback().message()));
                    }
                    if (!ability.feedback().particles().isEmpty()) {
                        FanfareDispatcher.dispatchParticles(player, null, ability.feedback().particles());
                    }
                    if (!ability.feedback().sounds().isEmpty()) {
                        FanfareDispatcher.dispatchSounds(player, ability.feedback().sounds());
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

    private static final org.bukkit.Color[] BRIGHT_COLORS = {
            org.bukkit.Color.RED, org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW,
            org.bukkit.Color.LIME, org.bukkit.Color.GREEN, org.bukkit.Color.AQUA,
            org.bukkit.Color.BLUE, org.bukkit.Color.PURPLE, org.bukkit.Color.FUCHSIA
    };

    private void broadcastLevelUp(Player player, SkillDefinition skill, int newLevel) {
        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skill.id();
        boolean major = isMajorLevelUp(skill, newLevel);
        var unlockedAbilities = skill.abilities().stream()
                .filter(a -> a.unlockLevel() == newLevel)
                .toList();

        String levelUpMsg = "<gray>[</gray><gold>Level Up!</gold><gray>]</gray> <yellow>" + displayName + " increased to " + newLevel + "</yellow>";
        player.sendMessage(MiniMessage.miniMessage().deserialize(levelUpMsg));
        int stayMs = plugin.getTitleStayDuration();
        player.showTitle(Title.title(
                MiniMessage.miniMessage().deserialize("<gold><bold>Level up!</bold></gold>"),
                MiniMessage.miniMessage().deserialize("<yellow>" + displayName + " increased to " + newLevel + "</yellow>"),
                Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(stayMs),
                        java.time.Duration.ofMillis(500)
                )
        ));

        long firstDelay = Math.min(stayMs + 500L, 3000L) / 50L;
        for (int i = 0; i < unlockedAbilities.size(); i++) {
            int idx = i;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Component line = SkillMenuBuilder.formatAbilityLine(unlockedAbilities.get(idx), newLevel);
                String unlockMsg = "<gray>[</gray><aqua>Ability Unlocked!</aqua><gray>]</gray> ";
                player.sendMessage(MiniMessage.miniMessage().deserialize(unlockMsg).append(line));
                player.showTitle(Title.title(
                        MiniMessage.miniMessage().deserialize("<gold><bold>New unlock!</bold></gold>"),
                        line.colorIfAbsent(NamedTextColor.WHITE),
                        Title.Times.times(
                                java.time.Duration.ZERO,
                                java.time.Duration.ofMillis(1500),
                                java.time.Duration.ofMillis(500)
                        )
                ));
            }, firstDelay + idx * 40L);
        }

        if (major) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    org.bukkit.SoundCategory.PLAYERS, 1.0f, 1.2f);
            spawnFirework(player.getLocation(), randomBrightColor(),
                    org.bukkit.FireworkEffect.Type.BURST, 3);
            spawnFirework(player.getLocation(), randomBrightColor(),
                    org.bukkit.FireworkEffect.Type.STAR, 2);
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                    org.bukkit.SoundCategory.PLAYERS, 1.0f, 1.0f);
            spawnFirework(player.getLocation(), randomBrightColor(),
                    org.bukkit.FireworkEffect.Type.BURST, 1);
        }

        StringBuilder logMsg = new StringBuilder("Level up! " + player.getName() + "'s " + skill.id() + " increased to " + newLevel);
        for (SkillDefinition.Ability a : unlockedAbilities) {
            logMsg.append("\n  ").append(
                    LegacyComponentSerializer.legacySection().serialize(
                            SkillMenuBuilder.formatAbilityLine(a, newLevel)));
        }
        plugin.getLogger().info(logMsg.toString());
    }

    private static boolean isMajorLevelUp(SkillDefinition skill, int newLevel) {
        return skill.abilities().stream().anyMatch(a -> a.unlockLevel() == newLevel);
    }

    private static org.bukkit.Color randomBrightColor() {
        return BRIGHT_COLORS[(int) (Math.random() * BRIGHT_COLORS.length)];
    }

    private static void spawnFirework(org.bukkit.Location location, org.bukkit.Color color,
                                       org.bukkit.FireworkEffect.Type type, int count) {
        var fwLoc = location.clone().add(
                (Math.random() - 0.5) * 2, 3, (Math.random() - 0.5) * 2);
        for (int i = 0; i < count; i++) {
            org.bukkit.entity.Firework fw = fwLoc.getWorld().spawn(fwLoc,
                    org.bukkit.entity.Firework.class);
            fw.getPersistentDataContainer().set(Skilling.FIREWORK_KEY, PersistentDataType.BOOLEAN, true);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                    .withColor(color)
                    .with(type)
                    .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
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
