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
    private final Set<UUID> trackedPlayers = new HashSet<>();
    private final Set<UUID> pendingHidePlayers = new HashSet<>();
    
    public PlayerListener() {
        this.plugin = QQCapture.getInstance();
    }
    
    public void addTrackedPlayer(Player player) {
        trackedPlayers.add(player.getUniqueId());
    }
    
    public void removeTrackedPlayer(Player player) {
        trackedPlayers.remove(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        for (CaptureSession session : plugin.getSessionManager().getActiveSessions()) {
            Template template = session.getTemplate();
            if (template.isBossBarEnabled() && template.isSendOnRejoin()) {
                if (template.isInAnyZone(player.getLocation())) {
                    trackedPlayers.add(player.getUniqueId());
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
        trackedPlayers.remove(player.getUniqueId());
        pendingHidePlayers.remove(player.getUniqueId());
        
        CaptureSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session != null) {
            plugin.getSessionManager().removePlayerFromSession(session.getSessionId(), player);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!trackedPlayers.contains(uuid)) {
            return;
        }
        
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
                plugin.getLogger().info("Player " + player.getName() + 
                    " - nowInZone: " + nowInZone + 
                    ", isInSession: " + isInSession);
            }
            
            if (nowInZone && !isInSession) {
                // ВОШЕЛ В ЗОНУ
                pendingHidePlayers.remove(uuid);
                onPlayerEnterZone(player, session);
            } else if (nowInZone && isInSession) {
                // УЖЕ В ЗОНЕ
                pendingHidePlayers.remove(uuid);
                if (template.isBossBarEnabled()) {
                    plugin.getBossBarManager().showBossBar(player, session);
                }
            } else if (!nowInZone && isInSession) {
                // ВЫШЕЛ ИЗ ЗОНЫ — НАЧИНАЕМ ОТСЧЕТ 5 СЕКУНД
                onPlayerLeaveZone(player, session);
            }
        }
    }
    
    private void onPlayerEnterZone(Player player, CaptureSession session) {
        if (session == null) return;
        if (session.getPlayers().containsKey(player.getUniqueId())) return;
        
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
        
        trackedPlayers.add(player.getUniqueId());
        pendingHidePlayers.remove(player.getUniqueId());
        
        plugin.getSessionManager().addPlayerToSession(session.getSessionId(), player);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + " added to session " + session.getSessionId());
        }
    }
    
    private void onPlayerLeaveZone(Player player, CaptureSession session) {
        if (session == null) return;
        if (!session.getPlayers().containsKey(player.getUniqueId())) return;
        
        UUID uuid = player.getUniqueId();
        
        // Если уже в очереди на скрытие — не дублируем
        if (pendingHidePlayers.contains(uuid)) {
            return;
        }
        
        // Проверяем, есть ли игрок в других сессиях
        boolean inOtherSession = false;
        for (CaptureSession s : plugin.getSessionManager().getActiveSessions()) {
            if (s.getSessionId().equals(session.getSessionId())) continue;
            if (s.getPlayers().containsKey(player.getUniqueId())) {
                inOtherSession = true;
                break;
            }
        }
        
        // Если есть в другой сессии — не скрываем
        if (inOtherSession) {
            return;
        }
        
        // Добавляем в очередь на скрытие с задержкой 5 секунд
        pendingHidePlayers.add(uuid);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player " + player.getName() + 
                " left zone, bossbar will hide in 5 seconds");
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Проверяем, не зашел ли игрок обратно за это время
            if (!pendingHidePlayers.contains(uuid)) {
                return;
            }
            
            // Проверяем, не в сессии ли игрок
            if (session.getPlayers().containsKey(player.getUniqueId())) {
                pendingHidePlayers.remove(uuid);
                return;
            }
            
            // Проверяем другие сессии
            boolean inOtherSessionNow = false;
            for (CaptureSession s : plugin.getSessionManager().getActiveSessions()) {
                if (s.getPlayers().containsKey(player.getUniqueId())) {
                    inOtherSessionNow = true;
                    break;
                }
            }
            
            if (inOtherSessionNow) {
                pendingHidePlayers.remove(uuid);
                return;
            }
            
            // Скрываем боссбар
            plugin.getBossBarManager().hideBossBar(player, session);
            trackedPlayers.remove(uuid);
            pendingHidePlayers.remove(uuid);
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("BossBar hidden for player " + player.getName() + 
                    " after 5s delay");
            }
        }, 100L); // 5 секунд = 100 тиков
    }
}
