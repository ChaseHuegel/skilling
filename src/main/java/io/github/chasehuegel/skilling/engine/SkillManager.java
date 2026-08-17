package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


/**
 * Parses skill YAML definitions into executable skill records and manages
 * the lifecycle of skill data.
 *
 * <p>Responsible for reading {@code template-skill.yml} schema files, resolving
 * tags via {@link TagResolver}, instantiating mechanics and evaluators from
 * the registries, and building {@link SkillDefinition} records.
 */
public final class SkillManager {

    /** Upper bound on {@code max_level}: keeps the per-skill threshold-table allocation sane. */
    private static final int MAX_MAX_LEVEL = 10_000;

    private static final java.util.regex.Pattern PLACEHOLDER_PATTERN =
            java.util.regex.Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    private final EvaluatorRegistry evaluatorRegistry;
    private final MechanicRegistry mechanicRegistry;
    private final TriggerRegistry triggerRegistry;
    private final io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry stateFilterRegistry;
    private TagResolver tagResolver;
    private volatile AbilityManager abilityManager;
    private volatile Map<String, SkillDefinition> skills = Map.of();
    private volatile Map<String, List<XpSourceRef>> xpSourcesByTrigger = Map.of();
    private volatile Map<String, List<AbilityRef>> abilitiesByTrigger = Map.of();

    /**
     * Coordinates external validation ({@code SkillHandler.validateStagedSkill},
     * which parses staged YAML against the shared registries on a Jetty worker)
     * with the reload rebuild, which clears and re-populates those same registries
     * on the main thread. Validation holds the read lock; the rebuild holds the
     * write lock so a mid-reload parse never sees momentarily-empty registries.
     */
    private final java.util.concurrent.locks.ReentrantReadWriteLock registryLock =
            new java.util.concurrent.locks.ReentrantReadWriteLock();

    /**
     * The read-write lock guarding the shared registries against the reload
     * rebuild. Callers that validate skill content against the live registries
     * take the read lock; the reload rebuild takes the write lock.
     *
     * @return the registry guard lock
     */
    public java.util.concurrent.locks.ReentrantReadWriteLock registryLock() {
        return registryLock;
    }

    /**
     * A skill XP source paired with its owning skill, indexed by trigger so event
     * dispatch only visits the sources bound to the dispatched trigger.
     */
    public record XpSourceRef(SkillDefinition skill, SkillDefinition.XpSource source) {}

    /**
     * A skill ability paired with its owning skill, indexed by trigger so event
     * dispatch only visits the abilities bound to the dispatched trigger.
     */
    public record AbilityRef(SkillDefinition skill, SkillDefinition.Ability ability) {}

    /**
     * Constructs a new skill manager.
     *
     * @param evaluatorRegistry   the evaluator registry for instantiating parameter evaluators
     * @param mechanicRegistry    the mechanic registry for instantiating mechanics
     * @param triggerRegistry     the trigger registry for instantiating triggers
     * @param tagResolver         the tag resolver for filter resolution
     * @param stateFilterRegistry the registry of known player-state filter keys, used to validate
     *                            {@code state:} references at load instead of failing on the event path
     */
    public SkillManager(EvaluatorRegistry evaluatorRegistry, MechanicRegistry mechanicRegistry,
                        TriggerRegistry triggerRegistry, TagResolver tagResolver,
                        io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry stateFilterRegistry) {
        this.evaluatorRegistry = evaluatorRegistry;
        this.mechanicRegistry = mechanicRegistry;
        this.triggerRegistry = triggerRegistry;
        this.tagResolver = tagResolver;
        this.stateFilterRegistry = stateFilterRegistry;
    }

    /**
     * Loads all skill YAML files from the given directory, recursing into
     * subfolders in sorted relative-path order.
     *
     * <p>A file that cannot be parsed logs a warning and is skipped, so skill
     * packs may hold work-in-progress files. A parsed skill whose id already
     * exists keeps the first loaded skill and logs a warning. Files are built
     * into a local map and atomically swapped in as an immutable snapshot so
     * concurrent readers (web threads, addons) never see a half-loaded view.
     * Only catastrophic file-system errors throw.
     *
     * @param skillsDir the directory containing skill YAML files
     */
    public void loadSkills(File skillsDir) {
        Map<String, SkillDefinition> built = new LinkedHashMap<>();
        if (skillsDir.exists() && skillsDir.isDirectory()) {
            for (File file : collectSkillFiles(skillsDir)) {
                SkillDefinition def;
                try {
                    def = parseSkill(file);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning(
                            "Skipping malformed skill file " + file.getName() + ": " + e.getMessage());
                    continue;
                }
                if (built.containsKey(def.id())) {
                    Bukkit.getLogger().warning("Duplicate skill ID '" + def.id()
                            + "' in file: " + file.getName() + "; keeping the first definition");
                    continue;
                }
                built.put(def.id(), def);
            }
        }
        this.skills = Collections.unmodifiableMap(built);
        // Build the trigger index over the same immutable snapshot so dispatch
        // never scans every skill × source/ability per event. Swapped atomically
        // with the skill map (rebuilt on every load, including reload).
        Map<String, List<XpSourceRef>> xpIndex = new HashMap<>();
        Map<String, List<AbilityRef>> abilityIndex = new HashMap<>();
        for (SkillDefinition skill : built.values()) {
            for (SkillDefinition.XpSource source : skill.xpSources()) {
                xpIndex.computeIfAbsent(source.trigger(), k -> new ArrayList<>())
                        .add(new XpSourceRef(skill, source));
            }
            for (SkillDefinition.Ability ability : skill.abilities()) {
                abilityIndex.computeIfAbsent(ability.trigger(), k -> new ArrayList<>())
                        .add(new AbilityRef(skill, ability));
            }
        }
        this.xpSourcesByTrigger = freezeIndex(xpIndex);
        this.abilitiesByTrigger = freezeIndex(abilityIndex);
    }

