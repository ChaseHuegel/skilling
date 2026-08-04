package io.github.chasehuegel.skilling.engine.integration;

import io.github.chasehuegel.skilling.Skilling;

public class IntegrationManager {
    private final Skilling plugin;
    private PlaceholderAPIHook papiHook;
    private VaultHook vaultHook;
    private bStatsHook bStatsHook;

    public IntegrationManager(Skilling plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiHook = new PlaceholderAPIHook(plugin);
            if (papiHook.register()) {
                plugin.getLogger().info("PlaceholderAPI integration enabled");
            } else {
                papiHook = null;
            }
        }
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook(plugin);
            vaultHook.initialize();
            plugin.getLogger().info("Vault economy integration enabled");
        }
        bStatsHook = new bStatsHook(plugin);
        bStatsHook.initialize();
    }

    public void shutdown() {
        if (papiHook != null) papiHook.unregister();
        if (vaultHook != null) vaultHook.shutdown();
    }

    public boolean hasPlaceholderAPI() { return papiHook != null; }
    public boolean hasVault() { return vaultHook != null; }
    public boolean hasbStats() { return bStatsHook != null; }

    public PlaceholderAPIHook getPapiHook() { return papiHook; }
    public VaultHook getVaultHook() { return vaultHook; }
}
