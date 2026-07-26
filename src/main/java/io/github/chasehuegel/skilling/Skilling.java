package io.github.chasehuegel.skilling;

import io.github.chasehuegel.skilling.api.Registries;
import io.github.chasehuegel.skilling.api.SkillingAPI;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Skilling — a data-driven RPG skills engine for PaperMC.
 *
 * Initializes all registries during {@link #onEnable()} and performs a
 * synchronous database flush during {@link #onDisable()}.
 */
public final class Skilling extends JavaPlugin {

    private static Skilling instance;
    private Registries registries;
    private boolean reloading;

    /**
     * Returns the plugin singleton instance.
     *
     * @return the Skilling plugin instance
     */
    public static Skilling getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.reloading = false;

        saveDefaultConfig();
        saveResource("tags.yml", false);

        this.registries = new Registries(
                new MechanicRegistry(),
                new TriggerRegistry(),
                new EvaluatorRegistry()
        );

        Bukkit.getServicesManager().register(SkillingAPI.class, new SkillingAPI(registries), this, ServicePriority.Normal);

        getLogger().info("Skilling v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("Skilling disabled.");
    }

    /**
     * Returns the combined registries container.
     *
     * @return the registries container
     */
    public Registries getRegistries() {
        return registries;
    }

    /**
     * Whether the plugin is currently in a reload lockdown.
     *
     * @return true if a reload is in progress
     */
    public boolean isReloading() {
        return reloading;
    }

    /**
     * Sets the reload lockdown flag.
     *
     * @param reloading the new reload state
     */
    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }
}
