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

public class PlayerListener implements Listener {
    private final QQCapture plugin;
    
    public PlayerListener() {
        this.plugin = QQCapture.getInstance();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        for (CaptureSession session : plugin.getSessionManager().getActiveSessions()) {
            Template template = session.getTemplate();
            if (template.isBossBarEnabled() && template.isSendOnRejoin()) {
                if (template.isInAnyZone(player.getLocation())) {
                    plugin.getBossBarManager().showBossBar(player, session);
                    if (!session.getPlayers().containsKey(player.getUniqueId())) {
                        plugin.getSessionManager().addPlayerToSession(session.getSessionId(), player);
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
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
        
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        for (CaptureSession session : plugin.getSessionManager().getActiveSessions()) {
            if (session.isStopped() || session.isComplete()) {
                continue;
            }
            
            Template template = session.getTemplate();
            boolean nowInZone = template.isInAnyZone(player.getLocation());
            boolean isInSession = session.getPlayers().containsKey(player.getUniqueId());
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + " - nowInZone: " + nowInZone + ", isInSession: " + isInSession);
            }
            
            if (nowInZone && !isInSession) {
                onPlayerEnterZone(player, session);
            } else if (nowInZone && isInSession) {
                if (template.isBossBarEnabled()) {
                    plugin.getBossBarManager().showBossBar(player, session);
                }
            } else if (!nowInZone && isInSession) {
                onPlayerLeaveZone(player, session);
            }
        }
    }
    
    private void onPlayerEnterZone(Player player, CaptureSession session) {
        if (session == null) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("Session is null in onPlayerEnterZone!");
            }
            return;
        }
        
        if (session.getPlayers().containsKey(player.getUniqueId())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + " already in session " + session.getSessionId());
            }
            return;
        }
        
        Template template = session.getTemplate();
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " entered zone for session " + session.getSessionId());
        }
        
        if (!template.getPermission().isEmpty() && !player.hasPermission(template.getPermission())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + " doesn't have permission: " + template.getPermission());
            }
            return;
        }
        
        if (!plugin.getConditionManager().checkPlayerConditions(player, template)) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + " doesn't meet conditions");
            }
            return;
        }
        
        plugin.getSessionManager().addPlayerToSession(session.getSessionId(), player);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " added to session " + session.getSessionId());
        }
    }
    
    private void onPlayerLeaveZone(Player player, CaptureSession session) {
        if (session == null) {
            return;
        }
        
        if (!session.getPlayers().containsKey(player.getUniqueId())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + " not in session " + session.getSessionId());
            }
            return;
        }
        
        plugin.getSessionManager().removePlayerFromSession(session.getSessionId(), player);
        plugin.getBossBarManager().hideBossBar(player, session);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " left zone for session " + session.getSessionId());
        }
    }
}
