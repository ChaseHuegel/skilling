package io.github.chasehuegel.skilling.engine.integration;

import io.github.chasehuegel.skilling.Skilling;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class BStatsHook {
    private final Skilling plugin;
    private Metrics metrics;

    public BStatsHook(Skilling plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        int pluginId = 33002;
        metrics = new Metrics(plugin, pluginId);

        metrics.addCustomChart(new SimplePie("loaded_skills", () ->
                String.valueOf(plugin.getSkillManager().getSkills().size())));

        metrics.addCustomChart(new SimplePie("loaded_mechanics", () ->
                String.valueOf(plugin.getRegistries().getMechanicRegistry().size())));

        metrics.addCustomChart(new SimplePie("loaded_triggers", () ->
                String.valueOf(plugin.getRegistries().getTriggerRegistry().size())));

        metrics.addCustomChart(new SimplePie("loaded_evaluators", () ->
                String.valueOf(plugin.getRegistries().getEvaluatorRegistry().size())));
    }
}
