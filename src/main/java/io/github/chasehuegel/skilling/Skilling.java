package io.github.chasehuegel.skilling;

import io.github.chasehuegel.skilling.api.Registries;
import io.github.chasehuegel.skilling.api.SkillingAPI;
import io.github.chasehuegel.skilling.engine.SkillManager;
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
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.github.chasehuegel.skilling.engine.ui.GuiLayoutConfig;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import io.github.chasehuegel.skilling.engine.ui.SkillsGuideBook;
import io.github.chasehuegel.skilling.engine.ui.UIProtectionListener;
import io.github.chasehuegel.skilling.web.WebServer;
import io.github.chasehuegel.skilling.web.config.WebConfig;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.SQLException;
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

    private static final String CONFIG_DEBUG_LOGGING = "debug_logging";
    private static final String CONFIG_TITLES_STAY_DURATION = "titles.stay_duration";
    private static final String CONFIG_GLOBAL_XP_MODIFIER = "global_xp_modifier";
    private static final String CONFIG_DEBOUNCER_INTERVAL_MS = "debouncer.interval_ms";
    private static final String CONFIG_BOSSBAR_MAX_ACTIVE = "bossbar.max_active";
    private static final String CONFIG_BOSSBAR_FADE_TICKS = "bossbar.fade_ticks";
    private static final String CONFIG_CROP_GROW_RADIUS = "crop_grow.search_radius";

    private Registries registries;
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
    private SkillEventListener skillEventListener;
    private WebServer webServer;
    private IntegrationManager integrationManager;
    private volatile boolean reloading;
    private volatile boolean debugLogging;
    private volatile int titleStayDuration;
    private volatile double globalXpModifier;
    private volatile int cropGrowRadius;

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
            if (!new File(getDataFolder(), "tags.yml").exists()) {
                getLogger().info("Generating default tags.yml...");
                saveResource("tags.yml", false);
            }
            if (!new File(getDataFolder(), "template-skill.yml").exists()) {
                getLogger().info("Generating default template-skill.yml...");
                saveResource("template-skill.yml", false);
            }
            if (!new File(getDataFolder(), "gui.yml").exists()) {
                getLogger().info("Generating default gui.yml...");
                saveResource("gui.yml", false);
            }

            String[] bundledSkills = {"mining.yml", "woodcutting.yml", "excavation.yml",
                                      "farming.yml", "fishing.yml", "archery.yml"};
            for (String skill : bundledSkills) {
                if (!new File(getDataFolder(), "skills/" + skill).exists()) {
                    getLogger().info("Generating default " + skill + "...");
                    saveResource("skills/" + skill, false);
                }
            }

            // Mark setup as complete so bundled files are not regenerated on subsequent starts
            config.set("setup.first_run", false);
            saveConfig();
        }

        this.debugLogging = config.getBoolean(CONFIG_DEBUG_LOGGING, false);
        if (debugLogging) {
            getLogger().info("Debug logging enabled.");
        }
        this.titleStayDuration = config.getInt(CONFIG_TITLES_STAY_DURATION, 5000);
        this.globalXpModifier = config.getDouble(CONFIG_GLOBAL_XP_MODIFIER, 1.0);
        this.cropGrowRadius = config.getInt(CONFIG_CROP_GROW_RADIUS, 10);

        this.registries = new Registries(
                new MechanicRegistry(),
                new TriggerRegistry(),
                new EvaluatorRegistry()
        );
        registerBuiltins();

        // Initialize database
        this.databaseManager = new DatabaseManager(getDataFolder());
        try {
            databaseManager.initialize(config);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database", e);
        }

        this.profileManager = new ProfileManager(databaseManager);
        this.asyncBatchWorker = new AsyncBatchWorker(this, databaseManager, profileManager);
        this.asyncBatchWorker.start();

        // Load skill definitions from YAML
        this.customTagLoader = new CustomTagLoader();
        customTagLoader.load(new File(getDataFolder(), "tags.yml"));
        var tagResolver = new TagResolver(customTagLoader);
        this.skillManager = new SkillManager(
                registries.getEvaluatorRegistry(),
                registries.getMechanicRegistry(),
                registries.getTriggerRegistry(),
                tagResolver
        );
        loadSkills();

        // UI
        this.guiLayoutConfig = GuiLayoutConfig.load();
        this.skillMenuBuilder = new SkillMenuBuilder(skillManager, guiLayoutConfig);

        // Skills Guide Book
        var skillsGuideBook = new SkillsGuideBook(this, profileManager, skillMenuBuilder);
        skillsGuideBook.register();

        // Feedback systems
        int debounceMs = config.getInt(CONFIG_DEBOUNCER_INTERVAL_MS, 500);
        this.feedbackDebouncer = new FeedbackDebouncer(debounceMs);
        int maxBars = config.getInt(CONFIG_BOSSBAR_MAX_ACTIVE, 2);
        int fadeTicks = config.getInt(CONFIG_BOSSBAR_FADE_TICKS, 40);
        this.bossBarPool = new BossBarPool(maxBars, fadeTicks);

        // Requirements engine
        this.requirementEngine = new RequirementEngine(tagResolver);

        // Lockdown / reload manager
        this.lockdownManager = new LockdownManager(this, profileManager, asyncBatchWorker, skillManager);

        // Web GUI
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
        Bukkit.getPluginManager().registerEvents(new PlayerListener(profileManager, asyncBatchWorker, requirementEngine), this);
        this.skillEventListener = new SkillEventListener(this, skillManager, profileManager, tagResolver, requirementEngine,
                        registries.getMechanicRegistry(), feedbackDebouncer, bossBarPool);
        Bukkit.getPluginManager().registerEvents(skillEventListener, this);

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
        getLogger().info("Loaded " + skillCount + " skill(s) | " + mechCount + " mechanic(s) | "
                + trigCount + " trigger(s) | " + evalCount + " evaluator(s) | "
                + tagCount + " custom tag(s)");
        getLogger().info("Skilling v" + getPluginMeta().getVersion() + " enabled.");
    }

    public void registerBuiltins() {
        var evalReg = registries.getEvaluatorRegistry();
        evalReg.register("linear", new LinearEvaluator(0, 1, 0, Double.MAX_VALUE));
        evalReg.register("constant", new ConstantEvaluator(0));
        evalReg.register("milestone", new MilestoneEvaluator(new TreeMap<>()));
        evalReg.register("polynomial", new PolynomialEvaluator(50, 2.5));

        var mechReg = registries.getMechanicRegistry();
        mechReg.register("core:yield_multiplier", YieldMultiplierMechanic.class, List.of("yield_chance"));
        mechReg.register("core:chain_break", ChainBreakMechanic.class, List.of("chain_limit", "exhaustion"));
        mechReg.register("core:modify_damage", ModifyDamageMechanic.class, List.of("multiplier"));
        mechReg.register("core:apply_status", ApplyStatusMechanic.class, List.of("effect", "duration", "amplifier"));
        mechReg.register("core:cancel_damage", CancelDamageMechanic.class, List.of("chance"));
        mechReg.register("core:modify_attribute", ModifyAttributeMechanic.class, List.of("attribute", "amount", "duration"));
        mechReg.register("core:modify_craft_output", ModifyCraftOutputMechanic.class, List.of("multiplier"));
        mechReg.register("core:modify_furnace_output", ModifyFurnaceOutputMechanic.class, List.of("multiplier"));
        mechReg.register("core:saturation_inject", SaturationInjectMechanic.class, List.of("saturation"));
        mechReg.register("core:modify_brew_time", ModifyBrewTimeMechanic.class, List.of("multiplier"));
        mechReg.register("core:modify_potion_duration", ModifyPotionDurationMechanic.class, List.of("multiplier"));
        mechReg.register("core:aoe_effect", AoeEffectMechanic.class, List.of("effect", "radius", "duration", "amplifier"));
        mechReg.register("core:projectile", ProjectileMechanic.class, List.of("speed", "damage"));
        mechReg.register("core:teleport", TeleportMechanic.class, List.of("range"));
        mechReg.register("core:block_damage", BlockDamageMechanic.class, List.of("chance"));
        mechReg.register("core:thorns_damage", ThornsDamageMechanic.class, List.of("damage"));
        mechReg.register("core:dodge", DodgeMechanic.class, List.of("chance"));
        mechReg.register("core:lifesteal", LifestealMechanic.class, List.of("percentage"));
        mechReg.register("core:armor_bonus", ArmorBonusMechanic.class, List.of("amount"));
        mechReg.register("core:knockback_resist", KnockbackResistMechanic.class, List.of("amount"));
        mechReg.register("core:crowd_control", CrowdControlMechanic.class, List.of("effect", "duration", "amplifier", "radius"));
        mechReg.register("core:execute", ExecuteMechanic.class, List.of("threshold"));
        mechReg.register("core:auto_smelt", AutoSmeltMechanic.class, List.of("chance"));
        mechReg.register("core:speed_bonus", SpeedBonusMechanic.class, List.of("multiplier"));
        mechReg.register("core:xp_bonus", XpBonusMechanic.class, List.of("multiplier"));

        var trigReg = registries.getTriggerRegistry();
        trigReg.register("block_break", BlockBreakTrigger.class);
        trigReg.register("block_place", BlockPlaceTrigger.class);
        trigReg.register("entity_damage", EntityDamageTrigger.class);
        trigReg.register("entity_damage_taken", EntityDamageTakenTrigger.class);
        trigReg.register("entity_kill", EntityKillTrigger.class);
        trigReg.register("craft_item", CraftItemTrigger.class);
        trigReg.register("furnace_extract", FurnaceExtractTrigger.class);
        trigReg.register("brew_potion", BrewPotionTrigger.class);
        trigReg.register("player_interact", PlayerInteractTrigger.class);
        trigReg.register("consume_item", ConsumeItemTrigger.class);
        trigReg.register("fishing", FishingTrigger.class);
        trigReg.register("crop_grow", CropGrowTrigger.class);
        trigReg.register("breed_animals", BreedAnimalsTrigger.class);
        trigReg.register("sprint", SprintTrigger.class);
        trigReg.register("sneak", SneakTrigger.class);
        trigReg.register("ride_horse", RideHorseTrigger.class);
        trigReg.register("collect_xp", CollectXpTrigger.class);
        trigReg.register("level_up", LevelUpTrigger.class);
        trigReg.register("enchant_item", EnchantItemTrigger.class);
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
        if (integrationManager != null) {
            integrationManager.shutdown();
        }
        if (webServer != null) {
            webServer.stop();
        }
        if (asyncBatchWorker != null) {
            asyncBatchWorker.stop();
            asyncBatchWorker.flushDirtyProfiles();
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

    public int getTitleStayDuration() {
        return titleStayDuration;
    }

    public double getGlobalXpModifier() {
        return globalXpModifier;
    }

    public int getCropGrowRadius() {
        return cropGrowRadius;
    }

    public void reloadConfigSettings() {
        reloadConfig();
        var config = (YamlConfiguration) getConfig();
        this.debugLogging = config.getBoolean(CONFIG_DEBUG_LOGGING, false);
        this.titleStayDuration = config.getInt(CONFIG_TITLES_STAY_DURATION, 5000);
        this.globalXpModifier = config.getDouble(CONFIG_GLOBAL_XP_MODIFIER, 1.0);
        this.cropGrowRadius = config.getInt(CONFIG_CROP_GROW_RADIUS, 10);
    }
}
