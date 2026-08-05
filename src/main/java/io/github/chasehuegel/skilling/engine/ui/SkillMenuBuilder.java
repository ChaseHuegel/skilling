package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.ui.GuiLayoutConfig.FillerConfig;
import io.github.chasehuegel.skilling.engine.ui.branding.BrandingConfig;
import io.github.chasehuegel.skilling.engine.ui.branding.SkillColorCode;
import io.github.chasehuegel.skilling.engine.ui.branding.TemplateRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Bukkit {@link Inventory} objects from {@link SkillDefinition}
 * records with lazy instantiation and cache invalidation.
 *
 * <p>Menus are generated on demand and cached inside the
 * {@link PlayerProfile}. The cache is invalidated when the player's
 * level changes, forcing a rebuild on the next menu open.
 *
 * <p>If a {@link GuiLayoutConfig} is loaded from {@code gui.yml}, the
 * builder produces a paginated multi-page chest GUI. Otherwise it
 * falls back to the flat linear layout.
 *
 * <p>Lore and GUI chrome are rendered from the {@code branding} section
 * of {@code config.yml} via {@link TemplateRenderer}; skill and ability
 * lore placeholders are injected live through {@link LoreResolver}.
 */
public final class SkillMenuBuilder {

    private static final int MENU_SIZE = 54;

    private final SkillManager skillManager;
    private volatile GuiLayoutConfig guiLayoutConfig;

    public SkillMenuBuilder(SkillManager skillManager, GuiLayoutConfig guiLayoutConfig) {
        this.skillManager = skillManager;
        this.guiLayoutConfig = guiLayoutConfig;
    }

    public void setGuiLayoutConfig(GuiLayoutConfig guiLayoutConfig) {
        this.guiLayoutConfig = guiLayoutConfig;
    }

    public Inventory buildOverview(PlayerProfile profile) {
        if (guiLayoutConfig.isPresent()) {
            return buildPaginatedOverview(profile);
        }
        return buildFlatOverview(profile);
    }

    private Inventory buildFlatOverview(PlayerProfile profile) {
        var player = Bukkit.getPlayer(profile.getPlayerId());
        Inventory inventory = Bukkit.createInventory(new SkillInventoryHolder(player, -1, null, 0), MENU_SIZE,
                TemplateRenderer.toComponent(currentBranding().gui().title()));

        int slot = 0;
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            if (slot >= MENU_SIZE) break;
            inventory.setItem(slot, buildSkillIcon(skill, profile));
            slot++;
        }

