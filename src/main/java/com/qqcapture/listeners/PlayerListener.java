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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final QQCapture plugin;
    
    // ===== ОПТИМИЗАЦИЯ: ТОЛЬКО ИГРОКИ В ЗОНЕ =====
    private final Set<UUID> trackedPlayers = new HashSet<>();
    
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
                    // Добавляем в список отслеживаемых
                    trackedPlayers.add(player.getUniqueId());
                    
                    // Показываем боссбар
                    plugin.getBossBarManager().showBossBar(player, session);
                    
                    // Добавляем в сессию, если ещё не добавлен
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
        
        // Удаляем из списка отслеживаемых
        trackedPlayers.remove(player.getUniqueId());
        
        CaptureSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session != null) {
            plugin.getSessionManager().removePlayerFromSession(session.getSessionId(), player);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // ===== 1. ПРОВЕРКА: ИГРОК В СПИСКЕ ОТСЛЕЖИВАЕМЫХ? =====
        if (!trackedPlayers.contains(uuid)) {
            return; // ← ИГРОК НЕ В ЗОНЕ — НЕ ПРОВЕРЯЕМ!
        }
        
        // ===== 2. ПРОВЕРКА: ПЕРЕШЕЛ ЛИ ИГРОК НА НОВЫЙ БЛОК? =====
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        // ===== 3. ПРОВЕРКА: АКТИВНЫЕ СЕССИИ =====
        for (CaptureSession session : plugin.getSessionManager().getActiveSessions()) {
            if (session.isStopped() || session.isComplete()) {
                continue;
            }
            
            Template template = session.getTemplate();
            boolean nowInZone = template.isInAnyZone(player.getLocation());
            boolean isInSession = session.getPlayers().containsKey(player.getUniqueId());
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + 
                    " - nowInZone: " + nowInZone + 
                    ", isInSession: " + isInSession);
            }
            
            if (nowInZone && !isInSession) {
                // Игрок ВОШЕЛ в зону
                onPlayerEnterZone(player, session);
            } else if (nowInZone && isInSession) {
                // Игрок УЖЕ в зоне — проверяем боссбар
                if (template.isBossBarEnabled()) {
                    plugin.getBossBarManager().showBossBar(player, session);
                }
            } else if (!nowInZone && isInSession) {
                // Игрок ВЫШЕЛ из зоны
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
        
        // Проверяем, не добавлен ли уже игрок
        if (session.getPlayers().containsKey(player.getUniqueId())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + 
                    " already in session " + session.getSessionId());
            }
            return;
        }
        
        Template template = session.getTemplate();
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + 
                " entered zone for session " + session.getSessionId());
        }
        
        // Проверка прав
        if (!template.getPermission().isEmpty() && !player.hasPermission(template.getPermission())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + 
                    " doesn't have permission: " + template.getPermission());
            }
            return;
        }
        
        // Проверка условий
        if (!plugin.getConditionManager().checkPlayerConditions(player, template)) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + 
                    " doesn't meet conditions");
            }
            return;
        }
        
        // ===== ДОБАВЛЯЕМ В СПИСОК ОТСЛЕЖИВАЕМЫХ =====
        trackedPlayers.add(player.getUniqueId());
        
        // Добавляем в сессию
        plugin.getSessionManager().addPlayerToSession(session.getSessionId(), player);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + 
                " added to session " + session.getSessionId());
        }
    }
    
    private void onPlayerLeaveZone(Player player, CaptureSession session) {
        if (session == null) {
            return;
        }
        
        // Проверяем, есть ли игрок в сессии
        if (!session.getPlayers().containsKey(player.getUniqueId())) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + 
                    " not in session " + session.getSessionId());
            }
            return;
        }
        
        // Удаляем из сессии
        plugin.getSessionManager().removePlayerFromSession(session.getSessionId(), player);
        
        // Скрываем боссбар
        plugin.getBossBarManager().hideBossBar(player, session);
        
        // ===== ПРОВЕРЯЕМ: ЕСТЬ ЛИ ИГРОК В ДРУГИХ СЕССИЯХ? =====
        boolean inOtherSession = false;
        for (CaptureSession s : plugin.getSessionManager().getActiveSessions()) {
            if (s.getSessionId().equals(session.getSessionId())) continue;
            if (s.getPlayers().containsKey(player.getUniqueId())) {
                inOtherSession = true;
                break;
            }
        }
        
        // ===== ЕСЛИ НЕТ — УДАЛЯЕМ ИЗ СПИСКА ОТСЛЕЖИВАЕМЫХ =====
        if (!inOtherSession) {
            trackedPlayers.remove(player.getUniqueId());
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player " + player.getName() + 
                    " removed from tracking list");
            }
        }
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + 
                " left zone for session " + session.getSessionId());
        }
    }
}
