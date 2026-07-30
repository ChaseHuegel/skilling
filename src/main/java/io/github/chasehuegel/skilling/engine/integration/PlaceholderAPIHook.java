package io.github.chasehuegel.skilling.engine.integration;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class PlaceholderAPIHook {
    private final Skilling plugin;
    private Object expansion;
    private Method registerMethod;

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

            registerMethod = pluginClass.getMethod("registerPlaceholderExpansion", expansionClass);
            registerMethod.invoke(null, expansion);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
        }
    }

    private String onRequest(Object playerObj, String params) {
        var player = (org.bukkit.entity.Player) playerObj;
        String[] parts = params.split("_", 2);
        if (parts.length < 2) return "";

        String action = parts[0];
        String skillId = parts[1];

        if (action.equals("total_levels")) {
            int total = 0;
            var profile = plugin.getProfileManager().getProfile(player.getUniqueId());
            if (profile == null) return "0";
            for (var entry : profile.getXpSnapshot().entrySet()) {
                var skill = plugin.getSkillManager().getSkill(entry.getKey());
                if (skill != null) total += skill.getLevelForXp(entry.getValue());
            }
            return String.valueOf(total);
        }

        SkillDefinition skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) return "0";

        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) return "0";

        long xp = profile.getXp(skillId);
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

    public void unregister() {
        try {
            if (expansion != null) {
                Method unregister = expansion.getClass().getMethod("unregister");
                unregister.invoke(expansion);
            }
        } catch (Exception ignored) {}
    }
}