        return inventory;
    }

    private Inventory buildPaginatedOverview(PlayerProfile profile) {
        Map<Integer, Inventory> cached = profile.getCachedPageInventories();
        if (cached != null && !cached.isEmpty()) {
            return cached.get(0);
        }

        var player = Bukkit.getPlayer(profile.getPlayerId());
        if (player == null) return buildFlatOverview(profile);

        BrandingConfig branding = currentBranding();
        List<String> pageOrder = guiLayoutConfig.getPageOrder();
        int pageCount = pageOrder.size();
        Map<Integer, Inventory> inventories = new HashMap<>();

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            GuiPage page = guiLayoutConfig.getPage(pageOrder.get(pageIndex));
            if (page == null) continue;

            int size = page.inventorySize();
            Inventory inventory = Bukkit.createInventory(
                    new SkillInventoryHolder(player, pageIndex, pageOrder, pageCount),
                    size,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(page.displayTitle()));

            // Fill all slots with filler
            ItemStack filler = createFillerPane();
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, filler);
            }

            // Place navigation arrows on the last row
            if (pageIndex > 0) {
                inventory.setItem(page.prevSlot(), createNavItem(branding.gui().prevPage()));
            }
            if (pageIndex < pageCount - 1) {
                inventory.setItem(page.nextSlot(), createNavItem(branding.gui().nextPage()));
            }

            // Place page indicator at center of last row
            inventory.setItem(page.indicatorSlot(), buildPageIcon(page));

            // Place skill icons
            for (var entry : page.skillSlots().entrySet()) {
                String skillId = entry.getKey();
                int slot = entry.getValue();
                SkillDefinition skill = skillManager.getSkill(skillId);
                if (skill == null) {
                    Bukkit.getLogger().warning("Skill '" + skillId + "' from gui.yml not found in registry, skipping slot " + slot);
                    continue;
                }
                inventory.setItem(slot, buildSkillIcon(skill, profile));
            }

            // Tag everything with poison pill
            tagAllItems(inventory);

            inventories.put(pageIndex, inventory);
        }

        profile.setCachedPageInventories(inventories);
        return inventories.get(0);
    }

    private void tagAllItems(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
                item.editMeta(meta -> PoisonPillTag.apply(meta));
            }
        }
    }

    private ItemStack createFillerPane() {
        FillerConfig filler = guiLayoutConfig.getFillerConfig();
        Material material = Material.matchMaterial(filler.material());
        if (material == null) {
            material = Material.BLACK_STAINED_GLASS_PANE;
        }
        ItemStack pane = new ItemStack(material);
        pane.editMeta(meta -> {
            meta.displayName(Component.empty());
            meta.setHideTooltip(true);
            if (filler.customModelData() > 0) {
                meta.setCustomModelData(filler.customModelData());
            }
            PoisonPillTag.apply(meta);
        });
        return pane;
    }

    private ItemStack createNavItem(String template) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        arrow.editMeta(meta -> {
            meta.displayName(TemplateRenderer.toComponent(TemplateRenderer.renderLine(template, Map.of())));
            PoisonPillTag.apply(meta);
        });
        return arrow;
    }

    private ItemStack buildPageIcon(GuiPage page) {
        Material material = Material.matchMaterial(page.icon());
        if (material == null) material = Material.BOOK;
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(page.displayTitle()));
            int skillCount = page.skillSlots().size();
            String countLine = TemplateRenderer.renderLine(currentBranding().gui().pageCount(),
                    Map.of("count", String.valueOf(skillCount)));
            meta.lore(List.of(TemplateRenderer.toComponent(countLine)));
            if (page.customModelData() > 0) {
                meta.setCustomModelData(page.customModelData());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            PoisonPillTag.apply(meta);
        });
        return item;
    }

    public List<Component> buildSkillLore(SkillDefinition skill, PlayerProfile profile) {
        BrandingConfig branding = currentBranding();
        long currentXp = profile.getXp(skill.id());
        int level = skill.getLevelForXp(currentXp);
        int maxLevel = skill.maxLevel();
        boolean maxed = level >= maxLevel;

        long xpForCurrent = level > 0 ? (long) skill.progression().evaluator().evaluate(level, 0) : 0;
        long xpForNext = level < maxLevel ? (long) skill.progression().evaluator().evaluate(level + 1, 0) : 0;
        long xpInto = currentXp - xpForCurrent;
        // At max level there is no next threshold; pin the needed value to the
        // XP into the level so the progress line reads full ("XP: 5 / 5") and
        // the bar renders filled instead of showing a stale/zero divisor.
        long xpNeeded = maxed ? xpInto : (xpForNext - xpForCurrent);

        String colorCode = skillColorCode(skill);
        int filled = filledCount(branding.barTemplate(), xpInto, xpNeeded, maxed);
        String bar = TemplateRenderer.renderBar(branding.barTemplate(), branding.barTemplate().width(), filled);

        Map<String, String> scalars = new HashMap<>();
        scalars.put("level", String.valueOf(level));
        scalars.put("max_level", String.valueOf(maxLevel));
        scalars.put("bar", bar);
        scalars.put("xp_into", String.valueOf(xpInto));
        scalars.put("xp_needed", String.valueOf(xpNeeded));
        scalars.put("xp_total", String.valueOf(currentXp));
        scalars.put("color", colorCode);

        Map<String, List<String>> inserts = new HashMap<>();
        inserts.put("lore", resolveSkillLore(skill, level, maxLevel, currentXp));
        inserts.put("abilities", buildAbilityBlock(branding, skill, level));

        List<String> rendered = TemplateRenderer.renderLines(branding.skillTemplate().lines(), inserts, scalars);
        return TemplateRenderer.toComponents(rendered).stream()
                .map(component -> component.decoration(TextDecoration.ITALIC, false))
                .toList();
    }

    private ItemStack buildSkillIcon(SkillDefinition skill, PlayerProfile profile) {
        long currentXp = profile.getXp(skill.id());
        int level = skill.getLevelForXp(currentXp);
        boolean unlocked = level > 0;

        Material material = Material.matchMaterial(skill.display().icon());
        if (material == null || level == 0) {
            material = Material.BARRIER;
        }

        ItemStack item = new ItemStack(material);

        item.editMeta(meta -> {
            BrandingConfig branding = currentBranding();
            String skillName = skill.display().name() != null ? skill.display().name() : skill.id();
            Map<String, String> scalars = Map.of("name", skillName, "color", skillColorCode(skill));
            String nameLine = unlocked
                    ? TemplateRenderer.renderLine(branding.gui().skillNameUnlocked(), scalars)
                    : TemplateRenderer.renderLine(branding.gui().skillNameLocked(), scalars);
            meta.displayName(TemplateRenderer.toComponent(nameLine));

            meta.lore(buildSkillLore(skill, profile));

            if (skill.display().customModelData() > 0) {
                meta.setCustomModelData(skill.display().customModelData());
            }

            meta.setMaxStackSize(Math.clamp(skill.maxLevel(), 1, 99));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            PoisonPillTag.apply(meta);
        });

        item.setAmount(Math.clamp(level, 1, 99));

        return item;
    }

    /**
     * Formats an ability's header line (the first line of the locked or
     * unlocked ability template) for log output and unlock announcements.
     *
     * @param ability     the ability to format
     * @param playerLevel the player's current skill level
     * @return the rendered header line component
     */
    public static Component formatAbilityLine(SkillDefinition.Ability ability, int playerLevel) {
        BrandingConfig branding = currentBranding();
        boolean unlocked = playerLevel >= ability.unlockLevel();
        List<String> template = unlocked
                ? branding.abilities().unlocked() : branding.abilities().locked();
        String type = isActiveAbility(ability, playerLevel)
                ? branding.abilityType().active() : branding.abilityType().passive();
        Map<String, String> scalars = Map.of(
                "name", ability.displayName() != null ? ability.displayName() : ability.id(),
                "level", String.valueOf(ability.unlockLevel()),
                "type", type);
        // Empty lore drops the {lore} template line, leaving just the header.
        List<String> rendered = TemplateRenderer.renderLines(template, Map.of("lore", List.of()), scalars);
        return rendered.isEmpty() ? Component.empty() : TemplateRenderer.toComponent(rendered.get(0));
    }

    private static List<String> buildAbilityBlock(BrandingConfig branding, SkillDefinition skill, int playerLevel) {
        if (skill.abilities() == null || skill.abilities().isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        for (SkillDefinition.Ability ability : skill.abilities()) {
            boolean unlocked = playerLevel >= ability.unlockLevel();
            List<String> abilityTemplate = unlocked
                    ? branding.abilities().unlocked() : branding.abilities().locked();
            String type = isActiveAbility(ability, playerLevel)
                    ? branding.abilityType().active() : branding.abilityType().passive();
            Map<String, String> scalars = Map.of(
                    "name", ability.displayName() != null ? ability.displayName() : ability.id(),
                    "level", String.valueOf(ability.unlockLevel()),
                    "type", type);
            List<String> abilityBlock = TemplateRenderer.renderLines(abilityTemplate,
                    Map.of("lore", resolveAbilityLore(ability, playerLevel)), scalars);
            result.addAll(TemplateRenderer.renderLines(branding.abilitiesTemplate().lines(),
                    Map.of("ability", abilityBlock), Map.of()));
        }
        return result;
    }

    private static List<String> resolveSkillLore(SkillDefinition skill, int level, int maxLevel, long currentXp) {
        var skillLore = skill.display() != null ? skill.display().lore() : null;
        if (skillLore == null || skillLore.isEmpty()) return List.of();
        String name = skill.display().name() != null ? skill.display().name() : skill.id();
        Map<String, ParameterEvaluator> params = Map.of(
                "level", new ConstantEvaluator(level),
                "max_level", new ConstantEvaluator(maxLevel),
                "skill_name", new ConstantEvaluator(name),
                "xp", new ConstantEvaluator(currentXp));
        return LoreResolver.resolveAll(skillLore, params, level, 0);
    }

    /**
     * Whether an ability renders as {@code Active} (has a cooldown, state
     * requirements, or item requirements) versus {@code Passive}.
     *
     * @param ability     the ability to classify
     * @param playerLevel the player's current skill level
     * @return true when the ability is active
     */
    public static boolean isActiveAbility(SkillDefinition.Ability ability, int playerLevel) {
        return ability.requirements().cooldown().evaluate(playerLevel, ability.unlockLevel()) > 0
                || !ability.requirements().state().isEmpty()
                || !ability.requirements().items().isEmpty();
    }

    private static int filledCount(BrandingConfig.BarTemplate bar, long into, long needed, boolean maxed) {
        double progress;
        if (maxed) {
            progress = 1.0;
        } else if (needed <= 0) {
            progress = 0.0;
        } else {
            progress = Math.min(Math.max((double) into / needed, 0.0), 1.0);
        }
        return (int) Math.round(progress * bar.width());
    }

    private static String skillColorCode(SkillDefinition skill) {
        String color = skill.display() != null ? skill.display().color() : null;
        String code = SkillColorCode.toLegacyCode(color);
        return code != null ? code : "&f";
    }

    private static BrandingConfig currentBranding() {
        Skilling instance = Skilling.getInstance();
        return instance != null ? instance.getBranding() : BrandingConfig.DEFAULT;
    }

    /**
     * Resolves an ability's lore lines, injecting live evaluator outputs for
     * {@code {placeholder}} tokens sourced from the ability's mechanic parameters.
     *
     * @param ability     the ability whose lore to resolve
     * @param playerLevel the player's current skill level
     * @return the resolved lore lines, or an empty list if the ability has no lore
     */
    public static List<String> resolveAbilityLore(SkillDefinition.Ability ability, int playerLevel) {
        if (ability.display() == null || ability.display().lore() == null || ability.display().lore().isEmpty()) {
            return List.of();
        }
        Map<String, ParameterEvaluator> allParams = new HashMap<>();
        if (ability.mechanics() != null) {
            for (SkillDefinition.MechanicEntry me : ability.mechanics()) {
                allParams.putAll(me.parameters());
            }
        }
        return LoreResolver.resolveAll(ability.display().lore(), allParams, playerLevel, ability.unlockLevel());
    }
}