    /**
     * Collects every {@code .yml} file under the skills directory (recursively)
     * in sorted relative-path order for a deterministic load order.
     *
     * @param dir the skills directory
     * @return the skill files to load
     */
    private static List<File> collectSkillFiles(File dir) {
        List<File> files = new ArrayList<>();
        try (var stream = Files.walk(dir.toPath())) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml"))
                    .sorted(Comparator.comparing(p -> dir.toPath().relativize(p).toString()))
                    .forEach(p -> files.add(p.toFile()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to walk skills directory: " + dir, e);
        }
        return files;
    }

    private static <T> Map<String, List<T>> freezeIndex(Map<String, List<T>> index) {        Map<String, List<T>> frozen = new HashMap<>();
        index.forEach((trigger, refs) -> frozen.put(trigger, List.copyOf(refs)));
        return Collections.unmodifiableMap(frozen);
    }

    /**
     * Parses a single skill YAML file into a {@link SkillDefinition}.
     *
     * @param file the YAML file
     * @return the parsed skill definition
     * @throws IllegalArgumentException if the file is malformed
     */
    public SkillDefinition parseSkill(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return parseSkill(config);
    }

    /**
     * Parses a {@link YamlConfiguration} into a {@link SkillDefinition}.
     *
     * @param config the YAML configuration
     * @return the parsed skill definition
     * @throws IllegalArgumentException if required fields are missing
     */
    public SkillDefinition parseSkill(YamlConfiguration config) {
        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Skill definition missing 'id'");
        }

        int maxLevel = config.getInt("max_level", 100);
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Skill '" + id + "' has invalid max_level: " + maxLevel);
        }
        if (maxLevel > MAX_MAX_LEVEL) {
            throw new IllegalArgumentException("Skill '" + id + "' has max_level too large: " + maxLevel
                    + " (maximum " + MAX_MAX_LEVEL + ")");
        }

        // Display section
        SkillDefinition.Display display = parseDisplay(config.getConfigurationSection("display"));

        // Progression section
        SkillDefinition.Progression progression = parseProgression(config.getConfigurationSection("progression"));

        // XP sources
        List<SkillDefinition.XpSource> xpSources = parseXpSources(config.getList("xp_sources"));

        // Abilities
        List<SkillDefinition.Ability> abilities = parseAbilities(id, config.getList("abilities"));

        // Level-up commands
        List<SkillDefinition.LevelUpCommand> levelUpCommands = parseLevelUpCommands(config.getList("level_up_commands"));

