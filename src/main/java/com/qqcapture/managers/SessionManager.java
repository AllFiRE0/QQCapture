package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.Template;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final QQCapture plugin;
    private final Map<String, CaptureSession> activeSessions;
    private final Map<UUID, String> playerSessions;
    private final Map<String, BukkitRunnable> sessionTasks;
    
    public SessionManager(QQCapture plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.playerSessions = new ConcurrentHashMap<>();
        this.sessionTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * Запуск сессии по имени шаблона (стандартный способ)
     */
    public boolean startSession(String templateName, int points, boolean silent, Player starter) {
        Template template = plugin.getConfigManager().getTemplate(templateName);
        if (template == null) {
            return false;
        }
    
        // ===== ПРОВЕРЯЕМ, ЕСТЬ ЛИ УЖЕ АКТИВНАЯ СЕССИЯ ЭТОГО ШАБЛОНА =====
        for (CaptureSession s : activeSessions.values()) {
            if (s.getTemplate().getName().equalsIgnoreCase(templateName)) {
                if (!silent && starter != null) {
                    starter.sendMessage(ColorUtils.colorize("&cСессия для шаблона '" + templateName + "' уже запущена!"));
                }
                return false;
            }
        }
 
        String sessionId = generateSessionId(templateName);
        CaptureSession session = new CaptureSession(sessionId, template, points, silent, starter);
    
        activeSessions.put(sessionId, session);
        startSessionTask(sessionId);
    
        plugin.getLogger().info("Session started: " + sessionId + " by " + starter.getName());
        return true;
    }
    
    /**
     * Запуск сессии с готовым шаблоном (для переопределенных параметров из команды)
     */
    public boolean startSession(Template template, boolean silent, Player starter) {
        if (template == null) {
            return false;
        }
    
        String templateName = template.getName();
    
        // ===== ПРОВЕРЯЕМ, ЕСТЬ ЛИ УЖЕ АКТИВНАЯ СЕССИЯ ЭТОГО ШАБЛОНА =====
        for (CaptureSession s : activeSessions.values()) {
            if (s.getTemplate().getName().equalsIgnoreCase(templateName)) {
                if (!silent && starter != null) {
                    starter.sendMessage(ColorUtils.colorize("&cСессия для шаблона '" + templateName + "' уже запущена!"));
                }
                return false;
            }
        }
    
        String sessionId = generateSessionId(template.getName());
        CaptureSession session = new CaptureSession(sessionId, template, template.getNeedAmount(), silent, starter);
    
        activeSessions.put(sessionId, session);
        startSessionTask(sessionId);
    
        plugin.getLogger().info("Session started: " + sessionId + " by " + starter.getName());
        return true;
    }
    
    /**
     * Поиск существующей сессии по имени шаблона
     */
    private CaptureSession findExistingSession(String templateName) {
        for (CaptureSession s : activeSessions.values()) {
            String name = s.getTemplate().getName();
            if (name.endsWith("_override")) {
                name = name.substring(0, name.length() - 9);
            }
            if (name.equalsIgnoreCase(templateName)) {
                return s;
            }
        }
        return null;
    }
    
    public void stopSession(String sessionId) {
        CaptureSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.stop();
            activeSessions.remove(sessionId);
            
            BukkitRunnable task = sessionTasks.remove(sessionId);
            if (task != null) {
                task.cancel();
            }
            
            playerSessions.entrySet().removeIf(entry -> entry.getValue().equals(sessionId));
            plugin.getBossBarManager().removeBossBar(sessionId);
        }
    }
    
    public void stopAllSessions() {
        for (String sessionId : new ArrayList<>(activeSessions.keySet())) {
            stopSession(sessionId);
        }
    }
    
    private void startSessionTask(String sessionId) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                CaptureSession session = activeSessions.get(sessionId);
                if (session == null || session.isStopped()) {
                    this.cancel();
                    sessionTasks.remove(sessionId);
                    return;
                }
                
                session.update();
                
                if (session.isComplete()) {
                    plugin.getSessionManager().stopSession(sessionId);
                }
            }
        };
        
        task.runTaskTimer(plugin, 0L, 1L);
        sessionTasks.put(sessionId, task);
    }
    
    private String generateSessionId(String templateName) {
        return templateName + "_" + System.currentTimeMillis();
    }
    
    public void addPlayerToSession(String sessionId, Player player) {
        CaptureSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.addPlayer(player);
            playerSessions.put(player.getUniqueId(), sessionId);
        }
    }
    
    public void removePlayerFromSession(String sessionId, Player player) {
        CaptureSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.removePlayer(player);
            playerSessions.remove(player.getUniqueId());
        }
    }
    
    public CaptureSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    public CaptureSession getPlayerSession(Player player) {
        String sessionId = playerSessions.get(player.getUniqueId());
        return sessionId != null ? activeSessions.get(sessionId) : null;
    }
    
    public boolean isPlayerInSession(Player player) {
        return playerSessions.containsKey(player.getUniqueId());
    }
    
    public List<CaptureSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    public Map<String, CaptureSession> getActiveSessionsMap() {
        return activeSessions;
    }
}
