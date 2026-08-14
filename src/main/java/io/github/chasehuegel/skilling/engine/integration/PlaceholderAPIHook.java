package io.github.chasehuegel.skilling.engine.integration;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * Registers the {@code skilling} PlaceholderAPI expansion.
 *
 * <p>The expansion is a real {@link PlaceholderExpansion} subclass compiled
 * {@code compileOnly} against PlaceholderAPI, so its classes are only loaded
 * when the plugin is present on the server. {@link #register()} reports whether
 * the expansion actually became live so {@link IntegrationManager} can gate
 * {@code hasPlaceholderAPI()} on a working hook instead of a dangling proxy.
 */
public class PlaceholderAPIHook {
    private final Skilling plugin;
    private SkillingExpansion expansion;

    public PlaceholderAPIHook(Skilling plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the expansion, reporting whether it became live.
     *
     * @return true when PlaceholderAPI accepted the expansion, false otherwise
     */
    public boolean register() {
        try {
            expansion = new SkillingExpansion(plugin, this);
            if (expansion.register()) {
                return true;
            }
            plugin.getLogger().warning("PlaceholderAPI rejected the skilling expansion");
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
        }
        return false;
    }

    /**
     * Resolves a placeholder request against the live skill and profile state.
     *
     * @param player the player the placeholder is evaluated for
     * @param params the parameters after the {@code skilling_} prefix
     * @return the resolved placeholder value
     */
    String onRequest(Player player, String params) {
        // total_levels contains an underscore, so it must be matched whole before
        // the action/skill split (otherwise "total_levels" -> ["total", "levels"]).
        if ("total_levels".equals(params)) {
            return totalLevels(player);
        }

        String[] parts = params.split("_", 2);
        if (parts.length < 2) return "";

        String action = parts[0];
        String rest = parts[1];

        if (action.equals("evaluator")) {
            return resolveEvaluator(player, rest);
        }

        SkillDefinition skill = plugin.getSkillManager().getSkill(rest);
        if (skill == null) return "0";

        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return "0";

        long xp = profile.getXp(rest);
        int level = skill.getLevelForXp(xp);

        return switch (action) {
            case "level" -> String.valueOf(level);
            case "xp" -> String.valueOf(xp);
            case "max_xp" -> {
                long next = skill.getXpForLevel(level + 1);
                yield String.valueOf(next);
            }
            case "progress" -> {
                long current = skill.getXpForLevel(level);
                long next = skill.getXpForLevel(level + 1);
                double pct = (double) (xp - current) / (next - current) * 100;
                yield String.format("%.1f", Math.min(pct, 100.0));
            }
            case "remaining" -> {
                long next = skill.getXpForLevel(level + 1);
                yield String.valueOf(Math.max(0, next - xp));
            }
            default -> "";
        };
    }

    private String totalLevels(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return "0";
        int total = 0;
        for (SkillDefinition skill : plugin.getSkillManager().getSkills().values()) {
            total += skill.getLevelForXp(profile.getXp(skill.id()));
        }
        return String.valueOf(total);
    }

    private String resolveEvaluator(Player player, String params) {
        // Skill IDs may contain underscores, so match the longest registered
        // skill ID that prefixes the placeholder rather than splitting on the
        // first underscore (which would resolve "heavy_weapons" as "heavy").
        SkillDefinition skill = null;
        String abilityParam = "";
        for (SkillDefinition candidate : plugin.getSkillManager().getSkills().values()) {
            String prefix = candidate.id() + "_";
            if (params.startsWith(prefix) && params.length() > prefix.length()
                    && (skill == null || candidate.id().length() > skill.id().length())) {
                skill = candidate;
                abilityParam = params.substring(prefix.length());
            }
        }
        if (skill == null) return "0";

        String skillId = skill.id();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return "0";

        int currentLevel = skill.getLevelForXp(profile.getXp(skillId));

        for (SkillDefinition.Ability ability : skill.abilities()) {
            String abilityKey = ability.id().replace('-', '_');
            if (!abilityParam.startsWith(abilityKey + "_")) continue;

            String paramName = abilityParam.substring(abilityKey.length() + 1);
            for (SkillDefinition.MechanicEntry entry : ability.mechanics()) {
                var evaluator = entry.parameters().get(paramName);
                if (evaluator != null) {
                    double result = evaluator.evaluate(currentLevel, ability.unlockLevel());
                    return String.format("%.2f", result);
                }
            }
            return "0";
        }

        return "0";
    }

    /**
     * Unregisters the expansion if it was registered.
     */
    public void unregister() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }

    /**
     * The actual {@link PlaceholderExpansion} subclass backing the
     * {@code skilling} identifier. Only ever constructed when PlaceholderAPI is
     * present, so loading this class never fails on servers without it.
     */
    static final class SkillingExpansion extends PlaceholderExpansion {
        private final Skilling plugin;
        private final PlaceholderAPIHook hook;

        SkillingExpansion(Skilling plugin, PlaceholderAPIHook hook) {
            this.plugin = plugin;
            this.hook = hook;
        }

        @Override
        public String getIdentifier() {
            return "skilling";
        }

        @Override
        public String getAuthor() {
            return "ChaseHuegel";
        }

        @Override
        public String getVersion() {
            return plugin.getPluginMeta().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onPlaceholderRequest(Player player, String params) {
            if (player == null || params == null) return "";
            return hook.onRequest(player, params);
        }
    }
}
