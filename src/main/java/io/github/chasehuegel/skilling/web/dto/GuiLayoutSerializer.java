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
 */
public final class GuiLayoutSerializer {

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
     * Returns the default layout if the content is null or empty.
     */
    @SuppressWarnings("unchecked")
    public static GuiLayoutDTO parse(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return GuiLayoutDTO.empty();
        }

        Map<String, Object> map = YAML.load(yamlContent);
        if (map == null) return GuiLayoutDTO.empty();

        String title = str(map, "title");
        int rows = intVal(map, "rows", 6);
        int version = intVal(map, "version", 1);

        List<Map<String, Object>> pagesRaw = null;
        Object pagesObj = map.get("pages");
        if (pagesObj instanceof List pagesList) {
            pagesRaw = (List<Map<String, Object>>) (List<?>) pagesList;
        }

        List<GuiLayoutDTO.GuiPageDTO> pages = new ArrayList<>();

        if (pagesRaw != null) {
            for (var pageMap : pagesRaw) {
                String label = str(pageMap, "label");
                Object slotsObj = pageMap.get("slots");
                Map<Integer, String> slots = new LinkedHashMap<>();
                if (slotsObj instanceof Map<?, ?> slotsRaw) {
                    for (var entry : slotsRaw.entrySet()) {
                        Object key = entry.getKey();
                        int slotIndex;
                        if (key instanceof Integer i) {
                            slotIndex = i;
                        } else if (key instanceof String s) {
                            slotIndex = Integer.parseInt(s);
                        } else {
                            continue;
                        }
                        slots.put(slotIndex, entry.getValue() != null ? entry.getValue().toString() : "");
                    }
                }
                pages.add(new GuiLayoutDTO.GuiPageDTO(label, slots));
            }
        }

        return new GuiLayoutDTO(title, rows, pages, version);
    }

    /**
     * Serialize a {@link GuiLayoutDTO} to a YAML string.
     */
    public static String serialize(GuiLayoutDTO dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", dto.title());
        map.put("rows", dto.rows());
        map.put("version", dto.version());

        List<Map<String, Object>> pagesList = new ArrayList<>();
        for (var page : dto.pages()) {
            Map<String, Object> pageMap = new LinkedHashMap<>();
            pageMap.put("label", page.label());
            Map<String, String> slotsMap = new LinkedHashMap<>();
            for (var entry : page.slots().entrySet()) {
                slotsMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            pageMap.put("slots", slotsMap);
            pagesList.add(pageMap);
        }
        map.put("pages", pagesList);

        return YAML.dump(map);
    }

    private static String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private static int intVal(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        return def;
    }
}
