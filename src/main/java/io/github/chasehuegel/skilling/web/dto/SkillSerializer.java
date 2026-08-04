package io.github.chasehuegel.skilling.web.dto;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillSerializer {

    private static final Yaml YAML;

    static {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        opts.setIndent(2);
        opts.setWidth(120);
        YAML = new Yaml(opts);
    }

    private SkillSerializer() {}

    public static SkillDetailDTO parseSkillFile(File file) {
        try (var reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Map<String, Object> raw = YAML.load(reader);
            return fromMap(raw);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse skill file: " + file.getName(), e);
        }
    }

    public static SkillDetailDTO fromMap(Map<String, Object> raw) {
        String id = str(raw, "id");
        String displayName = str(raw, "display_name");
        if (displayName == null) {
            Map<String, Object> d = map(raw, "display");
            displayName = str(d, "name", id);
        }
        int maxLevel = intVal(raw, "max_level", 100);

        Map<String, Object> display = map(raw, "display");
        String icon = str(display, "icon", "minecraft:barrier");
        int cmd = intVal(display, "custom_model_data", 0);
        String color = str(display, "color", "WHITE");
        String style = str(display, "style", "SOLID");
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) display.getOrDefault("lore", List.of());

        Map<String, Object> prog = map(raw, "progression");
        var progression = new SkillDetailDTO.ProgressionDTO(
            str(prog, "curve", "polynomial"),
            doubleVal(prog, "base_xp", 50.0),
            doubleVal(prog, "exponent", 2.5)
        );

        List<SkillDetailDTO.XpSourceDTO> xpSources = new ArrayList<>();
        List<Map<String, Object>> xpRaw = listMap(raw, "xp_sources");
        if (xpRaw != null) {
            for (Map<String, Object> x : xpRaw) {
                xpSources.add(new SkillDetailDTO.XpSourceDTO(
                    str(x, "trigger"),
                    parseFilters(listMap(x, "filters")),
                    parseEvaluator(map(x, "reward"))
                ));
            }
        }

        List<SkillDetailDTO.AbilityDTO> abilities = new ArrayList<>();
        List<Map<String, Object>> abRaw = listMap(raw, "abilities");
        if (abRaw != null) {
            for (Map<String, Object> a : abRaw) {
                abilities.add(parseAbility(a));
            }
        }

        List<SkillDetailDTO.LevelUpCommandDTO> levelUpCommands = new ArrayList<>();
        List<String> cmdRaw = listStr(raw, "level_up_commands");
        if (cmdRaw != null) {
            for (String c : cmdRaw) {
                levelUpCommands.add(new SkillDetailDTO.LevelUpCommandDTO(c));
            }
        }

        return new SkillDetailDTO(id, displayName, maxLevel, icon, cmd, color, style, lore,
            progression, xpSources, abilities, levelUpCommands);
    }

    public static Map<String, Object> toMap(SkillDetailDTO dto) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", dto.id());
        root.put("display_name", dto.displayName());
        root.put("max_level", dto.maxLevel());

        Map<String, Object> display = new LinkedHashMap<>();
        display.put("name", dto.displayName());
        display.put("icon", dto.icon());
        if (dto.customModelData() > 0) display.put("custom_model_data", dto.customModelData());
        display.put("color", dto.color());
        display.put("style", dto.style());
        if (dto.lore() != null && !dto.lore().isEmpty()) {
            display.put("lore", dto.lore());
        }
        root.put("display", display);

        Map<String, Object> prog = new LinkedHashMap<>();
        // The engine derives every curve from base_xp (+ exponent for polynomial);
        // linear and constant have no other parameters, so the web writes the same
        // key set the engine reads. Writing base/step/etc. would be silently ignored.
        prog.put("curve", dto.progression().curve());
        prog.put("base_xp", dto.progression().baseXp());
        prog.put("exponent", dto.progression().exponent());
        root.put("progression", prog);

        List<Map<String, Object>> xpSources = new ArrayList<>();
        for (var x : dto.xpSources()) {
            Map<String, Object> xm = new LinkedHashMap<>();
            xm.put("trigger", x.trigger());
            if (x.filters() != null && !x.filters().isEmpty()) {
                xm.put("filters", filtersToMap(x.filters()));
            }
            xm.put("reward", evaluatorToMap(x.reward()));
            xpSources.add(xm);
        }
        root.put("xp_sources", xpSources);

        List<Map<String, Object>> abilities = new ArrayList<>();
        for (var a : dto.abilities()) {
            abilities.add(abilityToMap(a));
        }
        root.put("abilities", abilities);

        if (dto.levelUpCommands() != null && !dto.levelUpCommands().isEmpty()) {
            root.put("level_up_commands",
                    dto.levelUpCommands().stream().map(SkillDetailDTO.LevelUpCommandDTO::command).toList());
        }

        return root;
    }

    public static String toYaml(SkillDetailDTO dto) {
        Map<String, Object> map = toMap(dto);
        return YAML.dump(map);
    }

    public static SkillDetailDTO fromYaml(String yamlContent) {
        Map<String, Object> raw = YAML.load(yamlContent);
        return fromMap(raw);
    }

    private static SkillDetailDTO.AbilityDTO parseAbility(Map<String, Object> raw) {
        String id = str(raw, "id");
        String displayName = str(raw, "display_name", id);
        int unlockLevel = intVal(raw, "unlock_level", 1);
        String trigger = str(raw, "trigger");
        if (trigger == null || trigger.isBlank()) {
            throw new IllegalArgumentException("Ability '" + id + "' missing required 'trigger' field");
        }

        Map<String, Object> displayMap = map(raw, "display");
        List<String> lore = new ArrayList<>();
        if (displayMap.containsKey("lore")) {
            lore = (List<String>) displayMap.get("lore");
        }

        Map<String, Object> reqMap = map(raw, "requirements");
        var requirements = parseRequirements(reqMap);

        List<Map<String, Object>> mechRaw = listMap(raw, "mechanics");
        List<SkillDetailDTO.MechanicEntryDTO> mechanics = new ArrayList<>();
        if (mechRaw != null) {
            for (Map<String, Object> m : mechRaw) {
                Map<String, SkillDetailDTO.EvaluatorDTO> params = new LinkedHashMap<>();
                Map<String, Object> rawParams = map(m, "parameters");
                for (var entry : rawParams.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> paramMap) {
                        Map<String, Object> converted = new LinkedHashMap<>();
                        paramMap.forEach((k, v) -> converted.put(k.toString(), v));
                        params.put(entry.getKey(), parseEvaluator(converted));
                    }
                }
                mechanics.add(new SkillDetailDTO.MechanicEntryDTO(
                    str(m, "type"),
                    parseFilters(listMap(m, "filters")),
                    params
                ));
            }
        }

        // Parse feedback
        Map<String, Object> fbMap = map(raw, "feedback");
        Map<String, Object> notify = map(fbMap, "notify");
        List<Map<String, Object>> particles = listMap(fbMap, "particles");
        List<Map<String, Object>> sounds = listMap(fbMap, "sounds");
        var feedback = new SkillDetailDTO.FeedbackDTO(
            boolVal(notify, "action_bar", false),
            boolVal(notify, "chat", false),
            str(notify, "message", ""),
            particles != null ? particles : List.of(),
            sounds != null ? sounds : List.of()
        );

        // Parse on_failure
        SkillDetailDTO.OnFailureDTO onFailure = parseOnFailureDTO(map(raw, "on_failure"));

        return new SkillDetailDTO.AbilityDTO(id, displayName, unlockLevel, trigger,
            new SkillDetailDTO.AbilityDisplayDTO(lore),
            requirements, mechanics, onFailure, feedback);
    }

    private static SkillDetailDTO.OnFailureDTO parseOnFailureDTO(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) return new SkillDetailDTO.OnFailureDTO(Map.of());
        Map<String, SkillDetailDTO.FailureFeedbackDTO> reasons = new LinkedHashMap<>();
        for (var entry : raw.entrySet()) {
            Map<String, Object> reasonMap = castMap(entry.getValue());
            String actionBar = str(reasonMap, "action_bar", "");
            List<Map<String, Object>> sounds = listMap(reasonMap, "sounds");
            if (sounds == null) sounds = List.of();
            reasons.put(entry.getKey(), new SkillDetailDTO.FailureFeedbackDTO(actionBar, sounds));
        }
        return new SkillDetailDTO.OnFailureDTO(reasons);
    }

    private static Map<String, Object> onFailureToMap(SkillDetailDTO.OnFailureDTO dto) {
        if (dto == null || dto.reasons() == null || dto.reasons().isEmpty()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : dto.reasons().entrySet()) {
            Map<String, Object> reasonMap = new LinkedHashMap<>();
            String actionBar = entry.getValue().actionBar();
            if (actionBar != null && !actionBar.isBlank()) {
                reasonMap.put("action_bar", actionBar);
            }
            List<Map<String, Object>> sounds = entry.getValue().sounds();
            if (sounds != null && !sounds.isEmpty()) {
                reasonMap.put("sounds", sounds);
            }
            if (!reasonMap.isEmpty()) {
                result.put(entry.getKey(), reasonMap);
            }
        }
        return result;
    }

    private static Map<String, Object> abilityToMap(SkillDetailDTO.AbilityDTO a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id());
        m.put("display_name", a.displayName());
        m.put("unlock_level", a.unlockLevel());
        m.put("trigger", a.trigger());

        Map<String, Object> displayMap = new LinkedHashMap<>();
        if (a.display() != null && a.display().lore() != null && !a.display().lore().isEmpty()) {
            displayMap.put("lore", a.display().lore());
        }
        if (!displayMap.isEmpty()) m.put("display", displayMap);

        Map<String, Object> reqMap = new LinkedHashMap<>();
        var cooldown = a.requirements().cooldown();
        if (cooldown != null) {
            if ("constant".equals(cooldown.type())) {
                double cd = doubleVal(cooldown.params(), "value", 0);
                if (cd > 0) reqMap.put("cooldown", cd);
            } else {
                reqMap.put("cooldown", evaluatorToMap(cooldown));
            }
        }
        if (a.requirements().state() != null && !a.requirements().state().isEmpty()) {
            reqMap.put("state", a.requirements().state());
        }
        if (a.requirements().items() != null && !a.requirements().items().isEmpty()) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (var item : a.requirements().items()) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("action", item.action());
                im.put("tag", item.tag());
                im.put("slot", item.slot());
                im.put("amount", item.amount());
                if (item.itemCooldown() > 0) im.put("item_cooldown", item.itemCooldown());
                items.add(im);
            }
            reqMap.put("items", items);
        }
        if (a.requirements().exhaustion() != null) {
            Map<String, Object> exMap = new LinkedHashMap<>();
            exMap.put("amount", a.requirements().exhaustion().amount());
            exMap.put("minimum", a.requirements().exhaustion().minimum());
            reqMap.put("exhaustion", exMap);
        }
        m.put("requirements", reqMap);

        List<Map<String, Object>> mechanics = new ArrayList<>();
        for (var me : a.mechanics()) {
            Map<String, Object> meMap = new LinkedHashMap<>();
            meMap.put("type", me.type());
            if (me.filters() != null && !me.filters().isEmpty()) {
                meMap.put("filters", filtersToMap(me.filters()));
            }
            Map<String, Object> params = new LinkedHashMap<>();
            for (var entry : me.parameters().entrySet()) {
                params.put(entry.getKey(), evaluatorToMap(entry.getValue()));
            }
            meMap.put("parameters", params);
            mechanics.add(meMap);
        }
        m.put("mechanics", mechanics);

        Map<String, Object> fbMap = new LinkedHashMap<>();
        Map<String, Object> notify = new LinkedHashMap<>();
        notify.put("action_bar", a.feedback().actionBar());
        notify.put("chat", a.feedback().chat());
        notify.put("message", a.feedback().message());
        fbMap.put("notify", notify);
        fbMap.put("particles", a.feedback().particles() != null ? a.feedback().particles() : List.of());
        fbMap.put("sounds", a.feedback().sounds() != null ? a.feedback().sounds() : List.of());
        m.put("feedback", fbMap);

        Map<String, Object> ofMap = onFailureToMap(a.onFailure());
        if (!ofMap.isEmpty()) {
            m.put("on_failure", ofMap);
        }

        return m;
    }

    private static SkillDetailDTO.RequirementsDTO parseRequirements(Map<String, Object> raw) {
        SkillDetailDTO.EvaluatorDTO cooldown = parseCooldown(raw.get("cooldown"));
        List<String> state = raw.containsKey("state") ? (List<String>) raw.get("state") : List.of();
        List<Map<String, Object>> itemsRaw = listMap(raw, "items");
        List<SkillDetailDTO.ItemRequirementDTO> items = new ArrayList<>();
        if (itemsRaw != null) {
            for (Map<String, Object> im : itemsRaw) {
                items.add(new SkillDetailDTO.ItemRequirementDTO(
                    str(im, "action", "possession"),
                    str(im, "tag"),
                    str(im, "slot", "HAND"),
                    intVal(im, "amount", 1),
                    doubleVal(im, "item_cooldown", 0)
                ));
            }
        }
        SkillDetailDTO.ExhaustionDTO exhaustion = null;
        if (raw.containsKey("exhaustion")) {
            Map<String, Object> exMap = castMap(raw.get("exhaustion"));
            exhaustion = new SkillDetailDTO.ExhaustionDTO(
                doubleVal(exMap, "amount", 1.0),
                doubleVal(exMap, "minimum", 0.0)
            );
        }
        return new SkillDetailDTO.RequirementsDTO(cooldown, state, items, exhaustion);
    }

    /**
     * Parses a cooldown requirement value: a plain number becomes a constant evaluator,
     * an object is resolved as an evaluator block.
     *
     * @param raw the raw cooldown value
     * @return the parsed evaluator DTO
     */
    private static SkillDetailDTO.EvaluatorDTO parseCooldown(Object raw) {
        if (raw instanceof Number n) {
            return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", n.doubleValue()));
        }
        if (raw instanceof Map<?, ?> m) {
            return parseEvaluator(castMap(m));
        }
        return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", 0.0));
    }

    static List<SkillDetailDTO.FilterDTO> parseFilters(List<Map<String, Object>> raw) {
        if (raw == null) return List.of();
        return raw.stream().map(f -> new SkillDetailDTO.FilterDTO(
            str(f, "target"), str(f, "state"), str(f, "tool")
        )).toList();
    }

    static List<Map<String, Object>> filtersToMap(List<SkillDetailDTO.FilterDTO> filters) {
        return filters.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            if (f.target() != null && !f.target().isBlank()) m.put("target", f.target());
            if (f.state() != null && !f.state().isBlank()) m.put("state", f.state());
            if (f.tool() != null && !f.tool().isBlank()) m.put("tool", f.tool());
            return m;
        }).toList();
    }

    static SkillDetailDTO.EvaluatorDTO parseEvaluator(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", 0.0));
        }

        if (raw.containsKey("constant")) {
            Object val = raw.get("constant");
            if (val instanceof Number n) {
                return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", n.doubleValue()));
            }
            if (val instanceof String s) {
                return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", s));
            }
            Map<String, Object> nested = (Map<String, Object>) val;
            Object nestedValue = nested.getOrDefault("value", 0.0);
            if (nestedValue instanceof String s) {
                return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", s));
            }
            return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", doubleVal(nested, "value", 0)));
        }
        if (raw.containsKey("linear")) {
            Map<String, Object> n = (Map<String, Object>) raw.get("linear");
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("base", doubleVal(n, "base", 0));
            params.put("step", doubleVal(n, "step", 0));
            if (n.containsKey("min")) params.put("min", doubleVal(n, "min", 0));
            if (n.containsKey("max")) params.put("max", doubleVal(n, "max", 0));
            return new SkillDetailDTO.EvaluatorDTO("linear", params);
        }
        if (raw.containsKey("milestones")) {
            return new SkillDetailDTO.EvaluatorDTO("milestones",
                    Map.of("milestones", normalizeMilestones(raw.get("milestones"))));
        }
        if (raw.containsKey("polynomial")) {
            Map<String, Object> n = (Map<String, Object>) raw.get("polynomial");
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("base_xp", doubleVal(n, "base_xp", 50));
            params.put("exponent", doubleVal(n, "exponent", 2.5));
            return new SkillDetailDTO.EvaluatorDTO("polynomial", params);
        }
        if (raw.size() == 1 && raw.values().iterator().next() instanceof Number n) {
            return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", n.doubleValue()));
        }
        // A single-key object whose type is not a built-in is a registered custom
        // evaluator (e.g. { logistic: {...} }); preserve the type and raw params
        // instead of collapsing it to a constant 0.
        if (raw.size() == 1) {
            String type = raw.keySet().iterator().next();
            Object nested = raw.get(type);
            if (nested instanceof Map<?, ?> m) {
                return new SkillDetailDTO.EvaluatorDTO(type, castMap(m));
            }
            return new SkillDetailDTO.EvaluatorDTO(type, Map.of("value", nested));
        }
        throw new IllegalArgumentException("Unknown evaluator type in: " + raw);
    }

    static Map<String, Object> evaluatorToMap(SkillDetailDTO.EvaluatorDTO ev) {
        return switch (ev.type()) {
            case "constant" -> {
                Object value = ev.params().getOrDefault("value", 0);
                yield value instanceof String s
                        ? Map.of("constant", s)
                        : Map.of("constant", Map.of("value", value));
            }
            case "linear" -> Map.of("linear", ev.params());
            case "milestones" -> Map.of("milestones",
                    normalizeMilestones(ev.params().getOrDefault("milestones", Map.of())));
            case "polynomial" -> Map.of("polynomial", ev.params());
            // A registered custom evaluator type (e.g. { logistic: {...} }) must
            // round-trip as-is; rewriting it to a constant would zero the parameter.
            default -> Map.of(ev.type(), ev.params() != null ? ev.params() : Map.of());
        };
    }

    /**
     * Normalizes a milestone evaluator's parameter value to an ordered
     * {@code level → value} map, accepting either a YAML/JSON map shape
     * ({@code {25: 3, 50: 8}}) or the editor's array-of-rows shape
     * ({@code [{level: 25, value: 3}, ...]}). The engine's
     * {@code parseInlineEvaluator} always expects the map form, so the web
     * serializer must never emit (or pass through) a list.
     *
     * @param raw the raw milestones value
     * @return an ordered {@code level → value} map
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> normalizeMilestones(Object raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (raw instanceof Map<?, ?> m) {
            m.forEach((k, v) -> {
                if (k != null && v != null) result.put(String.valueOf(k), v);
            });
        } else if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> row) {
                    Object level = row.get("level");
                    Object value = row.get("value");
                    if (level != null && value != null) {
                        result.put(String.valueOf(level), value);
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            Map<String, Object> result = new LinkedHashMap<>();
            m.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        }
        return new LinkedHashMap<>();
    }

    static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    static String str(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v != null ? v.toString() : def;
    }

    static int intVal(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }

    static double doubleVal(Map<String, Object> map, String key, double def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.doubleValue();
        return def;
    }

    static boolean boolVal(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        return def;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Map<String, Object> parent, String key) {
        Object v = parent.get(key);
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> result = new LinkedHashMap<>();
            m.forEach((k, val) -> result.put(k.toString(), val));
            return result;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> listMap(Map<String, Object> parent, String key) {
        Object v = parent.get(key);
        if (v instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> converted = new LinkedHashMap<>();
                    m.forEach((k, val) -> converted.put(k.toString(), val));
                    result.add(converted);
                }
            }
            return result;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static List<String> listStr(Map<String, Object> parent, String key) {
        Object v = parent.get(key);
        if (v instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s) {
                    result.add(s);
                }
            }
            return result;
        }
        return null;
    }
}