        return new SkillDefinition(id, maxLevel, display, progression, xpSources, abilities, levelUpCommands);
    }

    private SkillDefinition.Display parseDisplay(ConfigurationSection section) {
        if (section == null) {
            return new SkillDefinition.Display("Unknown", "minecraft:barrier", 0, "WHITE", "SOLID");
        }
        String name = section.getString("name", "Unknown");
        String icon = section.getString("icon", "minecraft:barrier");
        int customModelData = section.getInt("custom_model_data", 0);
        String color = section.getString("color", "WHITE");
        String style = section.getString("style", "SOLID");
        List<String> lore = asStringList(section.getList("lore", List.of()), "display lore");
        return new SkillDefinition.Display(name, icon, customModelData, color, style, lore);
    }

    private SkillDefinition.Progression parseProgression(ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Skill missing 'progression' section");
        }
        String curve = section.getString("curve", "polynomial");
        double baseXp = section.getDouble("base_xp", 50.0);
        double exponent = section.getDouble("exponent", 2.5);

        ParameterEvaluator evaluator = switch (curve) {
            case "polynomial" -> new PolynomialEvaluator(baseXp, exponent);
            // Thresholds are evaluated anchored at level 1 (see LevelThresholds),
            // so base_xp is the exact level-1 requirement and each further level
            // adds base_xp * 0.1.
            case "linear" -> new LinearEvaluator(baseXp, baseXp * 0.1, 0, Double.MAX_VALUE);
            case "constant" -> new ConstantEvaluator(baseXp);
            default -> {
                if (evaluatorRegistry.contains(curve)) {
                    yield evaluatorRegistry.create(curve);
                }
                throw new IllegalArgumentException("Unknown progression curve: " + curve);
            }
        };

        return new SkillDefinition.Progression(curve, baseXp, exponent, evaluator);
    }

    private List<SkillDefinition.XpSource> parseXpSources(List<?> list) {
        if (list == null) return List.of();
        List<SkillDefinition.XpSource> sources = new ArrayList<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("XP source must be a map, got: " + raw);
            }
            Map<String, Object> entry = castMap(map);
            String trigger = asString(entry, "trigger", "XP source");
            if (trigger == null) throw new IllegalArgumentException("XP source missing 'trigger'");
            // Fail fast on a typo'd trigger so a source that can never be
            // dispatched is rejected here instead of silently never firing.
            if (!triggerRegistry.contains(trigger)) {
                throw new IllegalArgumentException("XP source has unknown trigger: " + trigger);
            }

            List<SkillDefinition.Filter> filters = new ArrayList<>();
            Object filtersRaw = entry.get("filters");
            if (filtersRaw instanceof List<?> filterList) {
                for (Object f : filterList) {
                    if (f instanceof Map<?, ?> fm) {
                        Map<String, Object> filterMap = castMap(fm);
                        String target = asString(filterMap, "target", "XP source filter");
                        String state = asString(filterMap, "state", "XP source filter");
                        String tool = asString(filterMap, "tool", "XP source filter");
                        validateTagReference(target);
                        validateTagReference(tool);
                        validateAndWarmState(state, "XP source for trigger '" + trigger + "'");
                        filters.add(new SkillDefinition.Filter(target, state, tool));
                    }
                }
            }

            Object rewardRaw = entry.get("reward");
            if (rewardRaw == null) {
                throw new IllegalArgumentException("XP source for trigger '" + trigger + "' missing 'reward'");
            }
            if (!(rewardRaw instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("XP source for trigger '" + trigger
                        + "' reward must be an evaluator block (e.g. 'reward: { constant: 50 }'), got: " + rewardRaw);
            }
            ParameterEvaluator reward = parseInlineEvaluator(castMap(rewardRaw));

            SkillDefinition.XpScaling scaling = parseXpScaling(entry, trigger);

            sources.add(new SkillDefinition.XpSource(trigger, filters, reward, scaling));
        }
        return sources;
    }

    private SkillDefinition.XpScaling parseXpScaling(Map<String, Object> entry, String trigger) {
        Object scalingRaw = entry.get("scaling");
        if (scalingRaw == null) return SkillDefinition.XpScaling.NONE;
        String value = String.valueOf(scalingRaw).trim().toLowerCase();
        if (!value.equals("damage")) {
            throw new IllegalArgumentException("XP source for trigger '" + trigger
                    + "' has unknown scaling: " + scalingRaw + " (supported: damage)");
        }
        // Damage scaling multiplies the reward by the event's damage; a trigger
        // whose event carries no damage (e.g. block_break) must be rejected here.
        Class<? extends org.bukkit.event.Event> eventClass = triggerRegistry.create(trigger).getEventClass();
        if (!EntityDamageEvent.class.isAssignableFrom(eventClass)) {
            throw new IllegalArgumentException("XP source for trigger '" + trigger
                    + "' cannot use scaling: damage — trigger is not a damage event");
        }
        return SkillDefinition.XpScaling.DAMAGE;
    }

    private List<SkillDefinition.Ability> parseAbilities(String skillId, List<?> list) {
        if (list == null) return List.of();
        List<SkillDefinition.Ability> abilities = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Ability must be a map, got: " + raw);
            }
            Map<String, Object> abilityMap = castMap(map);

            String id = asString(abilityMap, "id", "Ability");
            if (id == null) throw new IllegalArgumentException("Ability missing 'id'");
            if (!seenIds.add(id)) {
                throw new IllegalArgumentException("Duplicate ability ID: " + id);
            }

            // When an ability with this id is registered in the AbilityManager,
            // the registered definition acts as the base: the skill's inline map
            // is overlaid on top (top-level field overwrite, no deep merge of
            // nested structures), so one field can be overridden per skill while
            // everything else is inherited. Each skill parses its own instance.
            AbilityManager manager = abilityManager;
            if (manager != null) {
                Map<String, Object> base = manager.getRaw(id);
                if (base != null) {
                    Map<String, Object> merged = new LinkedHashMap<>(base);
                    merged.putAll(abilityMap);
                    abilityMap = merged;
                }
            }

            String displayName = asString(abilityMap, "display_name", id, "Ability '" + id + "'");
            int unlockLevel = parseUnlockLevel(id, abilityMap.getOrDefault("unlock_level", 1));

            String trigger = asString(abilityMap, "trigger", "Ability '" + id + "'");
            if (trigger == null || trigger.isBlank()) {
                throw new IllegalArgumentException("Ability '" + id + "' missing required 'trigger' field");
            }
            if (!triggerRegistry.contains(trigger)) {
                throw new IllegalArgumentException("Ability '" + id + "' has unknown trigger: " + trigger);
            }

            // Display lore
            SkillDefinition.AbilityDisplay abilityDisplay = parseAbilityDisplay(castMap(abilityMap.get("display")));

            // Requirements
            SkillDefinition.Requirements requirements = parseRequirements(castMap(abilityMap.get("requirements")), id);
            SkillDefinition.OnFailure onFailure = parseOnFailure(castMap(abilityMap.get("on_failure")), id);

            // Mechanics
            List<SkillDefinition.MechanicEntry> mechanics = parseMechanics(skillId, id, abilityMap.get("mechanics"));
            validateAbilityLorePlaceholders(id, abilityDisplay, mechanics);

            // Feedback
            SkillDefinition.Feedback feedback = parseFeedback(castMap(abilityMap.get("feedback")), id);

            abilities.add(new SkillDefinition.Ability(id, displayName, unlockLevel, trigger, abilityDisplay,
                    requirements, onFailure, mechanics, feedback));
        }
        return abilities;
    }

    private int parseUnlockLevel(String abilityId, Object raw) {
        if (raw instanceof Number n) return n.intValue();
        throw new IllegalArgumentException("Ability '" + abilityId
                + "' unlock_level must be a number, got: " + raw);
    }

    private SkillDefinition.AbilityDisplay parseAbilityDisplay(Map<String, Object> map) {
        if (map == null) return new SkillDefinition.AbilityDisplay(List.of());
        List<String> lore = asStringList(map, "lore", "Ability display");
        return new SkillDefinition.AbilityDisplay(lore);
    }

    /**
     * Fail-fast validation that every {@code {placeholder}} in an ability's lore
     * resolves against that ability's mechanic parameter keys. The lore resolver
     * would otherwise keep unknown tokens verbatim (and log a warning at GUI-open
     * time), so a typo or stale placeholder is rejected at load instead.
     *
     * @param abilityId the ability id (for error messages)
     * @param display   the ability's parsed display/lore
     * @param mechanics the ability's parsed mechanic entries
     * @throws IllegalArgumentException if any lore placeholder has no matching mechanic parameter
     */
    private static void validateAbilityLorePlaceholders(String abilityId,
            SkillDefinition.AbilityDisplay display,
            List<SkillDefinition.MechanicEntry> mechanics) {
        if (display.lore() == null || display.lore().isEmpty()) return;
        Set<String> paramKeys = new HashSet<>();
        for (SkillDefinition.MechanicEntry me : mechanics) {
            paramKeys.addAll(me.parameters().keySet());
        }
        for (String line : display.lore()) {
            var matcher = PLACEHOLDER_PATTERN.matcher(line);
            while (matcher.find()) {
                String placeholder = matcher.group(1);
                if (!paramKeys.contains(placeholder)) {
                    throw new IllegalArgumentException("Ability '" + abilityId
                            + "' lore references unknown placeholder {" + placeholder
                            + "}; available mechanic parameters: " + paramKeys);
                }
            }
        }
    }

    private SkillDefinition.Requirements parseRequirements(Map<String, Object> map, String abilityId) {
        if (map == null) return new SkillDefinition.Requirements(0, List.of(), List.of());
        ParameterEvaluator cooldown = parseCooldown(map.get("cooldown"));
        String reqContext = "Ability '" + abilityId + "' requirements";
        List<String> state = asStringList(map, "state", reqContext);
        state.forEach(s -> validateAndWarmState(s, reqContext));
        List<Map<String, Object>> itemsRaw = asMapList(map, "items", reqContext);
        List<SkillDefinition.ItemRequirement> items = itemsRaw.stream().map(this::parseItemRequirement).toList();

        SkillDefinition.Exhaustion exhaustion = null;
        if (map.containsKey("exhaustion")) {
            Map<String, Object> exMap = castMap(map.get("exhaustion"));
            double amount = asDouble(exMap, "amount", 1.0, reqContext + " exhaustion");
            double minimum = asDouble(exMap, "minimum", 0.0, reqContext + " exhaustion");
            exhaustion = new SkillDefinition.Exhaustion(amount, minimum);
        }

        return new SkillDefinition.Requirements(cooldown, state, items, exhaustion);
    }

    /**
     * Parses a cooldown requirement as either a scalar seconds value or an inline
     * evaluator block (e.g. {@code { linear: { base, step } }}). Scalars are wrapped
     * in a {@link ConstantEvaluator}.
     *
     * @param raw the raw YAML value for the cooldown key
     * @return the resolved cooldown evaluator
     */
    private ParameterEvaluator parseCooldown(Object raw) {
        if (raw == null) return new ConstantEvaluator(0.0);
        if (raw instanceof Number n) {
            return new ConstantEvaluator(n.doubleValue());
        }
        if (raw instanceof String s) {
            throw new IllegalArgumentException("Cooldown must be a number or evaluator block, got string: " + s);
        }
        return parseInlineEvaluator(castMap(raw));
    }

    private SkillDefinition.ItemRequirement parseItemRequirement(Map<String, Object> map) {
        String itemContext = "Item requirement";
        String action = asString(map, "action", "possession", itemContext);
        String tag = asString(map, "tag", itemContext);
        // Fail fast: a missing tag would NPE inside the requirement resolver on
        // the event path; reject it here at load instead.
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Item requirement missing required 'tag'");
        }
        String slot = asString(map, "slot", "HAND", itemContext);
        validateTagReference(tag);
        if (!io.github.chasehuegel.skilling.engine.requirements.RequirementEngine.isKnownSlot(slot)) {
            throw new IllegalArgumentException("Item requirement has unknown slot: " + slot
                    + " (supported: HAND, MAIN_HAND, OFF_HAND, HEAD/HELMET, CHEST, LEGS, FEET/BOOTS, ANY, ALL)");
        }
        int amount = asInt(map, "amount", 1, itemContext);
        double itemCooldown = asDouble(map, "item_cooldown", 0.0, itemContext);
        return new SkillDefinition.ItemRequirement(action, tag, slot, amount, itemCooldown);
    }

    /**
     * Fail-fast validation of a filter/requirement tag or material reference so a
     * typo is rejected at load instead of throwing inside an event handler at runtime.
     * Known references are also pre-resolved into the tag resolver's cache so no
     * tag or material resolution work happens on the event path.
     */
    private void validateTagReference(String reference) {
        if (reference != null && !tagResolver.isKnown(reference)) {
            throw new IllegalArgumentException("Unknown tag or material in filter/requirement: " + reference);
        }
        if (reference != null && !reference.isBlank()) {
            tagResolver.warm(reference);
        }
    }

    /**
     * Fail-fast validation of a {@code state:} reference. The state key (before
     * the first colon) must be a registered {@code StateFilterRegistry} filter,
     * a {@code player_placed} value must be {@code true} or {@code false}, and a
     * {@code biome} value must resolve to a known biome, so a typo is rejected
     * at load instead of silently never matching (or throwing) on the event
     * path. The {@code equipped_*} target tag is also pre-warmed.
     *
     * @param state   the raw state string (e.g. {@code equipped_all:#c:heavy_armor})
     * @param context human-readable load context for error messages
     */
    private void validateAndWarmState(String state, String context) {
        if (state == null || state.isBlank()) return;
        int colonIdx = state.indexOf(':');
        String key = colonIdx > 0 ? state.substring(0, colonIdx) : state;
        if (stateFilterRegistry.get(key) == null) {
            throw new IllegalArgumentException(context + " references unknown state '" + key + "'");
        }
        if ("player_placed".equals(key)) {
            String value = colonIdx > 0 ? state.substring(colonIdx + 1) : "";
            if (!value.equals("true") && !value.equals("false")) {
                throw new IllegalArgumentException(context
                        + " state 'player_placed' value must be true or false, got '" + value + "'");
            }
        }
        if ("biome".equals(key)) {
            String value = colonIdx > 0 ? state.substring(colonIdx + 1) : "";
            try {
                var biomeKey = org.bukkit.NamespacedKey.fromString(value);
                if (biomeKey == null || org.bukkit.Registry.BIOME.get(biomeKey) == null) {
                    throw new IllegalArgumentException(context
                            + " state 'biome' value '" + value + "' is not a known biome");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(context
                        + " state 'biome' value '" + value + "' is not a valid namespaced key", e);
            }
        }
        if ("cause".equals(key)) {
            String value = colonIdx > 0 ? state.substring(colonIdx + 1) : "";
            if (!io.github.chasehuegel.skilling.engine.requirements.DamageCauseFilter.isValidValue(value)) {
                throw new IllegalArgumentException(context
                        + " state 'cause' value '" + value
                        + "' is not a supported damage cause (burn, fire, lava, drowning, suffocation, cactus, starvation)");
            }
        }
        if ("honey_level".equals(key)) {
            String value = colonIdx > 0 ? state.substring(colonIdx + 1) : "";
            String[] parts = value.split(":", 2);
            String comparison = parts[0];
            if (!comparison.equals("below") && !comparison.equals("above") && !comparison.equals("exactly")) {
                throw new IllegalArgumentException(context
                        + " state 'honey_level' comparison must be below, above, or exactly, got '" + comparison + "'");
            }
            if (parts.length < 2) {
                throw new IllegalArgumentException(context
                        + " state 'honey_level' is missing a honey level, use " + comparison + ":N");
            }
            try {
                Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(context
                        + " state 'honey_level' level '" + parts[1] + "' is not an integer", ex);
            }
        }
        if (colonIdx > 0 && (state.startsWith("equipped_all:") || state.startsWith("equipped_any:"))) {
            validateTagReference(state.substring(colonIdx + 1));
        }
    }

    private SkillDefinition.OnFailure parseOnFailure(Map<String, Object> map, String abilityId) {
        if (map == null) return new SkillDefinition.OnFailure(Map.of());
        Map<String, SkillDefinition.FailureFeedback> failures = new HashMap<>();
        for (var entry : map.entrySet()) {
            Map<String, Object> feedbackMap = castMap(entry.getValue());
            String actionBar = feedbackString(feedbackMap, "action_bar");
            List<Map<String, Object>> sounds = asMapList(feedbackMap, "sounds", "on_failure '" + entry.getKey() + "' of ability '" + abilityId + "'");
            validateFeedbackSounds("on_failure '" + entry.getKey() + "' of ability '" + abilityId + "'", sounds);
            failures.put(entry.getKey(), new SkillDefinition.FailureFeedback(actionBar, sounds));
        }
        return new SkillDefinition.OnFailure(failures);
    }

    private List<SkillDefinition.MechanicEntry> parseMechanics(String skillId, String abilityId, Object mechanicsRaw) {
        if (!(mechanicsRaw instanceof List<?> list)) return List.of();
        List<SkillDefinition.MechanicEntry> entries = new ArrayList<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Mechanic entry must be a map, got: " + raw);
            }
            Map<String, Object> mechanicMap = castMap(map);
            String type = asString(mechanicMap, "type", "Mechanic");
            if (type == null) throw new IllegalArgumentException("Mechanic entry missing 'type'");
            if (!mechanicRegistry.contains(type)) {
                throw new IllegalArgumentException("Unknown mechanic type: " + type);
            }

            Map<String, Object> rawParams = castMap(mechanicMap.getOrDefault("parameters", Map.of()));
            // Reject parameters the mechanic does not support so a typo'd key
            // (which the impl silently ignores) is caught at load, not left
            // dangling as a no-op the author thinks is active.
            List<String> supportedParams = mechanicRegistry.getParameterNames(type);
            for (String paramKey : rawParams.keySet()) {
                if (!supportedParams.contains(paramKey)) {
                    throw new IllegalArgumentException("Mechanic '" + type + "' of ability '" + abilityId
                            + "' in skill '" + skillId + "' does not support parameter '" + paramKey
                            + "'; supported: " + supportedParams);
                }
            }
            Map<String, ParameterEvaluator> parameters = new HashMap<>();
            // Constant-valued parameters (e.g. a namespaced effect key) are handed
            // to the mechanic's load-time validator so a typo fails here, not in
            // an event handler. Level-scaled evaluators cannot be resolved without
            // a level context and are skipped.
            Map<String, Object> constantParams = new HashMap<>();
            for (var paramEntry : rawParams.entrySet()) {
                Object rawValue = paramEntry.getValue();
                if (!(rawValue instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException("Mechanic '" + type + "' parameter '"
                            + paramEntry.getKey() + "' must be an evaluator block (e.g. '"
                            + paramEntry.getKey() + ": { constant: 2 }'), got: " + rawValue);
                }
                Map<String, Object> evaluatorMap = castMap(rawValue);
                ParameterEvaluator evaluator = parseInlineEvaluator(evaluatorMap);
                parameters.put(paramEntry.getKey(), evaluator);
                Object constant = constantValueOf(evaluator);
                if (constant != null) {
                    constantParams.put(paramEntry.getKey(), constant);
                }
            }
            mechanicRegistry.validate(type, "ability '" + abilityId + "' in skill '" + skillId + "'", constantParams);

            List<Map<String, Object>> filtersRaw = asMapList(mechanicMap, "filters", "Mechanic '" + type + "' of ability '" + abilityId + "'");
            List<SkillDefinition.Filter> filters = filtersRaw.stream()
                    .map(fm -> {
                        String target = fm.get("target") != null ? String.valueOf(fm.get("target")) : null;
                        String state = fm.get("state") != null ? String.valueOf(fm.get("state")) : null;
                        String tool = fm.get("tool") != null ? String.valueOf(fm.get("tool")) : null;
                        validateTagReference(target);
                        validateTagReference(tool);
                        validateAndWarmState(state, "Mechanic '" + type + "' of ability '" + abilityId + "'");
                        return new SkillDefinition.Filter(target, state, tool);
                    })
                    .toList();

            entries.add(new SkillDefinition.MechanicEntry(type, filters, parameters));
        }
        return entries;
    }

    /**
     * Returns the constant value carried by a parameter evaluator, or null when
     * the evaluator is level-scaled (cannot be resolved at load time).
     *
     * @param evaluator the parsed parameter evaluator
     * @return the constant string/number, or null
     */
    private static Object constantValueOf(ParameterEvaluator evaluator) {
        if (evaluator instanceof ConstantEvaluator ce) return ce.rawValue();
        return null;
    }

    private SkillDefinition.Feedback parseFeedback(Map<String, Object> map, String abilityId) {
        if (map == null) {
            return new SkillDefinition.Feedback(false, false, "", List.of(), List.of());
        }
        Map<String, Object> notify = castMap(map.get("notify"));
        String feedbackContext = "feedback of ability '" + abilityId + "'";
        boolean actionBar = io.github.chasehuegel.skilling.engine.mechanic.impl.MechanicParamValidators
                .bool(feedbackContext, notify.getOrDefault("action_bar", false), "action_bar");
        boolean chat = io.github.chasehuegel.skilling.engine.mechanic.impl.MechanicParamValidators
                .bool(feedbackContext, notify.getOrDefault("chat", false), "chat");
        String message = feedbackString(notify, "message");

        List<Map<String, Object>> particles = asMapList(map, "particles", "feedback of ability '" + abilityId + "'");
        List<Map<String, Object>> sounds = asMapList(map, "sounds", "feedback of ability '" + abilityId + "'");

        validateFeedbackParticles("feedback of ability '" + abilityId + "'", particles);
        validateFeedbackSounds("feedback of ability '" + abilityId + "'", sounds);

        return new SkillDefinition.Feedback(actionBar, chat, message, particles, sounds);
    }

    /**
     * Reads a feedback string, coalescing an absent or empty scalar (SnakeYAML
     * yields {@code null} for a key written as {@code key:}) to an empty string
     * so the event path never NPEs on {@code null.isBlank()}.
     *
     * @param map the feedback map
     * @param key the feedback key
     * @return the string value, or "" when absent/empty
     */
    private static String feedbackString(Map<String, Object> map, String key) {
        Object raw = map.get(key);
        if (raw == null) return "";
        if (raw instanceof String s) return s;
        throw new IllegalArgumentException("Feedback '" + key + "' must be a string, got: " + raw);
    }

    private void validateFeedbackSounds(String context, List<Map<String, Object>> sounds) {
        for (var sound : sounds) {
            io.github.chasehuegel.skilling.engine.mechanic.impl.MechanicParamValidators.sound(context, sound, "type");
        }
    }

    private void validateFeedbackParticles(String context, List<Map<String, Object>> particles) {
        for (var particle : particles) {
            io.github.chasehuegel.skilling.engine.mechanic.impl.MechanicParamValidators.particle(context, particle, "type");
        }
    }

    /**
     * Parses an inline evaluator block from a YAML parameter entry.
     *
     * <p>Supports formats:
     * <ul>
     *   <li>{@code constant: value}</li>
     *   <li>{@code linear: { base, step, min?, max? }}</li>
     *   <li>{@code milestones: { level: value, ... }}</li>
     * </ul>
     *
     * <p>An empty map is a scalar-like value where an evaluator block was
     * expected; callers that legitimately have no value must pass {@code null},
     * which resolves to a zero constant.
     *
     * @param map the evaluator configuration map, or null when no value supplied
     * @return the parsed evaluator
     * @throws IllegalArgumentException if the evaluator type is unknown
     */
    public ParameterEvaluator parseInlineEvaluator(Map<String, Object> map) {
        if (map == null) {
            return new ConstantEvaluator(0.0);
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException(
                    "Evaluator block must be a non-empty map with a supported type key "
                            + "(constant, linear, milestones, polynomial, or a registered evaluator type), got: " + map);
        }

        // Check for direct constant value
        if (map.containsKey("value") && map.size() == 1) {
            Object val = map.get("value");
            if (val instanceof Number n) {
                return new ConstantEvaluator(n.doubleValue());
            }
            return new ConstantEvaluator(String.valueOf(val));
        }

        // Check for known evaluator type keys
        if (map.containsKey("constant")) {
            Object val = map.get("constant");
            if (val instanceof Number n) {
                return new ConstantEvaluator(n.doubleValue());
            }
            if (val instanceof String s) {
                return new ConstantEvaluator(s);
            }
            Map<String, Object> nested = castMap(val);
            Object nestedValue = nested.getOrDefault("value", 0.0);
            if (nestedValue instanceof Number n) {
                return new ConstantEvaluator(n.doubleValue());
            }
            return new ConstantEvaluator(String.valueOf(nestedValue));
        }

        if (map.containsKey("linear")) {
            Map<String, Object> linearMap = castMap(map.get("linear"));
            String linearContext = "linear evaluator";
            double base = asDouble(linearMap, "base", 0.0, linearContext);
            double step = asDouble(linearMap, "step", 0.0, linearContext);
            double min = linearMap.containsKey("min")
                    ? asDouble(linearMap, "min", Double.NEGATIVE_INFINITY, linearContext)
                    : Double.NEGATIVE_INFINITY;
            double max = linearMap.containsKey("max")
                    ? asDouble(linearMap, "max", Double.POSITIVE_INFINITY, linearContext)
                    : Double.POSITIVE_INFINITY;
            return new LinearEvaluator(base, step, min, max);
        }

        if (map.containsKey("milestones")) {
            Object rawMilestones = map.get("milestones");
            if (!(rawMilestones instanceof Map<?, ?>)) {
                // A list-valued milestones block (e.g. an un-normalized web editor
                // payload) would otherwise silently resolve to an empty curve.
                throw new IllegalArgumentException(
                        "Milestones must be a map of level: value, got: " + rawMilestones);
            }
            Map<String, Object> milestonesMap = castMap(rawMilestones);
            TreeMap<Integer, Double> milestones = new TreeMap<>();
            for (var entry : milestonesMap.entrySet()) {
                try {
                    milestones.put(Integer.parseInt(entry.getKey()),
                            asNumber(entry.getValue(), "milestone value").doubleValue());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid milestone level key: " + entry.getKey(), e);
                }
            }
            return new MilestoneEvaluator(milestones);
        }

        if (map.containsKey("polynomial")) {
            Map<String, Object> polyMap = castMap(map.get("polynomial"));
            double baseXp = asDouble(polyMap, "base_xp", 50.0, "polynomial evaluator");
            double exponent = asDouble(polyMap, "exponent", 2.5, "polynomial evaluator");
            return new PolynomialEvaluator(baseXp, exponent);
        }

        // A registered custom evaluator type (e.g. { logistic: {...} }) is usable
        // as a YAML parameter evaluator key.
        if (map.size() == 1) {
            String type = map.keySet().iterator().next();
            if (evaluatorRegistry.contains(type)) {
                return evaluatorRegistry.create(type);
            }
        }

        // Fallback: treat the entire map as a constant with single value
        if (map.size() == 1 && map.values().iterator().next() instanceof Number n) {
            return new ConstantEvaluator(n.doubleValue());
        }

        throw new IllegalArgumentException("Unknown evaluator type in: " + map);
    }

    /**
     * Normalizes a raw YAML value to a string-keyed map, or the empty map when
     * no value was supplied ({@code null}). Any other non-map value — a scalar
     * supplied where a map was expected — fails fast so a typo (e.g. a numeric
     * reward or parameter) is rejected at load instead of silently evaluating
     * to zero at runtime.
     *
     * @param raw the raw YAML value
     * @return the normalized map
     * @throws IllegalArgumentException if the value is a non-map non-null scalar
     */
    private Map<String, Object> castMap(Object raw) {
        if (raw == null) return Map.of();
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        throw new IllegalArgumentException("Expected a YAML map, got: " + raw);
    }

    private static String asString(Map<String, Object> map, String key, String context) {
        Object raw = map.get(key);
        if (raw == null) return null;
        if (raw instanceof String s) return s;
        throw new IllegalArgumentException(context + " '" + key + "' must be a string, got: " + raw);
    }

    private static String asString(Map<String, Object> map, String key, String defaultValue, String context) {
        Object raw = map.get(key);
        if (raw == null) return defaultValue;
        if (raw instanceof String s) return s;
        throw new IllegalArgumentException(context + " '" + key + "' must be a string, got: " + raw);
    }

    private static double asDouble(Map<String, Object> map, String key, double defaultValue, String context) {
        Object raw = map.get(key);
        if (raw == null) return defaultValue;
        if (raw instanceof Number n) return n.doubleValue();
        throw new IllegalArgumentException(context + " '" + key + "' must be a number, got: " + raw);
    }

    private static int asInt(Map<String, Object> map, String key, int defaultValue, String context) {
        Object raw = map.get(key);
        if (raw == null) return defaultValue;
        if (raw instanceof Number n) return n.intValue();
        throw new IllegalArgumentException(context + " '" + key + "' must be a number, got: " + raw);
    }

    private static Number asNumber(Object raw, String context) {
        if (raw instanceof Number n) return n;
        throw new IllegalArgumentException(context + " must be a number, got: " + raw);
    }

    private static List<String> asStringList(Map<String, Object> map, String key, String context) {
        Object raw = map.get(key);
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object element : list) {
                if (element instanceof String s) out.add(s);
                else throw new IllegalArgumentException(context + " '" + key
                        + "' must be a list of strings, got element: " + element);
            }
            return out;
        }
        throw new IllegalArgumentException(context + " '" + key + "' must be a list of strings, got: " + raw);
    }

    private static List<String> asStringList(List<?> raw, String context) {
        if (raw == null) return List.of();
        List<String> out = new ArrayList<>(raw.size());
        for (Object element : raw) {
            if (element instanceof String s) out.add(s);
            else throw new IllegalArgumentException(context + " must be a list of strings, got element: " + element);
        }
        return out;
    }

    private List<Map<String, Object>> asMapList(Map<String, Object> map, String key, String context) {
        Object raw = map.get(key);
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>(list.size());
            for (Object element : list) {
                if (element instanceof Map<?, ?>) {
                    out.add(castMap(element));
                } else {
                    throw new IllegalArgumentException(context + " '" + key
                            + "' must be a list of maps, got element: " + element);
                }
            }
            return out;
        }
        throw new IllegalArgumentException(context + " '" + key + "' must be a list of maps, got: " + raw);
    }

    /**
     * Returns all loaded skill definitions keyed by ID.
     *
     * @return the skill map
     */
    public Map<String, SkillDefinition> getSkills() {
        // The field is already an immutable snapshot swapped in atomically.
        return skills;
    }

    /**
     * Returns the XP sources bound to a trigger key, with their owning skills.
     *
     * @param triggerKey the trigger key (e.g. {@code block_break})
     * @return the indexed sources for the trigger, in load order
     */
    public List<XpSourceRef> xpSourcesFor(String triggerKey) {
        return xpSourcesByTrigger.getOrDefault(triggerKey, List.of());
    }

    /**
     * Returns the abilities bound to a trigger key, with their owning skills.
     *
     * @param triggerKey the trigger key (e.g. {@code block_break})
     * @return the indexed abilities for the trigger, in load order
     */
    public List<AbilityRef> abilitiesFor(String triggerKey) {
        return abilitiesByTrigger.getOrDefault(triggerKey, List.of());
    }

    /**
     * Returns a skill definition by its ID.
     *
     * @param id the skill ID
     * @return the skill definition, or null if not loaded
     */
    public SkillDefinition getSkill(String id) {
        return skills.get(id);
    }

    private List<SkillDefinition.LevelUpCommand> parseLevelUpCommands(List<?> list) {
        if (list == null) return List.of();
        List<SkillDefinition.LevelUpCommand> cmds = new ArrayList<>();
        for (Object raw : list) {
            if (raw instanceof String s && !s.isBlank()) {
                cmds.add(new SkillDefinition.LevelUpCommand(s));
            }
        }
        return cmds;
    }

    /**
     * Replaces the tag resolver used for filter resolution.
     *
     * @param tagResolver the new tag resolver
     */
    public void setTagResolver(TagResolver tagResolver) {
        this.tagResolver = tagResolver;
    }

    /**
     * Replaces the ability registry used for reusable ability base-merge.
     *
     * @param abilityManager the new ability registry
     */
    public void setAbilityManager(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    /**
     * Returns the ability registry used for reusable ability base-merge.
     *
     * @return the current ability registry, or null when none is set
     */
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    /**
     * Clears all loaded skills.
     */
    public void clear() {
        this.skills = Map.of();
        this.xpSourcesByTrigger = Map.of();
        this.abilitiesByTrigger = Map.of();
    }
}