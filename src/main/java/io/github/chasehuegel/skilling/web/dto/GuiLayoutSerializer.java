package io.github.chasehuegel.skilling.web.dto;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes and deserializes {@link GuiLayoutDTO} to/from YAML format.
 * Uses SnakeYAML with block-level formatting to produce human-readable output.
 *
 * <p><b>Format Compatibility:</b> The parser supports two formats on read:
 * <ul>
 *   <li><b>New format (canonical):</b> {@code pages} is a list, slots map {@code slot_index → skill_id}</li>
 *   <li><b>Legacy format (server):</b> {@code pages} is a map with named keys, skills map {@code skill_id → slot_index}</li>
 * </ul>
 * Serialization always produces the legacy format to maintain compatibility with
 * the plugin's in-game rendering ({@code GuiLayoutConfig}).
 */
public final class GuiLayoutSerializer {

    private static final String DEFAULT_TITLE = "&8\u2692 &6Skills &8\u2692";
    private static final int DEFAULT_ROWS = 6;

    private static final Yaml YAML;

    static {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        YAML = new Yaml(options);
    }

    private GuiLayoutSerializer() {}

    /**
     * Parse a YAML string into a {@link GuiLayoutDTO}.
     * Handles both the new canonical format and the legacy map-based format.
     * Returns the default layout if the content is null or empty.
     */
    @SuppressWarnings("unchecked")
    public static GuiLayoutDTO parse(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return GuiLayoutDTO.empty();
        }

        Map<String, Object> map = YAML.load(yamlContent);
        if (map == null) return GuiLayoutDTO.empty();

        Object pagesObj = map.get("pages");
        if (pagesObj instanceof List pagesList) {
            return parseNewFormat(map, (List<Map<String, Object>>) (List<?>) pagesList);
        } else if (pagesObj instanceof Map<?, ?> pagesMap) {
            return parseLegacyFormat(map, (Map<String, Object>) (Map<?, ?>) pagesMap);
        }

