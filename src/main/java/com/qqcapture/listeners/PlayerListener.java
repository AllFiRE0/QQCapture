package com.qqcapture.listeners;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.Template;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class PlayerListener implements Listener {
    private final QQCapture plugin;
    
    public PlayerListener() {
        this.plugin = QQCapture.getInstance();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
    
        // Check if player should see boss bars
        for (CaptureSession session : plugin.getSessionManager().getActiveSessions()) {
            Template template = session.getTemplate();
            // Проверяем: боссбар включен И нужно отправлять при перезаходе
            if (template.isBossBarEnabled() && template.isSendOnRejoin()) {
                // Check if player is in zone
                if (isPlayerInZone(player, session)) {
                    plugin.getBossBarManager().showBossBar(player, session);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Remove player from sessions
        CaptureSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session != null) {
            plugin.getSessionManager().removePlayerFromSession(session.getSessionId(), player);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Only check if position changed
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        // Check for zone entry/exit
        for (CaptureSession session : plugin.getSessionManager().getActiveSessions()) {
            if (session.isStopped() || session.isComplete()) {
                continue;
            }
            
            boolean wasInZone = isPlayerInZone(player, session);
            boolean nowInZone = isPlayerInZone(player, session);
            
            if (!wasInZone && nowInZone) {
                // Player entered zone
                onPlayerEnterZone(player, session);
            } else if (wasInZone && !nowInZone) {
                // Player left zone
                onPlayerLeaveZone(player, session);
            }
        }
    }
    
    private void onPlayerEnterZone(Player player, CaptureSession session) {
        Template template = session.getTemplate();
        
        // Check permission
        if (!template.getPermission().isEmpty() && !player.hasPermission(template.getPermission())) {
            return;
        }
        
        // Check player conditions
        if (!plugin.getConditionManager().checkPlayerConditions(player, template)) {
            return;
        }
        
        // Add player to session
        plugin.getSessionManager().addPlayerToSession(session.getSessionId(), player);
        
        // Show boss bar
        plugin.getBossBarManager().showBossBar(player, session);
        
        // Debug
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " entered zone for session " + session.getSessionId());
        }
    }
    
    private void onPlayerLeaveZone(Player player, CaptureSession session) {
        // Remove player from session
        plugin.getSessionManager().removePlayerFromSession(session.getSessionId(), player);
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBar(player, session);
        
        // Debug
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " left zone for session " + session.getSessionId());
        }
    }
    
    private boolean isPlayerInZone(Player player, CaptureSession session) {
        Template template = session.getTemplate();
        Location loc = player.getLocation();
        Location pos1 = template.getPos1();
        Location pos2 = template.getPos2();
        
        // Check WorldGuard region first
        if (!template.getRegionName().isEmpty()) {
            return plugin.getRegionManager().isPlayerInRegion(loc, template.getRegionName());
        }
        
        // Check cuboid area
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}
