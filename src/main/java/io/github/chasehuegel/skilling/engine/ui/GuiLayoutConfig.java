package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

/**
 * Parses and validates {@code gui.yml} into an ordered list of {@link GuiPage}.
 *
 * <p><b>YAML key:</b> {@code gui.yml} (generated on first run alongside config.yml).
 *
 * <p><b>Validation rules:</b>
 * <ul>
 *   <li>Slots 45, 49, 53 are reserved for navigation and page indicator.</li>
 *   <li>Duplicate slots within a single page throw {@link IllegalArgumentException}.</li>
 *   <li>Skill IDs not found in the registry produce a warning (fail-soft).</li>
 *   <li>Skills mapped to multiple pages produce a warning (first occurrence honored).</li>
 * </ul>
 */
public final class GuiLayoutConfig {

    private static final List<Integer> RESERVED_SLOTS = List.of(45, 49, 53);
    private static final int MIN_SLOT = 0;
    private static final int MAX_SLOT = 53;

    private final List<GuiPage> pages;
    private final Map<String, GuiPage> pageById;

    private GuiLayoutConfig(List<GuiPage> pages, Map<String, GuiPage> pageById) {
        this.pages = Collections.unmodifiableList(pages);
        this.pageById = Collections.unmodifiableMap(pageById);
    }

    /**
     * Loads and parses {@code gui.yml} from the plugin data folder.
     * Returns an empty config (with {@link #isPresent()} = false) if the
     * file does not exist or contains no pages.
     *
     * @return the parsed layout config
     */
    public static GuiLayoutConfig load() {
        Skilling plugin = Skilling.getInstance();
        if (plugin == null) return empty();

        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) return empty();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection pagesSection = config.getConfigurationSection("pages");
        if (pagesSection == null) return empty();

        List<GuiPage> pages = new ArrayList<>();
        Map<String, GuiPage> pageById = new LinkedHashMap<>();
        Set<String> seenSkills = new HashSet<>();

        for (String pageId : pagesSection.getKeys(false)) {
            ConfigurationSection pageSection = pagesSection.getConfigurationSection(pageId);
            if (pageSection == null) continue;

            String title = pageSection.getString("title");
            if (title == null || title.isBlank()) {
                plugin.getLogger().warning("Page '" + pageId + "' is missing a title, skipping.");
                continue;
            }

            String icon = pageSection.getString("icon", "minecraft:book");
            int customModelData = pageSection.getInt("custom_model_data", 0);

            ConfigurationSection skillsSection = pageSection.getConfigurationSection("skills");
            Map<String, Integer> skillSlots = new LinkedHashMap<>();
            if (skillsSection != null) {
                Set<Integer> usedSlots = new HashSet<>();
                for (String skillId : skillsSection.getKeys(false)) {
                    int slot = skillsSection.getInt(skillId, -1);

                    if (slot < MIN_SLOT || slot > MAX_SLOT) {
                        plugin.getLogger().warning("Page '" + pageId + "': skill '" + skillId
                                + "' has invalid slot " + slot + " (must be 0–53), skipping.");
                        continue;
                    }
                    if (RESERVED_SLOTS.contains(slot)) {
                        plugin.getLogger().warning("Page '" + pageId + "': skill '" + skillId
                                + "' assigned to reserved slot " + slot + " (45/49/53), skipping.");
                        continue;
                    }
                    if (!usedSlots.add(slot)) {
                        throw new IllegalArgumentException("Page '" + pageId + "' has duplicate slot " + slot
                                + " for skill '" + skillId + "'");
                    }
                    if (seenSkills.contains(skillId)) {
                        plugin.getLogger().warning("Skill '" + skillId + "' appears on multiple pages; "
                                + "only the first occurrence will be shown.");
                        continue;
                    }
                    seenSkills.add(skillId);
                    skillSlots.put(skillId, slot);
                }
            }

            GuiPage guiPage = new GuiPage(pageId, title, icon, customModelData, skillSlots);
            pages.add(guiPage);
            pageById.put(pageId, guiPage);
        }

        if (pages.isEmpty()) return empty();

        return new GuiLayoutConfig(pages, pageById);
    }

    /**
     * Returns an empty layout config indicating no GUI customization is active.
     *
     * @return an empty config
     */
    public static GuiLayoutConfig empty() {
        return new GuiLayoutConfig(List.of(), Map.of());
    }

    /**
     * Whether a valid GUI layout was loaded from {@code gui.yml}.
     *
     * @return true if at least one page is defined
     */
    public boolean isPresent() {
        return !pages.isEmpty();
    }

    /**
     * Returns the ordered list of pages as they appear in the YAML.
     *
     * @return the page list
     */
    public List<GuiPage> getPages() {
        return pages;
    }

    /**
     * Returns the ordered page IDs.
     *
     * @return list of page IDs
     */
    public List<String> getPageOrder() {
        return pages.stream().map(GuiPage::id).toList();
    }

    /**
     * Looks up a page by its ID.
     *
     * @param id the page identifier
     * @return the page, or null if not found
     */
    public GuiPage getPage(String id) {
        return pageById.get(id);
    }

    /**
     * Returns the total number of pages.
     *
     * @return page count
     */
    public int pageCount() {
        return pages.size();
    }
}
