package io.github.chasehuegel.skilling.engine.integration;

import io.github.chasehuegel.skilling.Skilling;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;

public class VaultHook {
    private final Skilling plugin;
    private Object economy;
    private boolean enabled;
    private Method hasAccountMethod;
    private Method withdrawPlayerMethod;
    private Method depositPlayerMethod;
    private Method hasMethod;

    public VaultHook(Skilling plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            var rsp = plugin.getServer().getServicesManager().getRegistration(economyClass);
            if (rsp != null) {
                economy = rsp.getProvider();
                hasAccountMethod = economyClass.getMethod("hasAccount", OfflinePlayer.class);
                withdrawPlayerMethod = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                depositPlayerMethod = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
                hasMethod = economyClass.getMethod("has", OfflinePlayer.class, double.class);
                enabled = true;
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            enabled = false;
        }
    }

    public boolean hasAccount(OfflinePlayer player) {
        if (!enabled) return false;
        try {
            return (boolean) hasAccountMethod.invoke(economy, player);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        try {
            Object result = withdrawPlayerMethod.invoke(economy, player, amount);
            Method success = result.getClass().getMethod("transactionSuccess");
            return (boolean) success.invoke(result);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        try {
            Object result = depositPlayerMethod.invoke(economy, player, amount);
            Method success = result.getClass().getMethod("transactionSuccess");
            return (boolean) success.invoke(result);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canAfford(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        try {
            return (boolean) hasMethod.invoke(economy, player, amount);
        } catch (Exception e) {
            return false;
        }
    }

    public void shutdown() {
        // Vault doesn't require cleanup
    }
}
