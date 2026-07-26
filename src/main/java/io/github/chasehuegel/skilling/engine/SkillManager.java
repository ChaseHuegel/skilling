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

    private final EvaluatorRegistry evaluatorRegistry;
    private final MechanicRegistry mechanicRegistry;
    private final TriggerRegistry triggerRegistry;
    private final TagResolver tagResolver;
    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

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
        skills.clear();
        if (!skillsDir.exists() || !skillsDir.isDirectory()) return;

        File[] files = skillsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            SkillDefinition def = parseSkill(file);
            skills.put(def.id(), def);
        }
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

        return new SkillDefinition(id, maxLevel, display, progression, xpSources, abilities);
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
        return new SkillDefinition.Display(name, icon, customModelData, color, style);
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
            default -> throw new IllegalArgumentException("Unknown progression curve: " + curve);
        };

        return new SkillDefinition.Progression(curve, baseXp, exponent, evaluator);
    }

    private List<SkillDefinition.XpSource> parseXpSources(List<?> list) {
        if (list == null) return List.of();
        List<SkillDefinition.XpSource> sources = new ArrayList<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> map)) continue;
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
                        filters.add(new SkillDefinition.Filter(target, state, tool));
                    }
                }
            }

            Map<String, Object> rewardRaw = castMap(entry.get("reward"));
            ParameterEvaluator reward = parseInlineEvaluator(rewardRaw);

            sources.add(new SkillDefinition.XpSource(trigger, filters, reward));
        }
        return sources;
    }

    private List<SkillDefinition.Ability> parseAbilities(List<?> list) {
        if (list == null) return List.of();
        List<SkillDefinition.Ability> abilities = new ArrayList<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> map)) continue;
            Map<String, Object> abilityMap = castMap(map);

            String id = (String) abilityMap.get("id");
            if (id == null) throw new IllegalArgumentException("Ability missing 'id'");

            String displayName = (String) abilityMap.getOrDefault("display_name", id);
            int unlockLevel = ((Number) abilityMap.getOrDefault("unlock_level", 1)).intValue();

            // Display lore
            SkillDefinition.AbilityDisplay abilityDisplay = parseAbilityDisplay(castMap(abilityMap.get("display")));

            // Requirements
            SkillDefinition.Requirements requirements = parseRequirements(castMap(abilityMap.get("requirements")));
            SkillDefinition.OnFailure onFailure = parseOnFailure(castMap(abilityMap.get("on_failure")));

            // Mechanics
            List<SkillDefinition.MechanicEntry> mechanics = parseMechanics(abilityMap.get("mechanics"));

            // Feedback
            SkillDefinition.Feedback feedback = parseFeedback(castMap(abilityMap.get("feedback")));

            abilities.add(new SkillDefinition.Ability(id, displayName, unlockLevel, abilityDisplay,
                    requirements, onFailure, mechanics, feedback));
        }
        return abilities;
    }

    private SkillDefinition.AbilityDisplay parseAbilityDisplay(Map<String, Object> map) {
        if (map == null) return new SkillDefinition.AbilityDisplay(List.of());
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) map.getOrDefault("lore", List.of());
        return new SkillDefinition.AbilityDisplay(lore);
    }

    private SkillDefinition.Requirements parseRequirements(Map<String, Object> map) {
        if (map == null) return new SkillDefinition.Requirements(0, List.of(), List.of());
        double cooldown = ((Number) map.getOrDefault("cooldown", 0.0)).doubleValue();
        @SuppressWarnings("unchecked")
        List<String> state = (List<String>) map.getOrDefault("state", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) map.getOrDefault("items", List.of());
        List<SkillDefinition.ItemRequirement> items = itemsRaw.stream().map(this::parseItemRequirement).toList();
        return new SkillDefinition.Requirements(cooldown, state, items);
    }

    private SkillDefinition.ItemRequirement parseItemRequirement(Map<String, Object> map) {
        String action = (String) map.getOrDefault("action", "possession");
        String tag = (String) map.get("tag");
        String slot = (String) map.getOrDefault("slot", "HAND");
        int amount = ((Number) map.getOrDefault("amount", 1)).intValue();
        double itemCooldown = ((Number) map.getOrDefault("item_cooldown", 0.0)).doubleValue();
        return new SkillDefinition.ItemRequirement(action, tag, slot, amount, itemCooldown);
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
            if (!(raw instanceof Map<?, ?> map)) continue;
            Map<String, Object> mechanicMap = castMap(map);
            String type = (String) mechanicMap.get("type");
            if (type == null) throw new IllegalArgumentException("Mechanic entry missing 'type'");

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
                    .map(fm -> new SkillDefinition.Filter((String) fm.get("target"), (String) fm.get("state"), (String) fm.get("tool")))
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
            return new ConstantEvaluator(((Number) map.get("value")).doubleValue());
        }

        // Check for known evaluator type keys
        if (map.containsKey("constant")) {
            Object val = map.get("constant");
            if (val instanceof Number n) {
                return new ConstantEvaluator(n.doubleValue());
            }
            Map<String, Object> nested = castMap(val);
            return new ConstantEvaluator(((Number) nested.getOrDefault("value", 0.0)).doubleValue());
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
                milestones.put(Integer.parseInt(entry.getKey()), ((Number) entry.getValue()).doubleValue());
            }
            return new MilestoneEvaluator(milestones);
        }

        if (map.containsKey("polynomial")) {
            Map<String, Object> polyMap = castMap(map.get("polynomial"));
            double baseXp = ((Number) polyMap.getOrDefault("base_xp", 50.0)).doubleValue();
            double exponent = ((Number) polyMap.getOrDefault("exponent", 2.5)).doubleValue();
            return new PolynomialEvaluator(baseXp, exponent);
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
        return Collections.unmodifiableMap(skills);
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

    /**
     * Clears all loaded skills.
     */
    public void clear() {
        skills.clear();
    }
}