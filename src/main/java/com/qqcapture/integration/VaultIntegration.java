package com.qqcapture.integration;

import com.qqcapture.QQCapture;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultIntegration {
    private final QQCapture plugin;
    private Economy economy;
    private boolean enabled;
    
    public VaultIntegration(QQCapture plugin) {
        this.plugin = plugin;
        this.enabled = false;
        setupEconomy();
    }
    
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        
        economy = rsp.getProvider();
        enabled = true;
        plugin.getLogger().info("Vault economy integrated!");
    }
    
    public double getBalance(Player player) {
        if (!enabled || economy == null || player == null) {
            return 0.0;
        }
        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get balance for " + player.getName());
            return 0.0;
        }
    }
    
    public String getFormattedBalance(Player player) {
        if (!enabled || economy == null || player == null) {
            return "0.00";
        }
        try {
            return economy.format(economy.getBalance(player));
        } catch (Exception e) {
            return "0.00";
        }
    }
    
    public boolean withdraw(Player player, double amount) {
        if (!enabled || economy == null || player == null) {
            return false;
        }
        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to withdraw from " + player.getName());
            return false;
        }
    }
    
    public boolean deposit(Player player, double amount) {
        if (!enabled || economy == null || player == null) {
            return false;
        }
        try {
            return economy.depositPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deposit to " + player.getName());
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public Economy getEconomy() {
        return economy;
    }
}
