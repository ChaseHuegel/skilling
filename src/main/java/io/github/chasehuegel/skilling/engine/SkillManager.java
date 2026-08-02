package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantValueEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.MilestoneEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.PolynomialEvaluator;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
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

    private static final java.util.regex.Pattern PLACEHOLDER_PATTERN =
            java.util.regex.Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    private final EvaluatorRegistry evaluatorRegistry;
    private final MechanicRegistry mechanicRegistry;
    private final TriggerRegistry triggerRegistry;
    private TagResolver tagResolver;
    private volatile Map<String, SkillDefinition> skills = Map.of();

    /**
     * Constructs a new skill manager.
     *
     * @param evaluatorRegistry the evaluator registry for instantiating parameter evaluators
     * @param mechanicRegistry  the mechanic registry for instantiating mechanics
     * @param triggerRegistry   the trigger registry for instantiating triggers
     * @param tagResolver       the tag resolver for filter resolution
     */
    public SkillManager(EvaluatorRegistry evaluatorRegistry, MechanicRegistry mechanicRegistry,
                        TriggerRegistry triggerRegistry, TagResolver tagResolver) {
        this.evaluatorRegistry = evaluatorRegistry;
        this.mechanicRegistry = mechanicRegistry;
        this.triggerRegistry = triggerRegistry;
        this.tagResolver = tagResolver;
    }

    /**
     * Loads all skill YAML files from the given directory.
     *
     * @param skillsDir the directory containing skill YAML files
     * @throws IllegalArgumentException if a file is malformed
     */
    public void loadSkills(File skillsDir) {
        // Build into a local map and atomically swap in an immutable snapshot so
        // concurrent readers (web threads, addons) never see a half-loaded view.
        // A malformed file throws here, leaving the previous skill set intact.
        Map<String, SkillDefinition> built = new LinkedHashMap<>();
        if (skillsDir.exists() && skillsDir.isDirectory()) {
            File[] files = skillsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    SkillDefinition def;
                    try {
                        def = parseSkill(file);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "Failed to parse skill file " + file.getName() + ": " + e.getMessage(), e);
                    }
                    if (built.containsKey(def.id())) {
                        throw new IllegalArgumentException("Duplicate skill ID '" + def.id() + "' in file: " + file.getName());
                    }
                    built.put(def.id(), def);
                }
            }
        }
        this.skills = Collections.unmodifiableMap(built);
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

        // Display section
        SkillDefinition.Display display = parseDisplay(config.getConfigurationSection("display"));

        // Progression section
        SkillDefinition.Progression progression = parseProgression(config.getConfigurationSection("progression"));

        // XP sources
        List<SkillDefinition.XpSource> xpSources = parseXpSources(config.getList("xp_sources"));

        // Abilities
        List<SkillDefinition.Ability> abilities = parseAbilities(config.getList("abilities"));

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
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) section.getList("lore", List.of());
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
            String trigger = (String) entry.get("trigger");
            if (trigger == null) throw new IllegalArgumentException("XP source missing 'trigger'");

            List<SkillDefinition.Filter> filters = new ArrayList<>();
            Object filtersRaw = entry.get("filters");
            if (filtersRaw instanceof List<?> filterList) {
                for (Object f : filterList) {
                    if (f instanceof Map<?, ?> fm) {
                        Map<String, Object> filterMap = castMap(fm);
                        String target = (String) filterMap.get("target");
                        String state = (String) filterMap.get("state");
                        String tool = (String) filterMap.get("tool");
                        validateTagReference(target);
                        validateTagReference(tool);
                        warmStateTarget(state);
                        filters.add(new SkillDefinition.Filter(target, state, tool));
                    }
                }
            }

            Object rewardRaw = entry.get("reward");
            if (rewardRaw == null) {
                throw new IllegalArgumentException("XP source for trigger '" + trigger + "' missing 'reward'");
            }
            ParameterEvaluator reward = parseInlineEvaluator(castMap(rewardRaw));

            sources.add(new SkillDefinition.XpSource(trigger, filters, reward));
        }
        return sources;
    }

    private List<SkillDefinition.Ability> parseAbilities(List<?> list) {
        if (list == null) return List.of();
        List<SkillDefinition.Ability> abilities = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Ability must be a map, got: " + raw);
            }
            Map<String, Object> abilityMap = castMap(map);

            String id = (String) abilityMap.get("id");
            if (id == null) throw new IllegalArgumentException("Ability missing 'id'");
            if (!seenIds.add(id)) {
                throw new IllegalArgumentException("Duplicate ability ID: " + id);
            }

            String displayName = (String) abilityMap.getOrDefault("display_name", id);
            int unlockLevel = parseUnlockLevel(id, abilityMap.getOrDefault("unlock_level", 1));

            String trigger = (String) abilityMap.get("trigger");
            if (trigger == null || trigger.isBlank()) {
                throw new IllegalArgumentException("Ability '" + id + "' missing required 'trigger' field");
            }
            if (!triggerRegistry.contains(trigger)) {
                throw new IllegalArgumentException("Ability '" + id + "' has unknown trigger: " + trigger);
            }

            // Display lore
            SkillDefinition.AbilityDisplay abilityDisplay = parseAbilityDisplay(castMap(abilityMap.get("display")));

            // Requirements
            SkillDefinition.Requirements requirements = parseRequirements(castMap(abilityMap.get("requirements")));
            SkillDefinition.OnFailure onFailure = parseOnFailure(castMap(abilityMap.get("on_failure")));

            // Mechanics
            List<SkillDefinition.MechanicEntry> mechanics = parseMechanics(abilityMap.get("mechanics"));
            validateAbilityLorePlaceholders(id, abilityDisplay, mechanics);

            // Feedback
            SkillDefinition.Feedback feedback = parseFeedback(castMap(abilityMap.get("feedback")));

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
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) map.getOrDefault("lore", List.of());
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

    private SkillDefinition.Requirements parseRequirements(Map<String, Object> map) {
        if (map == null) return new SkillDefinition.Requirements(0, List.of(), List.of());
        ParameterEvaluator cooldown = parseCooldown(map.get("cooldown"));
        @SuppressWarnings("unchecked")
        List<String> state = (List<String>) map.getOrDefault("state", List.of());
        state.forEach(this::warmStateTarget);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) map.getOrDefault("items", List.of());
        List<SkillDefinition.ItemRequirement> items = itemsRaw.stream().map(this::parseItemRequirement).toList();

        SkillDefinition.Exhaustion exhaustion = null;
        if (map.containsKey("exhaustion")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> exMap = (Map<String, Object>) map.get("exhaustion");
            double amount = ((Number) exMap.getOrDefault("amount", 1.0)).doubleValue();
            double minimum = ((Number) exMap.getOrDefault("minimum", 0.0)).doubleValue();
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
        String action = (String) map.getOrDefault("action", "possession");
        String tag = (String) map.get("tag");
        String slot = (String) map.getOrDefault("slot", "HAND");
        validateTagReference(tag);
        int amount = ((Number) map.getOrDefault("amount", 1)).intValue();
        double itemCooldown = ((Number) map.getOrDefault("item_cooldown", 0.0)).doubleValue();
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
     * Pre-warms the target reference of an {@code equipped_all}/{@code equipped_any}
     * state value (the part after the colon) so armor gating performs no tag
     * resolution on the event path. Other state values have no tag reference.
     *
     * @param state the state string (e.g. {@code equipped_all:#c:heavy_armor})
     */
    private void warmStateTarget(String state) {
        if (state == null || !state.startsWith("equipped_all:") && !state.startsWith("equipped_any:")) {
            return;
        }
        String target = state.substring(state.indexOf(':') + 1);
        validateTagReference(target);
    }

    private SkillDefinition.OnFailure parseOnFailure(Map<String, Object> map) {
        if (map == null) return new SkillDefinition.OnFailure(Map.of());
        Map<String, SkillDefinition.FailureFeedback> failures = new HashMap<>();
        for (var entry : map.entrySet()) {
            Map<String, Object> feedbackMap = castMap(entry.getValue());
            String actionBar = (String) feedbackMap.getOrDefault("action_bar", "");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sounds = (List<Map<String, Object>>) feedbackMap.getOrDefault("sounds", List.of());
            failures.put(entry.getKey(), new SkillDefinition.FailureFeedback(actionBar, sounds));
        }
        return new SkillDefinition.OnFailure(failures);
    }

    private List<SkillDefinition.MechanicEntry> parseMechanics(Object mechanicsRaw) {
        if (!(mechanicsRaw instanceof List<?> list)) return List.of();
        List<SkillDefinition.MechanicEntry> entries = new ArrayList<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Mechanic entry must be a map, got: " + raw);
            }
            Map<String, Object> mechanicMap = castMap(map);
            String type = (String) mechanicMap.get("type");
            if (type == null) throw new IllegalArgumentException("Mechanic entry missing 'type'");
            if (!mechanicRegistry.contains(type)) {
                throw new IllegalArgumentException("Unknown mechanic type: " + type);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawParams = (Map<String, Object>) mechanicMap.getOrDefault("parameters", Map.of());
            Map<String, ParameterEvaluator> parameters = new HashMap<>();
            for (var paramEntry : rawParams.entrySet()) {
                Map<String, Object> evaluatorMap = castMap(paramEntry.getValue());
                parameters.put(paramEntry.getKey(), parseInlineEvaluator(evaluatorMap));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> filtersRaw = (List<Map<String, Object>>) mechanicMap.getOrDefault("filters", List.of());
            List<SkillDefinition.Filter> filters = filtersRaw.stream()
                    .map(fm -> {
                        String target = fm.get("target") != null ? String.valueOf(fm.get("target")) : null;
                        String state = fm.get("state") != null ? String.valueOf(fm.get("state")) : null;
                        String tool = fm.get("tool") != null ? String.valueOf(fm.get("tool")) : null;
                        validateTagReference(target);
                        validateTagReference(tool);
                        return new SkillDefinition.Filter(target, state, tool);
                    })
                    .toList();

            entries.add(new SkillDefinition.MechanicEntry(type, filters, parameters));
        }
        return entries;
    }

    private SkillDefinition.Feedback parseFeedback(Map<String, Object> map) {
        if (map == null) {
            return new SkillDefinition.Feedback(false, false, "", List.of(), List.of());
        }
        Map<String, Object> notify = castMap(map.get("notify"));
        boolean actionBar = (boolean) notify.getOrDefault("action_bar", false);
        boolean chat = (boolean) notify.getOrDefault("chat", false);
        String message = (String) notify.getOrDefault("message", "");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> particles = (List<Map<String, Object>>) map.getOrDefault("particles", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sounds = (List<Map<String, Object>>) map.getOrDefault("sounds", List.of());

        return new SkillDefinition.Feedback(actionBar, chat, message, particles, sounds);
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
     * @param map the evaluator configuration map
     * @return the parsed evaluator
     * @throws IllegalArgumentException if the evaluator type is unknown
     */
    public ParameterEvaluator parseInlineEvaluator(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new ConstantEvaluator(0.0);
        }

        // Check for direct constant value
        if (map.containsKey("value") && map.size() == 1) {
            Object val = map.get("value");
            if (val instanceof Number n) {
                return new ConstantEvaluator(n.doubleValue());
            }
            return new ConstantValueEvaluator(String.valueOf(val));
        }

        // Check for known evaluator type keys
        if (map.containsKey("constant")) {
            Object val = map.get("constant");
            if (val instanceof Number n) {
                return new ConstantEvaluator(n.doubleValue());
            }
            if (val instanceof String s) {
                return new ConstantValueEvaluator(s);
            }
            Map<String, Object> nested = castMap(val);
            Object nestedValue = nested.getOrDefault("value", 0.0);
            if (nestedValue instanceof Number n) {
                return new ConstantEvaluator(n.doubleValue());
            }
            return new ConstantValueEvaluator(String.valueOf(nestedValue));
        }

        if (map.containsKey("linear")) {
            Map<String, Object> linearMap = castMap(map.get("linear"));
            double base = ((Number) linearMap.getOrDefault("base", 0.0)).doubleValue();
            double step = ((Number) linearMap.getOrDefault("step", 0.0)).doubleValue();
            double min = linearMap.containsKey("min") ? ((Number) linearMap.get("min")).doubleValue() : Double.NEGATIVE_INFINITY;
            double max = linearMap.containsKey("max") ? ((Number) linearMap.get("max")).doubleValue() : Double.POSITIVE_INFINITY;
            return new LinearEvaluator(base, step, min, max);
        }

        if (map.containsKey("milestones")) {
            Map<String, Object> milestonesMap = castMap(map.get("milestones"));
            TreeMap<Integer, Double> milestones = new TreeMap<>();
            for (var entry : milestonesMap.entrySet()) {
                try {
                    milestones.put(Integer.parseInt(entry.getKey()), ((Number) entry.getValue()).doubleValue());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid milestone level key: " + entry.getKey(), e);
                }
            }
            return new MilestoneEvaluator(milestones);
        }

        if (map.containsKey("polynomial")) {
            Map<String, Object> polyMap = castMap(map.get("polynomial"));
            double baseXp = ((Number) polyMap.getOrDefault("base_xp", 50.0)).doubleValue();
            double exponent = ((Number) polyMap.getOrDefault("exponent", 2.5)).doubleValue();
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Map.of();
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
     * Clears all loaded skills.
     */
    public void clear() {
        this.skills = Map.of();
    }
}