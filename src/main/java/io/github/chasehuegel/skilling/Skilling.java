package io.github.chasehuegel.skilling;

import io.github.chasehuegel.skilling.api.Registries;
import io.github.chasehuegel.skilling.api.SkillingAPI;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.AbilityManager;
import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.mechanic.impl.*;
import io.github.chasehuegel.skilling.engine.trigger.impl.*;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.command.SkillsCommand;
import io.github.chasehuegel.skilling.engine.integration.IntegrationManager;
import io.github.chasehuegel.skilling.engine.listener.PlayerListener;
import io.github.chasehuegel.skilling.engine.listener.SkillEventListener;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.github.chasehuegel.skilling.engine.ui.GuiLayoutConfig;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import io.github.chasehuegel.skilling.engine.ui.SkillsGuideBook;
import io.github.chasehuegel.skilling.engine.ui.UIProtectionListener;
import io.github.chasehuegel.skilling.engine.ui.branding.BrandingConfig;
import io.github.chasehuegel.skilling.web.WebServer;
import io.github.chasehuegel.skilling.web.config.WebConfig;
import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * Main plugin class for Skilling — a data-driven RPG skills engine for PaperMC.
 *
 * Initializes all registries during {@link #onEnable()} and performs a
 * synchronous database flush during {@link #onDisable()}.
 */
public final class Skilling extends JavaPlugin {

    private static Skilling instance;

    /** Key used to tag fireworks spawned by Skilling for visual-only damage suppression. */
    public static final NamespacedKey FIREWORK_KEY = NamespacedKey.fromString("skilling:visual_firework");

    /**
     * Key used to stamp the firing player's sneak state onto an arrow as it is
     * released. The {@code was_sneaking} state filter reads this stamp back at
     * impact time, so a "sneak-shot" condition reflects the stance used to draw
     * and release the bow rather than the player's stance when the arrow lands.
     */
    public static final NamespacedKey SHOT_SNEAK_KEY = NamespacedKey.fromString("skilling:shot_sneak");

    private static final String CONFIG_DEBUG_LOGGING = "debug_logging";
    private static final String CONFIG_TITLES_STAY_DURATION = "titles.stay_duration";
    private static final String CONFIG_GLOBAL_XP_MODIFIER = "global_xp_modifier";
    private static final String CONFIG_DEBOUNCER_INTERVAL_MS = "debouncer.interval_ms";
    private static final String CONFIG_BOSSBAR_MAX_ACTIVE = "bossbar.max_active";
    private static final String CONFIG_BOSSBAR_FADE_TICKS = "bossbar.fade_ticks";
    private static final String CONFIG_CROP_GROW_RADIUS = "crop_grow.search_radius";

    private Registries registries;
    private StateFilterRegistry stateFilterRegistry;
    private DatabaseManager databaseManager;
    private ProfileManager profileManager;
    private AsyncBatchWorker asyncBatchWorker;
    private SkillManager skillManager;
    private GuiLayoutConfig guiLayoutConfig;
    private SkillMenuBuilder skillMenuBuilder;
    private RequirementEngine requirementEngine;
    private FeedbackDebouncer feedbackDebouncer;
    private BossBarPool bossBarPool;
    private LockdownManager lockdownManager;
    private SkillsCommand skillsCommand;
    private CustomTagLoader customTagLoader;
    private volatile AbilityManager abilityManager;
    private volatile TagResolver tagResolver;
    private volatile EntityTagResolver entityTagResolver;
    private SkillEventListener skillEventListener;
    private WebServer webServer;
    private IntegrationManager integrationManager;
    private SkillsGuideBook skillsGuideBook;
    private volatile boolean reloading;
    private volatile boolean debugLogging;
    private volatile int titleStayDuration;
    private volatile double globalXpModifier;
    private volatile int cropGrowRadius;
    private volatile BrandingConfig branding;

    /**
     * Returns the plugin singleton instance.
     *
     * @return the Skilling plugin instance
     */
    public static Skilling getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.reloading = false;

        saveDefaultConfig();
        reloadConfig();

        var config = (YamlConfiguration) getConfig();
        boolean firstRun = config.getBoolean("setup.first_run", true);

        if (firstRun) {
            File tagsDir = new File(getDataFolder(), "tags");
            if (!tagsDir.exists() && !tagsDir.mkdirs()) {
                getLogger().warning("Could not create tags data directory: " + tagsDir);
            }
            if (!new File(tagsDir, "base.yml").exists()) {
                getLogger().info("Generating default tags/base.yml...");
                saveResource("tags/base.yml", false);
            }
            if (!new File(getDataFolder(), "template-skill.yml").exists()) {
                getLogger().info("Generating default template-skill.yml...");
                saveResource("template-skill.yml", false);
            }
            File abilitiesDir = new File(getDataFolder(), "abilities");
            if (!abilitiesDir.exists() && !abilitiesDir.mkdirs()) {
                getLogger().warning("Could not create abilities data directory: " + abilitiesDir);
            }
            if (!new File(abilitiesDir, "vein_miner.yml").exists()) {
                getLogger().info("Generating default abilities/vein_miner.yml...");
                saveResource("abilities/vein_miner.yml", false);
            }
            if (!new File(getDataFolder(), "gui.yml").exists()) {
                getLogger().info("Generating default gui.yml...");
                saveResource("gui.yml", false);
            }

            String[] bundledSkills = {"mining.yml", "woodcutting.yml", "excavation.yml",
                                      "farming.yml", "fishing.yml", "archery.yml",
                                      "carpentry.yml", "masonry.yml", "tailoring.yml",
                                      "building.yml", "cooking.yml", "smithing.yml",
                                      "herbalism.yml", "heavy_weapons.yml", "light_weapons.yml",
                                      "heavy_armor.yml", "medium_armor.yml", "light_armor.yml",
                                      "alchemy.yml", "enchanting.yml", "riding.yml",
                                      "unarmed.yml", "one_handed.yml", "dual_wield.yml",
                                      "shields.yml", "unarmored.yml", "husbandry.yml",
                                      "throwing.yml", "acrobatics.yml", "piety.yml",
                                      "bard.yml", "wizardry.yml", "survival.yml",
                                      "trade.yml", "exploration.yml", "stealth.yml"};
            for (String skill : bundledSkills) {
                if (!new File(getDataFolder(), "skills/" + skill).exists()) {
                    getLogger().info("Generating default " + skill + "...");
                    saveResource("skills/" + skill, false);
                }
            }

            // Seed the bundled datapacks into the plugin data folder so the
            // bootstrap can discover them. These are ordinary files: an admin
            // who deletes one simply stops it being served.
            File datapacksDir = new File(getDataFolder(), "datapacks");
            if (!datapacksDir.exists() && !datapacksDir.mkdirs()) {
                getLogger().warning("Could not create datapacks data directory: " + datapacksDir);
            }
            if (!new File(datapacksDir, "stealth.zip").exists()) {
                getLogger().info("Generating default datapacks/stealth.zip...");
                saveResource("datapacks/stealth.zip", false);
            }
            if (!new File(datapacksDir, "carpentry.zip").exists()) {
                getLogger().info("Generating default datapacks/carpentry.zip...");
                saveResource("datapacks/carpentry.zip", false);
            }

            // Mark setup as complete so bundled files are not regenerated on subsequent starts
            config.set("setup.first_run", false);
            saveConfig();

            // Enabling a datapack reloads data, so defer it until the server has
            // finished loading. Only needed on the first run, when the bootstrap's
            // discovery pass may already have run before the zip was copied.
            getServer().getScheduler().runTaskLater(this, this::enableBundledDatapacks, 200L);
        }

        this.debugLogging = config.getBoolean(CONFIG_DEBUG_LOGGING, false);
        if (debugLogging) {
            getLogger().info("Debug logging enabled.");
        }
        this.titleStayDuration = config.getInt(CONFIG_TITLES_STAY_DURATION, 5000);
        this.globalXpModifier = config.getDouble(CONFIG_GLOBAL_XP_MODIFIER, 1.0);
        this.cropGrowRadius = config.getInt(CONFIG_CROP_GROW_RADIUS, 10);
        this.branding = BrandingConfig.from(config.getConfigurationSection("branding"));

        this.registries = new Registries(
                new MechanicRegistry(),
                new TriggerRegistry(),
                new EvaluatorRegistry()
        );
        this.stateFilterRegistry = new StateFilterRegistry();
        this.customTagLoader = new CustomTagLoader();
        customTagLoader.loadDirectory(new File(getDataFolder(), "tags"));
        this.tagResolver = new TagResolver(customTagLoader);
        this.entityTagResolver = new EntityTagResolver(customTagLoader);
        registerBuiltins();

        // Requirements engine
        this.requirementEngine = new RequirementEngine(tagResolver, stateFilterRegistry);

        // Initialize database
        this.databaseManager = new DatabaseManager(getDataFolder());
        try {
            databaseManager.initialize(config);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database; disabling Skilling", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.profileManager = new ProfileManager(databaseManager);
        this.asyncBatchWorker = new AsyncBatchWorker(this, databaseManager, profileManager, requirementEngine);
        this.asyncBatchWorker.start();

        // Load skill definitions from YAML
        var tagResolver = this.tagResolver;
        this.skillManager = new SkillManager(
                registries.getEvaluatorRegistry(),
                registries.getMechanicRegistry(),
                registries.getTriggerRegistry(),
                tagResolver,
                stateFilterRegistry
        );
        this.abilityManager = new AbilityManager();
        abilityManager.loadAbilities(new File(getDataFolder(), "abilities"));
        skillManager.setAbilityManager(abilityManager);
        loadSkills();

        // UI
        this.guiLayoutConfig = GuiLayoutConfig.load();
        this.skillMenuBuilder = new SkillMenuBuilder(skillManager, guiLayoutConfig);

        // Skills Guide Book
        this.skillsGuideBook = new SkillsGuideBook(this, profileManager, skillMenuBuilder);
        this.skillsGuideBook.register();

        // Feedback systems
        int debounceMs = config.getInt(CONFIG_DEBOUNCER_INTERVAL_MS, 500);
        this.feedbackDebouncer = new FeedbackDebouncer(debounceMs);
        int maxBars = config.getInt(CONFIG_BOSSBAR_MAX_ACTIVE, 2);
        int fadeTicks = config.getInt(CONFIG_BOSSBAR_FADE_TICKS, 40);
        this.bossBarPool = new BossBarPool(maxBars, fadeTicks,
                branding.bossBar().defaultColor(), branding.bossBar().defaultStyle());

        // Lockdown / reload manager
        this.lockdownManager = new LockdownManager(this, profileManager, asyncBatchWorker, skillManager);

        // Web GUI
        ensureWebPassword(config);
        WebConfig webConfig = WebConfig.load(config);
        var stagingManager = new io.github.chasehuegel.skilling.web.staging.StagingManager(getDataFolder());
        this.webServer = new WebServer(this, webConfig, skillManager, stagingManager, lockdownManager);
        this.webServer.start();

        // External integrations
        this.integrationManager = new IntegrationManager(this);
        integrationManager.initialize();

        // Commands
        this.skillsCommand = new SkillsCommand(this, skillManager, profileManager, skillMenuBuilder,
                lockdownManager, bossBarPool);
        this.skillsCommand.register();

        // Event listeners
        Bukkit.getPluginManager().registerEvents(new UIProtectionListener(), this);
        this.skillEventListener = new SkillEventListener(this, skillManager, profileManager, tagResolver, requirementEngine,
                        registries.getMechanicRegistry(), feedbackDebouncer, bossBarPool, stateFilterRegistry);
        Bukkit.getPluginManager().registerEvents(skillEventListener, this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(profileManager, asyncBatchWorker, skillManager, bossBarPool, feedbackDebouncer, skillEventListener), this);

        // BossBar TTL tick loop (every tick so fadeTicks config is in game ticks)
        Bukkit.getScheduler().runTaskTimer(this, bossBarPool::tickAll, 1L, 1L);

        // API service
        Bukkit.getServicesManager().register(
                SkillingAPI.class,
                new SkillingAPI(registries, profileManager, skillManager, skillMenuBuilder, requirementEngine, feedbackDebouncer, bossBarPool),
                this,
                ServicePriority.Normal
        );

        var mechCount = registries.getMechanicRegistry().size();
        var trigCount = registries.getTriggerRegistry().size();
        var evalCount = registries.getEvaluatorRegistry().size();
        var skillCount = skillManager.getSkills().size();
        var tagCount = customTagLoader.getKeys().size();
        var entityTagCount = customTagLoader.getEntityKeys().size();
        getLogger().info("Loaded " + skillCount + " skill(s) | " + mechCount + " mechanic(s) | "
                + trigCount + " trigger(s) | " + evalCount + " evaluator(s) | "
                + tagCount + " custom tag(s) | " + entityTagCount + " custom entity tag(s)");
        getLogger().info("Skilling v" + getPluginMeta().getVersion() + " enabled.");
    }

    public void registerBuiltins() {
        registerBuiltinEvaluators(registries.getEvaluatorRegistry());
        registerBuiltinMechanics(registries.getMechanicRegistry());
        registerBuiltinTriggers(registries.getTriggerRegistry());
        registerBuiltinStateFilters(stateFilterRegistry, tagResolver, entityTagResolver);
        // Wire load-time mechanic validation to the live registries. Done here
        // (after mechanics register their validators) so a YAML typo fails at
        // load instead of inside an event handler.
        MechanicParamValidators.configureLookups(
                key -> Registry.POTION_EFFECT_TYPE.get(key) != null,
                key -> Registry.ATTRIBUTE.get(key) != null,
                key -> Registry.SOUND_EVENT.get(key) != null,
                key -> Registry.PARTICLE_TYPE.get(key) != null,
                Skilling::isRecipeRegistered,
                tagResolver::isKnown);
    }

    /**
     * Whether a recipe with the given namespaced key is registered on this server.
     * Recipe registration is dynamic (data packs and other plugins load and
     * unload recipes), so this is queried at skill load and, for missing keys,
     * once more when an unlock executes.
     *
     * @param key the recipe's namespaced key
     * @return true if the recipe is currently registered
     */
    private static boolean isRecipeRegistered(NamespacedKey key) {
        return Bukkit.getServer().getRecipe(key) != null;
    }

    /** Registers the built-in parameter evaluators into the given registry. */
    public static void registerBuiltinEvaluators(EvaluatorRegistry evalReg) {
        evalReg.register("linear", new LinearEvaluator(0, 1, 0, Double.MAX_VALUE));
        evalReg.register("constant", new ConstantEvaluator(0));
        evalReg.register("milestone", new MilestoneEvaluator(new TreeMap<>()));
        evalReg.register("polynomial", new PolynomialEvaluator(50, 2.5));
    }

    /** Registers the built-in mechanics into the given registry. */
    public static void registerBuiltinMechanics(MechanicRegistry mechReg) {
        mechReg.register("core:yield_multiplier", YieldMultiplierMechanic.class, List.of("yield_chance", "triple_chance"),
                (ctx, p) -> {
                    MechanicParamValidators.chance(ctx, p, "yield_chance", 100);
                    MechanicParamValidators.chance(ctx, p, "triple_chance", 100);
                });
        mechReg.register("core:chain_break", ChainBreakMechanic.class, List.of("chain_limit", "target"),
                (ctx, p) -> MechanicParamValidators.materialOrTag(ctx, p, "target"));
        mechReg.register("core:level_break", LevelBreakMechanic.class, List.of("chain_limit", "target"),
                (ctx, p) -> MechanicParamValidators.materialOrTag(ctx, p, "target"));
        mechReg.register("core:modify_damage", ModifyDamageMechanic.class, List.of("multiplier"));
        mechReg.register("core:apply_status", ApplyStatusMechanic.class, List.of("effect", "duration", "amplifier"),
                (ctx, p) -> {
                    MechanicParamValidators.potionEffect(ctx, p, "effect");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:cancel_damage", DamageCancelMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:modify_attribute", ModifyAttributeMechanic.class, List.of("attribute", "amount", "duration", "uuid"),
                (ctx, p) -> {
                    MechanicParamValidators.attribute(ctx, p, "attribute");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:modify_craft_output", ModifyCraftOutputMechanic.class, List.of("multiplier"));
        mechReg.register("core:modify_furnace_output", ModifyFurnaceOutputMechanic.class, List.of("multiplier"));
        mechReg.register("core:saturation_inject", SaturationInjectMechanic.class, List.of("saturation"));
        mechReg.register("core:modify_brew_time", ModifyBrewTimeMechanic.class, List.of("multiplier"));
        mechReg.register("core:modify_potion_duration", ModifyPotionDurationMechanic.class, List.of("multiplier"));
        mechReg.register("core:modify_potion_amplifier", ModifyPotionAmplifierMechanic.class, List.of("amplifier"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "amplifier"));
        mechReg.register("core:modify_brew_output", ModifyBrewOutputMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:potion_self_immunity", PotionSelfImmunityMechanic.class, List.of());
        mechReg.register("core:transmute", TransmuteMechanic.class, List.of("source", "product", "source_count", "product_count"),
                (ctx, p) -> {
                    MechanicParamValidators.material(ctx, p, "source");
                    MechanicParamValidators.material(ctx, p, "product");
                    MechanicParamValidators.positive(ctx, p, "source_count");
                    MechanicParamValidators.positive(ctx, p, "product_count");
                });
        mechReg.register("core:aoe_effect", AoeEffectMechanic.class, List.of("effect", "radius", "duration", "amplifier", "targets"),
                (ctx, p) -> {
                    MechanicParamValidators.potionEffect(ctx, p, "effect");
                    MechanicParamValidators.radius(ctx, p, "radius");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:projectile", ProjectileMechanic.class, List.of("speed", "damage"));
        mechReg.register("core:teleport", TeleportMechanic.class, List.of("range"));
        mechReg.register("core:block_damage", DamageCancelMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:thorns_damage", ThornsDamageMechanic.class, List.of("damage"));
        mechReg.register("core:knockback", KnockbackMechanic.class, List.of("force", "radius", "vertical", "targets"),
                (ctx, p) -> MechanicParamValidators.radius(ctx, p, "radius"));
        mechReg.register("core:shield_disable", ShieldDisableMechanic.class, List.of("ticks", "target"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "ticks"));
        mechReg.register("core:offhand_strike", OffhandStrikeMechanic.class, List.of("multiplier", "reach", "targets"));
        mechReg.register("core:offhand_swing", OffhandSwingMechanic.class, List.of());
        mechReg.register("core:damage", DamageMechanic.class, List.of("damage", "percent"));
        mechReg.register("core:true_damage", TrueDamageMechanic.class, List.of("damage", "percent"));
        mechReg.register("core:set_cooldown", SetCooldownMechanic.class, List.of("material", "ticks"),
                (ctx, p) -> {
                    MechanicParamValidators.material(ctx, p, "material");
                    MechanicParamValidators.nonNegative(ctx, p, "ticks");
                });
        mechReg.register("core:modify_attack_speed", ModifyAttackSpeedMechanic.class, List.of("multiplier", "duration", "uuid"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "duration"));
        mechReg.register("core:dodge", DamageCancelMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:lifesteal", LifestealMechanic.class, List.of("percentage"));
        mechReg.register("core:armor_bonus", ArmorBonusMechanic.class, List.of("amount", "duration", "uuid"),
                (ctx, p) -> {
                    MechanicParamValidators.nonNegative(ctx, p, "amount");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:knockback_resist", KnockbackResistMechanic.class, List.of("amount", "duration", "uuid"),
                (ctx, p) -> {
                    MechanicParamValidators.nonNegative(ctx, p, "amount");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:crowd_control", CrowdControlMechanic.class, List.of("effect", "duration", "amplifier", "radius", "targets"),
                (ctx, p) -> {
                    MechanicParamValidators.potionEffect(ctx, p, "effect");
                    MechanicParamValidators.radius(ctx, p, "radius");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:execute", ExecuteMechanic.class, List.of("threshold"));
        mechReg.register("core:auto_smelt", AutoSmeltMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:speed_bonus", SpeedBonusMechanic.class, List.of("multiplier", "duration", "uuid"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "duration"));
        mechReg.register("core:xp_bonus", XpBonusMechanic.class, List.of("multiplier", "duration"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "duration"));
        mechReg.register("core:fishing_yield", FishingYieldMechanic.class, List.of("yield_chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "yield_chance", 100));
        mechReg.register("core:fishing_loot", FishingLootMechanic.class, List.of("multiplier"));
        mechReg.register("core:area_harvest", AreaHarvestMechanic.class, List.of("radius", "max_blocks"));
        mechReg.register("core:area_fertilize", AreaFertilizeMechanic.class, List.of("radius"),
                (ctx, p) -> MechanicParamValidators.radius(ctx, p, "radius"));
        mechReg.register("core:auto_replant", AutoReplantMechanic.class, List.of());
        mechReg.register("core:durability_save", DurabilitySaveMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:haste_effect", HasteMechanic.class, List.of("amplifier", "duration"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "duration"));
        mechReg.register("core:repair_discount", RepairDiscountMechanic.class, List.of("discount"));
        mechReg.register("core:modify_tame_chance", ModifyTameChanceMechanic.class, List.of("multiplier"),
                (ctx, p) -> MechanicParamValidators.positive(ctx, p, "multiplier"));
        mechReg.register("core:instant_tame", InstantTameMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:projectile_return", ProjectileReturnMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:modify_enchant_cost", ModifyEnchantCostMechanic.class, List.of("discount"));
        mechReg.register("core:field_aura", FieldAuraMechanic.class, List.of("effect", "radius", "duration", "amplifier", "targets"),
                (ctx, p) -> {
                    MechanicParamValidators.potionEffect(ctx, p, "effect");
                    MechanicParamValidators.radius(ctx, p, "radius");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:ally_aura", AllyAuraMechanic.class, List.of("effect", "radius", "duration", "amplifier"),
                (ctx, p) -> {
                    MechanicParamValidators.potionEffect(ctx, p, "effect");
                    MechanicParamValidators.radius(ctx, p, "radius");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:modify_jump", ModifyJumpMechanic.class, List.of("multiplier", "duration", "uuid"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "duration"));
        mechReg.register("core:block_particles", BlockParticlesMechanic.class, List.of("particle", "count", "speed"),
                (ctx, p) -> MechanicParamValidators.particle(ctx, p, "particle"));
        mechReg.register("core:unlock_recipe", UnlockRecipeMechanic.class, List.of("recipe"),
                (ctx, p) -> MechanicParamValidators.recipe(ctx, p, "recipe"));
        mechReg.register("core:persistent_attribute", PersistentAttributeMechanic.class, List.of("attribute", "amount", "uuid"),
                (ctx, p) -> {
                    MechanicParamValidators.attribute(ctx, p, "attribute");
                    MechanicParamValidators.uuid(ctx, p, "uuid");
                    MechanicParamValidators.nonNegative(ctx, p, "amount");
                });
        mechReg.register("core:trade_bonus", TradeBonusMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:villager_xp", VillagerXpMechanic.class, List.of("amount"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "amount"));
        mechReg.register("core:summon_villager", SummonVillagerMechanic.class, List.of());
        mechReg.register("core:barter_luck", BarterLuckMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:reroll_trades", RerollTradesMechanic.class, List.of());
        mechReg.register("core:summon_wandering_trader", SummonWanderingTraderMechanic.class, List.of());
        mechReg.register("core:pick_up_mob", PickUpMobMechanic.class, List.of("max_passengers"),
                (ctx, p) -> MechanicParamValidators.nonNegative(ctx, p, "max_passengers"));
        mechReg.register("core:drop_passengers", DropPassengersMechanic.class, List.of());
        mechReg.register("core:sneak_speed", SneakSpeedMechanic.class, List.of("multiplier", "uuid"));
        mechReg.register("core:sneak_effect", SneakEffectMechanic.class, List.of("effect", "amplifier", "duration"),
                (ctx, p) -> {
                    MechanicParamValidators.potionEffect(ctx, p, "effect");
                    MechanicParamValidators.nonNegative(ctx, p, "amplifier");
                    MechanicParamValidators.nonNegative(ctx, p, "duration");
                });
        mechReg.register("core:cancel_event", CancelEventMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:drop_loot", DropLootMechanic.class, List.of("table", "chance"),
                (ctx, p) -> {
                    MechanicParamValidators.chance(ctx, p, "chance", 100);
                    Object raw = p.get("table");
                    boolean validKey;
                    try {
                        validKey = raw instanceof String s && NamespacedKey.fromString(s) != null;
                    } catch (IllegalArgumentException ex) {
                        validKey = false;
                    }
                    if (!validKey) {
                        throw new IllegalArgumentException(ctx + ": parameter 'table' must be a namespaced loot table key, got: " + raw);
                    }
                });
        mechReg.register("core:block_refund", BlockRefundMechanic.class, List.of("chance"),
                (ctx, p) -> MechanicParamValidators.chance(ctx, p, "chance", 100));
        mechReg.register("core:marked_demolition", MarkedDemolitionMechanic.class, List.of("target"),
                (ctx, p) -> MechanicParamValidators.materialOrTag(ctx, p, "target"));
        mechReg.register("core:elytra_flight", ElytraFlightMechanic.class, List.of());
        mechReg.register("core:open_crafting", OpenCraftingMechanic.class, List.of());
    }

    /** Registers the built-in triggers into the given registry. */
    public static void registerBuiltinTriggers(TriggerRegistry trigReg) {
        trigReg.register("block_break", BlockBreakTrigger.class);
        trigReg.register("block_place", BlockPlaceTrigger.class);
        trigReg.register("entity_damage", EntityDamageTrigger.class);
        trigReg.register("entity_damage_taken", EntityDamageTakenTrigger.class);
        trigReg.register("fall_damage", FallDamageTrigger.class);
        trigReg.register("entity_kill", EntityKillTrigger.class);
        trigReg.register("craft_item", CraftItemTrigger.class);
        trigReg.register("furnace_extract", FurnaceExtractTrigger.class);
        trigReg.register("brew_potion", BrewPotionTrigger.class);
        trigReg.register("brew_start", BrewStartTrigger.class);
        trigReg.register("repair", RepairTrigger.class);
        trigReg.register("player_interact", PlayerInteractTrigger.class);
        trigReg.register("right_click_air", RightClickAirTrigger.class);
        trigReg.register("right_click_block", RightClickBlockTrigger.class);
        trigReg.register("right_click_entity", RightClickEntityTrigger.class);
        trigReg.register("left_click_air", LeftClickAirTrigger.class);
        trigReg.register("left_click_block", LeftClickBlockTrigger.class);
        trigReg.register("left_click_entity", LeftClickEntityTrigger.class);
        trigReg.register("consume_item", ConsumeItemTrigger.class);
        trigReg.register("fishing", FishingTrigger.class);
        trigReg.register("crop_grow", CropGrowTrigger.class);
        trigReg.register("breed_animals", BreedAnimalsTrigger.class);
        trigReg.register("sprint", SprintTrigger.class);
        trigReg.register("sneak", SneakTrigger.class);
        trigReg.register("jump", JumpTrigger.class);
        trigReg.register("ride_horse", RideHorseTrigger.class);
        trigReg.register("collect_xp", CollectXpTrigger.class);
        trigReg.register("level_up", LevelUpTrigger.class);
        trigReg.register("enchant_item", EnchantItemTrigger.class);
        trigReg.register("shoot_bow", ShootBowTrigger.class);
        trigReg.register("item_damage", ItemDamageTrigger.class);
        trigReg.register("player_shear", ShearEntityTrigger.class);
        trigReg.register("player_tame", TameEntityTrigger.class);
        trigReg.register("launch_projectile", LaunchProjectileTrigger.class);
        trigReg.register("projectile_hit", ProjectileHitTrigger.class);
        trigReg.register("resurrect", ResurrectTrigger.class);
        trigReg.register("cure_villager", CureVillagerTrigger.class);
        trigReg.register("elytra_glide", ElytraGlideTrigger.class);
        trigReg.register("chunk_load", ChunkLoadTrigger.class);
        trigReg.register("sleep", SleepTrigger.class);
        trigReg.register("compost", CompostTrigger.class);
        trigReg.register("fertilize", FertilizeTrigger.class);
        trigReg.register("loot", LootTrigger.class);
        trigReg.register("trade", TradeTrigger.class);
        trigReg.register("barter", BarterTrigger.class);
        trigReg.register("recipe_discover", RecipeDiscoverTrigger.class);
        trigReg.register("smith", SmithTrigger.class);
        trigReg.register("mend", MendTrigger.class);
        trigReg.register("map_fill", MapFillTrigger.class);
        trigReg.register("cartography", CartographyTrigger.class);
        trigReg.register("vault_change", VaultChangeTrigger.class);
        trigReg.register("sniffer", SnifferTrigger.class);
        trigReg.register("potion_splash", PotionSplashTrigger.class);
        trigReg.register("sign_book", SignBookTrigger.class);
        trigReg.register("jukebox_play", JukeboxPlayTrigger.class);
        trigReg.register("lectern_place", LecternPlaceTrigger.class);
        trigReg.register("right_click", RightClickTrigger.class);
        trigReg.register("physical_interaction", PhysicalInteractionTrigger.class);
        trigReg.register("sensed", SensedTrigger.class);
        trigReg.register("trip_trap", TripTrapTrigger.class);
    }

    /** Registers the built-in state filters into the given registry. */
    public static void registerBuiltinStateFilters(StateFilterRegistry sf, TagResolver tagResolver,
            EntityTagResolver entityTagResolver) {
        sf.register("is_sneaking", (p, e, v) -> p.isSneaking());
        sf.register("is_sprinting", (p, e, v) -> p.isSprinting());
        sf.register("is_in_water", (p, e, v) -> p.isInWater());
        sf.register("is_on_ground", (p, e, v) -> p.isOnGround());
        sf.register("is_on_fire", (p, e, v) -> p.getFireTicks() > 0);
        sf.register("is_riding", (p, e, v) -> p.isInsideVehicle());
        sf.register("is_blocking", (p, e, v) -> p.isBlocking());

        sf.register("cause", (p, e, v) ->
                io.github.chasehuegel.skilling.engine.requirements.DamageCauseFilter.evaluate(e, v));

        sf.register("player_placed", (p, e, v) -> {
            // Value-aware: player_placed:true matches player-placed blocks,
            // player_placed:false (the bundled default) matches natural blocks.
            boolean expectPlaced = !"false".equalsIgnoreCase(v == null ? "" : v);
            if (e instanceof org.bukkit.event.block.BlockBreakEvent be) {
                return be.getBlock().hasMetadata("player_placed") == expectPlaced;
            }
            return true;
        });

        sf.register("honey_level", (p, e, v) -> {
            // Gates honey-harvest XP on the clicked beehive actually holding
            // honey: below:N, above:N, exactly:N read the hive's honey level.
            // Fails closed for non-interaction events, non-beehive clicks, and
            // malformed or unsupported values.
            String[] parts = v.split(":", 2);
            if (parts.length < 2) return false;
            int threshold;
            try {
                threshold = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                return false;
            }
            if (!(e instanceof org.bukkit.event.player.PlayerInteractEvent ie)) return false;
            var block = ie.getClickedBlock();
            if (block == null) return false;
            if (!(block.getBlockData() instanceof org.bukkit.block.data.type.Beehive beehive)) return false;
            int honeyLevel = beehive.getHoneyLevel();
            return switch (parts[0]) {
                case "below" -> honeyLevel < threshold;
                case "above" -> honeyLevel > threshold;
                case "exactly" -> honeyLevel == threshold;
                default -> false;
            };
        });

        sf.register("dimension", (p, e, v) -> {
            var env = p.getWorld().getEnvironment();
            return switch (v) {
                case "overworld" -> env == org.bukkit.World.Environment.NORMAL;
                case "nether" -> env == org.bukkit.World.Environment.NETHER;
                case "end" -> env == org.bukkit.World.Environment.THE_END;
                default -> false;
            };
        });

        sf.register("weather", (p, e, v) -> switch (v) {
            case "clear" -> p.getWorld().isClearWeather();
            case "rain" -> p.getWorld().hasStorm();
            case "thunder" -> p.getWorld().isThundering();
            default -> false;
        });

        sf.register("time", (p, e, v) -> switch (v) {
            // Day spans ticks [0, 13000) (dawn through dusk); night is [13000, 24000).
            // The halves are non-overlapping and leave no dusk gap (previously
            // 12300-12999 matched neither).
            case "day" -> p.getWorld().getTime() < 13000;
            case "night" -> p.getWorld().getTime() >= 13000;
            default -> false;
        });

        sf.register("light_level", (p, e, v) -> {
            String[] parts = v.split(":", 2);
            if (parts.length < 2) return true;
            int threshold;
            try { threshold = Integer.parseInt(parts[1]); } catch (NumberFormatException ex) { return true; }
            int light = p.getLocation().getBlock().getLightLevel();
            return switch (parts[0]) {
                case "below" -> light < threshold;
                case "above" -> light > threshold;
                case "exactly" -> light == threshold;
                default -> true;
            };
        });

        sf.register("health", (p, e, v) -> {
            String[] parts = v.split(":", 2);
            if (parts.length < 2) return true;
            double healthPct = p.getHealth() / p.getMaxHealth() * 100;
            String val = parts[1].endsWith("%") ? parts[1].substring(0, parts[1].length() - 1) : parts[1];
            double threshold;
            try { threshold = Double.parseDouble(val); } catch (NumberFormatException ex) { return true; }
            return switch (parts[0]) {
                case "below" -> healthPct < threshold;
                case "above" -> healthPct > threshold;
                default -> true;
            };
        });

        sf.register("hunger", (p, e, v) -> {
            String[] parts = v.split(":", 2);
            if (parts.length < 2) return true;
            int threshold;
            try { threshold = Integer.parseInt(parts[1]); } catch (NumberFormatException ex) { return true; }
            int food = p.getFoodLevel();
            return switch (parts[0]) {
                case "below" -> food < threshold;
                case "above" -> food > threshold;
                default -> true;
            };
        });

        sf.register("biome", (p, e, v) -> {
            var biome = p.getLocation().getBlock().getBiome();
            var biomeKey = safeNamespacedKey(v);
            if (biomeKey == null) return false;
            var targetBiome = org.bukkit.Registry.BIOME.get(biomeKey);
            return targetBiome != null && biome == targetBiome;
        });

        sf.register("target_type", (p, e, v) -> {
            EntityType entityType = resolveFilteredEntityType(e);
            if (entityType == null) return false;
            if (v == null || v.isBlank()) return false;
            if (v.startsWith("#")) {
                EnumSet<EntityType> target;
                try {
                    target = entityTagResolver.resolve(v);
                } catch (IllegalArgumentException ex) {
                    return false;
                }
                return target.contains(entityType);
            }
            var target = entityTagResolver.entity(v);
            return target != null && entityType == target;
        });

        // Whether the triggering arrow was released while the player was
        // sneaking. Reads the sneak stamp written onto the projectile by
        // onShootBow, so it is true at impact time only when the shot was a
        // sneak-shot. Fails closed for non-projectile events (a melee swing is
        // never a shot).
        sf.register("was_sneaking", (p, e, v) -> {
            org.bukkit.entity.Projectile proj = Skilling.resolveShotStateProjectile(e);
            if (proj != null && proj.getPersistentDataContainer()
                    .has(SHOT_SNEAK_KEY, org.bukkit.persistence.PersistentDataType.BOOLEAN)) {
                return proj.getPersistentDataContainer()
                        .get(SHOT_SNEAK_KEY, org.bukkit.persistence.PersistentDataType.BOOLEAN);
            }
            return false;
        });

        // Whether the event's target entity carries a given potion effect. Used
        // to gate an arrow's bonus against a target already marked (e.g.
        // corrected by an apply_status ability). Value is a full namespaced key
        // (e.g. target_status:minecraft:glowing). Fails closed for events without
        // a living target or an unknown effect.
        sf.register("target_status", (p, e, v) -> {
            org.bukkit.entity.LivingEntity target = resolveFilteredTargetEntity(e);
            if (target == null || v == null || v.isBlank()) return false;
            org.bukkit.potion.PotionEffectType type = resolveEffectType(v);
            return type != null && target.hasPotionEffect(type);
        });

        // True when the event's damaged/clicked target is a hostile mob that is
        // not currently targeting the attacking player. Gates a "sneak attack"
        // against unaware mobs: a calm mob (target null) or one hunting someone
        // else is a valid backstab target; one already locked onto the player is
        // not. Fails closed for non-mob victims and event-less requirements.
        sf.register("target_unaware", (p, e, v) -> {
            org.bukkit.entity.LivingEntity target = resolveFilteredTargetEntity(e);
            if (target == null || !(target instanceof org.bukkit.entity.Mob mob)) return false;
            return !p.equals(mob.getTarget());
        });

        sf.register("offhand", (p, e, v) -> {
            var offhand = p.getInventory().getItemInOffHand().getType();
            return switch (v) {
                case "empty" -> offhand == org.bukkit.Material.AIR;
                case "weapon" -> offhand.name().contains("SWORD") || offhand.name().contains("AXE")
                        || offhand == org.bukkit.Material.TRIDENT || offhand == org.bukkit.Material.MACE;
                default -> false;
            };
        });

        sf.register("hand", (p, e, v) -> {
            boolean mainEmpty = p.getInventory().getItemInMainHand().getType() == org.bukkit.Material.AIR;
            boolean offEmpty = p.getInventory().getItemInOffHand().getType() == org.bukkit.Material.AIR;
            return switch (v) {
                case "empty" -> mainEmpty && offEmpty;
                case "main_empty" -> mainEmpty;
                case "off_empty" -> offEmpty;
                default -> false;
            };
        });

        // Target-driven armor gating: the value is a material or #... tag resolved
        // through the cached TagResolver (O(1) after warmup), so no armor-tier
        // knowledge is hard-coded here. `equipped_all` requires every armor slot to
        // match; `equipped_any` requires at least one. Empty slots are treated as AIR.
        sf.register("equipped_all", (p, e, v) -> matchesEquipped(p, v, true, tagResolver));
        sf.register("equipped_any", (p, e, v) -> matchesEquipped(p, v, false, tagResolver));

        // Matches the specific goat-horn variant held in the main hand, read from
        // the item's `minecraft:instrument` data component (e.g. sing_goat_horn).
        // Fails closed for non-horns and horns without an instrument component.
        sf.register("instrument", (p, e, v) -> {
            if (v == null || v.isBlank()) return false;
            var hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() != org.bukkit.Material.GOAT_HORN) return false;
            if (!hand.hasData(io.papermc.paper.datacomponent.DataComponentTypes.INSTRUMENT)) return false;
            var instrument = hand.getData(io.papermc.paper.datacomponent.DataComponentTypes.INSTRUMENT);
            return instrument != null && v.equals(instrument.getKey().asString());
        });
    }

    /**
     * Parses a state value into a {@link NamespacedKey}, returning null for a
     * malformed value instead of throwing on the event path.
     *
     * @param value the raw namespaced key string (e.g. {@code minecraft:plains})
     * @return the parsed key, or null if malformed
     */
    private static org.bukkit.NamespacedKey safeNamespacedKey(String value) {
        try {
            return org.bukkit.NamespacedKey.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Resolves the entity whose type a {@code target_type} filter compares
     * against, for the events that carry a target entity: entity damage (the
     * damaged entity), entity death (the killed entity), and a right-clicked
     * entity (the clicked mob, so {@code target_type} gates {@code
     * right_click_entity} mechanics such as {@code core:instant_tame} and
     * {@code core:pick_up_mob}).
     *
     * @param e the triggering event
     * @return the target entity type, or null for events without a target entity
     */
    private static EntityType resolveFilteredEntityType(org.bukkit.event.Event e) {
        if (e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent de) {
            return de.getEntity().getType();
        }
        if (e instanceof org.bukkit.event.entity.EntityDeathEvent ede) {
            return ede.getEntity().getType();
        }
        if (e instanceof org.bukkit.event.player.PlayerInteractEntityEvent pe) {
            return pe.getRightClicked().getType();
        }
        return null;
    }

    /**
     * Resolves the projectile a triggering event originates from, so the
     * {@code was_sneaking} filter can read the sneak stamp written at shot time.
     *
     * @param e the triggering event
     * @return the projectile, or null for events with no projectile source
     */
    private static org.bukkit.entity.Projectile resolveShotStateProjectile(org.bukkit.event.Event e) {
        if (e instanceof org.bukkit.event.entity.ProjectileHitEvent phe
                && phe.getEntity() instanceof org.bukkit.entity.Projectile proj) {
            return proj;
        }
        if (e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent de
                && de.getDamager() instanceof org.bukkit.entity.Projectile proj) {
            return proj;
        }
        return null;
    }

    /**
     * Resolves the living target entity a triggering event points at, so a
     * {@code target_status} filter can inspect its active potion effects.
     *
     * @param e the triggering event
     * @return the target living entity, or null for events without one
     */
    private static org.bukkit.entity.LivingEntity resolveFilteredTargetEntity(org.bukkit.event.Event e) {
        if (e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent de
                && de.getEntity() instanceof org.bukkit.entity.LivingEntity living) {
            return living;
        }
        if (e instanceof org.bukkit.event.entity.EntityDeathEvent ede
                && ede.getEntity() instanceof org.bukkit.entity.LivingEntity living) {
            return living;
        }
        if (e instanceof org.bukkit.event.player.PlayerInteractEntityEvent pe
                && pe.getRightClicked() instanceof org.bukkit.entity.LivingEntity living) {
            return living;
        }
        return null;
    }

    /**
     * Resolves a namespaced potion-effect key (e.g. {@code minecraft:glowing})
     * into a {@link PotionEffectType}, returning null for a malformed or unknown
     * key instead of throwing on the filter path.
     *
     * @param value the namespaced effect key
     * @return the resolved effect type, or null if unresolvable
     */
    private static org.bukkit.potion.PotionEffectType resolveEffectType(String value) {
        try {
            org.bukkit.NamespacedKey key = safeNamespacedKey(value);
            if (key == null) return null;
            return org.bukkit.Registry.POTION_EFFECT_TYPE.get(key);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Evaluates an {@code equipped_all}/{@code equipped_any} state filter: every
     * armor slot (or at least one) must hold an item whose material is in the
     * target's resolved set. Empty slots count as {@link org.bukkit.Material#AIR}.
     *
     * @param player  the player whose armor to inspect
     * @param target  a material or {@code #...} tag reference
     * @param requireAll true for {@code equipped_all}, false for {@code equipped_any}
     * @param tagResolver the tag resolver used to resolve the target
     * @return true if the armor contents match the target requirement
     */
    private static boolean matchesEquipped(org.bukkit.entity.Player player, String target,
            boolean requireAll, TagResolver tagResolver) {
        if (target == null || target.isBlank()) return false;
        java.util.Set<org.bukkit.Material> matches;
        try {
            matches = tagResolver.resolve(target);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (matches.isEmpty()) return false;
        var armor = player.getInventory().getArmorContents();
        boolean any = false;
        for (var piece : armor) {
            var type = piece == null ? org.bukkit.Material.AIR : piece.getType();
            if (matches.contains(type)) {
                any = true;
                if (!requireAll) return true;
            } else if (requireAll) {
                return false;
            }
        }
        return requireAll ? any : false;
    }

    private void loadSkills() {
        File skillsDir = new File(getDataFolder(), "skills");
        if (!skillsDir.exists()) {
            skillsDir.mkdirs();
        }
        skillManager.loadSkills(skillsDir);
        int count = skillManager.getSkills().size();
        if (count > 0) {
            getLogger().info("Loaded " + count + " skill definition(s).");
        }
    }

    @Override
    public void onDisable() {
        // Hide every pooled boss bar first so a /reload never leaves frozen
        // bars floating over players after the tick loop is gone.
        if (bossBarPool != null) {
            bossBarPool.removeAll();
        }
        // Bukkit does not remove recipes automatically; unregister the guide book
        // recipe so a disabled/reloaded book leaves the game entirely.
        if (skillsGuideBook != null) {
            skillsGuideBook.shutdown();
        }
        if (integrationManager != null) {
            integrationManager.shutdown();
        }
        if (webServer != null) {
            webServer.stop();
        }
        io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic.clearAll();
        io.github.chasehuegel.skilling.engine.mechanic.impl.AttributeModifierHelper.clearAll();
        if (asyncBatchWorker != null) {
            asyncBatchWorker.stop();
            // Flush remaining dirty profiles on a worker thread and await with a
            // bounded timeout so shutdown never blocks the main thread indefinitely
            // while still persisting pending data before the pool closes.
            var flush = asyncBatchWorker.flushDirtyProfilesAsync();
            try {
                flush.get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Flush still running on shutdown; waiting for it to finish", e);
                // Never close the pool under an in-flight flush: a slow flush that
                // outlives the timeout would otherwise fail with "pool not
                // initialized" and lose the pending data.
                flush.join();
            }
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("Skilling disabled.");
    }

    public Registries getRegistries() {
        return registries;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public SkillMenuBuilder getSkillMenuBuilder() {
        return skillMenuBuilder;
    }

    public RequirementEngine getRequirementEngine() {
        return requirementEngine;
    }

    public StateFilterRegistry getStateFilterRegistry() {
        return stateFilterRegistry;
    }

    public FeedbackDebouncer getFeedbackDebouncer() {
        return feedbackDebouncer;
    }

    public BossBarPool getBossBarPool() {
        return bossBarPool;
    }

    public LockdownManager getLockdownManager() {
        return lockdownManager;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public SkillEventListener getSkillEventListener() {
        return skillEventListener;
    }

    public CustomTagLoader getCustomTagLoader() {
        return customTagLoader;
    }

    public void setCustomTagLoader(CustomTagLoader customTagLoader) {
        this.customTagLoader = customTagLoader;
    }

    /**
     * Returns the reusable ability registry used by {@link SkillManager} for
     * ability base-merge.
     *
     * @return the current ability registry
     */
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    /**
     * Replaces the reusable ability registry (used on reload when abilities
     * change).
     *
     * @param abilityManager the new ability registry
     */
    public void setAbilityManager(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    /**
     * Returns the active tag resolver used on the event path.
     *
     * @return the current tag resolver
     */
    public TagResolver getTagResolver() {
        return tagResolver;
    }

    /**
     * Replaces the active tag resolver (used on reload when tags change).
     *
     * @param tagResolver the new tag resolver
     */
    public void setTagResolver(TagResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    /**
     * Returns the entity-type tag resolver used by the {@code target_type}
     * state filter for {@code #...} entity tag references.
     *
     * @return the entity tag resolver
     */
    public EntityTagResolver getEntityTagResolver() {
        return entityTagResolver;
    }

    /**
     * Replaces the active entity-type tag resolver (used on reload when tags
     * change).
     *
     * @param entityTagResolver the new entity tag resolver
     */
    public void setEntityTagResolver(EntityTagResolver entityTagResolver) {
        this.entityTagResolver = entityTagResolver;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    /**
     * Logs a {@code [DEBUG]} line when {@code debug_logging} is enabled. The
     * guard runs before any string building, so this is a no-op off the hot
     * path when debugging is disabled.
     *
     * @param message the debug message
     */
    public void debug(String message) {
        if (debugLogging) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public int getTitleStayDuration() {
        return titleStayDuration;
    }

    public double getGlobalXpModifier() {
        return globalXpModifier;
    }

    public int getCropGrowRadius() {
        return cropGrowRadius;
    }

    /**
     * Returns the active branding configuration (color templates, bar settings,
     * message templates) parsed from {@code config.yml}.
     *
     * <p>Never null: a missing or unset {@code branding} section falls back to
     * {@link BrandingConfig#DEFAULT}. Re-parsed on {@code /skills reload}.
     *
     * @return the current branding
     */
    public BrandingConfig getBranding() {
        BrandingConfig current = branding;
        return current != null ? current : BrandingConfig.DEFAULT;
    }

    public void reloadConfigSettings() {
        reloadConfig();
        var config = (YamlConfiguration) getConfig();
        this.debugLogging = config.getBoolean(CONFIG_DEBUG_LOGGING, false);
        this.titleStayDuration = config.getInt(CONFIG_TITLES_STAY_DURATION, 5000);
        this.globalXpModifier = config.getDouble(CONFIG_GLOBAL_XP_MODIFIER, 1.0);
        this.cropGrowRadius = config.getInt(CONFIG_CROP_GROW_RADIUS, 10);
        this.branding = BrandingConfig.from(config.getConfigurationSection("branding"));
        // Refresh subsystems whose settings are otherwise fixed at construction so
        // /skills set and web config edits actually take effect at runtime.
        if (bossBarPool != null) {
            bossBarPool.setMaxActive(config.getInt(CONFIG_BOSSBAR_MAX_ACTIVE, 2));
            bossBarPool.setFadeTicks(config.getInt(CONFIG_BOSSBAR_FADE_TICKS, 40));
            bossBarPool.setDefaultColor(branding.bossBar().defaultColor());
            bossBarPool.setDefaultStyle(branding.bossBar().defaultStyle());
        }
        if (feedbackDebouncer != null) {
            feedbackDebouncer.setIntervalMs(config.getLong(CONFIG_DEBOUNCER_INTERVAL_MS, 500));
        }
        if (skillsGuideBook != null) {
            skillsGuideBook.setEnabled(config.getBoolean("skills_guide_book.enabled", true));
        }
    }

    private static final String WEB_PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final String WEB_DEFAULT_PASSWORD = "skilling";

    /**
     * Replaces the shipped default web password with a freshly generated one the
     * first time the web GUI is enabled, so publicly-known credentials are never
     * used. Logs the generated credential once; the admin can rotate it by
     * editing {@code config.yml} and restarting.
     *
     * @param config the plugin configuration
     */
    private void ensureWebPassword(YamlConfiguration config) {
        if (!config.getBoolean("web.enabled", false)) return;
        String password = config.getString("web.password", "");
        if (password == null || password.isBlank() || WEB_DEFAULT_PASSWORD.equals(password)) {
            String generated = generateWebPassword();
            config.set("web.password", generated);
            saveConfig();
            getLogger().warning("Web GUI enabled with a generated admin password. "
                + "Username: '" + config.getString("web.username", "admin")
                + "', Password: '" + generated
                + "'. Store it securely; the web GUI uses Basic auth over plaintext HTTP.");
        }
    }

    private static String generateWebPassword() {
        var random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(WEB_PASSWORD_CHARS.charAt(random.nextInt(WEB_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Refreshes the datapack list and enables every bundled pack under
     * {@code plugins/Skilling/datapacks/} that is not already enabled.
     *
     * <p>Fires once, shortly after the first run's setup copy, so a bundled pack
     * that arrived after the server's initial discovery pass still becomes
     * active without a restart. Enabling a newly found pack reloads data; on
     * later boots the bootstrap's {@code DATAPACK_DISCOVERY} handler enables
     * them at server start, so this is a one-time first-run cost.
     */
    private void enableBundledDatapacks() {
        DatapackManager manager = Bukkit.getDatapackManager();
        manager.refreshPacks();
        File dir = new File(getDataFolder(), "datapacks");
        File[] zips = dir.listFiles((d, name) -> name.endsWith(".zip"));
        if (zips == null) return;
        for (File zip : zips) {
            String id = zip.getName().replaceFirst("\\.zip$", "");
            Datapack pack = manager.getPack(getName() + "/" + id);
            if (pack != null && !pack.isEnabled()) {
                getLogger().info("Enabling bundled datapack " + id + "...");
                pack.setEnabled(true);
            }
        }
    }
}
