package io.github.chasehuegel.skilling.engine.integration;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class PlaceholderAPIHook {
    private final Skilling plugin;
    private Object expansion;

    public PlaceholderAPIHook(Skilling plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void register() {
        try {
            Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            Class<?> pluginClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");

            expansion = java.lang.reflect.Proxy.newProxyInstance(
                    expansionClass.getClassLoader(),
                    new Class<?>[]{expansionClass},
                    (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "getIdentifier" -> "skilling";
                            case "getAuthor" -> "ChaseHuegel";
                            case "getVersion" -> plugin.getPluginMeta().getVersion();
                            case "persist" -> true;
                            case "onPlaceholderRequest" -> {
                                if (args == null || args.length < 2) yield "";
                                var player = args[0];
                                var params = (String) args[1];
                                if (player == null || params == null) yield "";
                                yield onRequest(player, params);
                            }
                            default -> method.invoke(this, args);
                        };
                    });

            Method registerMethod = pluginClass.getMethod("registerPlaceholderExpansion", expansionClass);
            registerMethod.invoke(null, expansion);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
        }
    }

    String onRequest(Object playerObj, String params) {
        var player = (org.bukkit.entity.Player) playerObj;
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
                long next = (long) skill.progression().evaluator().evaluate(level + 1, 0);
                yield String.valueOf(next);
            }
            case "progress" -> {
                long current = (long) skill.progression().evaluator().evaluate(level, 0);
                long next = (long) skill.progression().evaluator().evaluate(level + 1, 0);
                double pct = (double) (xp - current) / (next - current) * 100;
                yield String.format("%.1f", Math.min(pct, 100.0));
            }
            case "remaining" -> {
                long next = (long) skill.progression().evaluator().evaluate(level + 1, 0);
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

    private String resolveEvaluator(Object playerObj, String params) {
        var player = (org.bukkit.entity.Player) playerObj;
        String[] parts = params.split("_", 2);
        if (parts.length < 2) return "0";

        String skillId = parts[0];
        String abilityParam = parts[1];

        SkillDefinition skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) return "0";

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

    public void unregister() {
        try {
            if (expansion != null) {
                Method unregister = expansion.getClass().getMethod("unregister");
                unregister.invoke(expansion);
            }
        } catch (Exception ignored) {}
    }
}
