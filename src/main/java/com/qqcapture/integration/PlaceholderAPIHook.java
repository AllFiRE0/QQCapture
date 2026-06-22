package com.qqcapture.integration;

import com.qqcapture.QQCapture;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderAPIHook {
    private final QQCapture plugin;
    private boolean enabled;
    
    public PlaceholderAPIHook(QQCapture plugin) {
        this.plugin = plugin;
        this.enabled = false;
        setupPlaceholderAPI();
    }
    
    private void setupPlaceholderAPI() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.enabled = true;
            plugin.getLogger().info("PlaceholderAPI integration enabled!");
            
            // Register custom placeholders
            new QQCapturePlaceholders(plugin).register();
        } else {
            plugin.getLogger().warning("PlaceholderAPI not found! Custom placeholders disabled.");
        }
    }
    
    public String parsePlaceholders(Player player, String text) {
        if (!enabled || player == null || text == null) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse PlaceholderAPI placeholders: " + text);
            return text;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