        return parseCommon(map, List.of());
    }

    private static GuiLayoutDTO parseCommon(Map<String, Object> map, List<GuiLayoutDTO.GuiPageDTO> pages) {
        String title = str(map, "title", DEFAULT_TITLE);
        int rows = intVal(map, "rows", DEFAULT_ROWS);
        int version = intVal(map, "version", 1);
        FillerDTO filler = parseFiller(map.get("filler"));
        return new GuiLayoutDTO(title, rows, pages, version, filler);
    }

    private static FillerDTO parseFiller(Object fillerObj) {
        if (fillerObj instanceof Map<?, ?> fillerRaw) {
            String material = fillerRaw.get("material") != null ? fillerRaw.get("material").toString() : null;
            if (material != null && !material.isBlank()) {
                int customModelData = 0;
                Object cmd = fillerRaw.get("custom_model_data");
                if (cmd instanceof Number n) customModelData = n.intValue();
                return new FillerDTO(material, customModelData);
            }
        }
        return FillerDTO.DEFAULT;
    }

    private static GuiLayoutDTO parseNewFormat(Map<String, Object> map, List<Map<String, Object>> pagesList) {
        List<GuiLayoutDTO.GuiPageDTO> pages = new ArrayList<>();
        for (var pageMap : pagesList) {
            String label = str(pageMap, "label", "&6Page");
            Map<Integer, String> slots = parseSlotsMap(pageMap.get("slots"));
            String icon = str(pageMap, "icon", "minecraft:book");
            int cmd = intVal(pageMap, "custom_model_data", 0);
            pages.add(new GuiLayoutDTO.GuiPageDTO(label, slots, icon, cmd));
        }

        return parseCommon(map, pages);
    }

    private static GuiLayoutDTO parseLegacyFormat(Map<String, Object> map, Map<String, Object> pagesMap) {
        List<GuiLayoutDTO.GuiPageDTO> pages = new ArrayList<>();
        for (var pageEntry : pagesMap.entrySet()) {
            if (!(pageEntry.getValue() instanceof Map<?, ?> pageData)) continue;
            Map<String, Object> pageMap = (Map<String, Object>) (Map<?, ?>) pageData;
            String label = str(pageMap, "title", pageEntry.getKey());
            Map<Integer, String> slots = parseLegacySkills(pageMap.get("skills"));
            String icon = str(pageMap, "icon", "minecraft:book");
            int cmd = intVal(pageMap, "custom_model_data", 0);
            pages.add(new GuiLayoutDTO.GuiPageDTO(label, slots, icon, cmd));
        }

        return parseCommon(map, pages);
    }

    private static Map<Integer, String> parseSlotsMap(Object slotsObj) {
        Map<Integer, String> slots = new LinkedHashMap<>();
        if (slotsObj instanceof Map<?, ?> slotsRaw) {
            for (var entry : slotsRaw.entrySet()) {
                int slotIndex = parseSlotKey(entry.getKey());
                if (slotIndex >= 0) {
                    slots.put(slotIndex, entry.getValue() != null ? entry.getValue().toString() : "");
                }
            }
        }
        return slots;
    }

    private static Map<Integer, String> parseLegacySkills(Object skillsObj) {
        Map<Integer, String> slots = new LinkedHashMap<>();
        if (skillsObj instanceof Map<?, ?> skillsRaw) {
            for (var entry : skillsRaw.entrySet()) {
                String skillId = entry.getKey().toString();
                Object slotVal = entry.getValue();
                int slotIndex;
                if (slotVal instanceof Number n) {
                    slotIndex = n.intValue();
                } else if (slotVal instanceof String s) {
                    try { slotIndex = Integer.parseInt(s); } catch (NumberFormatException e) { continue; }
                } else {
                    continue;
                }
                slots.put(slotIndex, skillId);
            }
        }
        return slots;
    }

    private static int parseSlotKey(Object key) {
        if (key instanceof Integer i) return i;
        if (key instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
        }
        return -1;
    }

    /**
     * Serialize a {@link GuiLayoutDTO} to a YAML string in the legacy format
     * compatible with {@code GuiLayoutConfig.load()}.
     *
     * <p>Pages are written as a map with auto-generated keys ({@code page_0},
     * {@code page_1}, ...). Skills are inverted back to the {@code skill_id → slot_index}
     * mapping expected by the plugin's in-game rendering.
     */
    public static String serialize(GuiLayoutDTO dto) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("title", dto.title());
        map.put("rows", dto.rows());
        map.put("version", dto.version());

        // Filler config, preserved from the DTO instead of defaulted
        FillerDTO filler = dto.filler() != null ? dto.filler() : FillerDTO.DEFAULT;
        Map<String, Object> fillerMap = new LinkedHashMap<>();
        fillerMap.put("material", filler.material());
        fillerMap.put("custom_model_data", filler.customModelData());
        map.put("filler", fillerMap);

        // Pages as a map with auto-generated keys
        Map<String, Object> pagesMap = new LinkedHashMap<>();
        int pageIdx = 0;
        for (var page : dto.pages()) {
            String pageKey = "page_" + pageIdx++;
            Map<String, Object> pageMap = new LinkedHashMap<>();
            pageMap.put("title", page.label());
            pageMap.put("icon", page.icon() != null ? page.icon() : "minecraft:book");
            pageMap.put("custom_model_data", page.customModelData());
            pageMap.put("rows", dto.rows());

            // Invert slots: slot_index → skill_id becomes skill_id → slot_index
            Map<String, Integer> skillsMap = new LinkedHashMap<>();
            for (var entry : page.slots().entrySet()) {
                skillsMap.put(entry.getValue(), entry.getKey());
            }
            pageMap.put("skills", skillsMap);

            pagesMap.put(pageKey, pageMap);
        }
        map.put("pages", pagesMap);

        return YAML.dump(map);
    }

    private static String str(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    private static int intVal(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        return def;
    }
}
