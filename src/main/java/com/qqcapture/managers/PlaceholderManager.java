package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderManager {
    private final QQCapture plugin;
    private final Pattern placeholderPattern;
    
    public PlaceholderManager(QQCapture plugin) {
        this.plugin = plugin;
        this.placeholderPattern = Pattern.compile("%([^%]+)%");
    }
    
    public String parsePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        Matcher matcher = placeholderPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = parsePlaceholder(player, placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String parsePlaceholder(Player player, String placeholder) {
        if (placeholder.startsWith("player_")) {
            return parsePlayerPlaceholder(player, placeholder);
        }
        
        if (placeholder.startsWith("qqcapture_")) {
            return parseQQCapturePlaceholder(player, placeholder);
        }
        
        if (player != null) {
            switch (placeholder) {
                case "player_name": return player.getName();
                case "player_displayname": return player.getDisplayName();
                case "player_uuid": return player.getUniqueId().toString();
                case "player_level": return String.valueOf(player.getLevel());
                case "player_health": return String.valueOf(player.getHealth());
                case "player_maxhealth": return String.valueOf(player.getMaxHealth());
                case "player_x": return String.valueOf(player.getLocation().getBlockX());
                case "player_y": return String.valueOf(player.getLocation().getBlockY());
                case "player_z": return String.valueOf(player.getLocation().getBlockZ());
                case "player_world": return player.getWorld().getName();
            }
        }
        
        if (plugin.getPlaceholderAPIHook() != null && player != null) {
            return plugin.getPlaceholderAPIHook().parsePlaceholders(player, "%" + placeholder + "%");
        }
        
        return "%" + placeholder + "%";
    }
    
    private String parsePlayerPlaceholder(Player player, String placeholder) {
        if (placeholder.startsWith("player_")) {
            String key = placeholder.substring(7);
            
            if (key.startsWith("vault_")) {
                if (plugin.getVaultIntegration() != null && player != null) {
                    String vaultKey = key.substring(6);
                    if (vaultKey.equals("balance")) {
                        return String.format("%.2f", plugin.getVaultIntegration().getBalance(player));
                    } else if (vaultKey.equals("balance_formatted")) {
                        return plugin.getVaultIntegration().getFormattedBalance(player);
                    }
                }
            }
            
            if (key.startsWith("session_")) {
                CaptureSession session = plugin.getSessionManager().getPlayerSession(player);
                if (session != null) {
                    String sessionKey = key.substring(8);
                    PlayerData data = session.getPlayers().get(player.getUniqueId());
                    if (data != null) {
                        if (sessionKey.equals("contribution")) {
                            return String.valueOf(data.getContribution());
                        } else if (sessionKey.equals("inzone")) {
                            return String.valueOf(data.isInZone());
                        }
                    }
                }
            }
        }
        return placeholder;
    }
    
    private String parseQQCapturePlaceholder(Player player, String placeholder) {
        String withoutPrefix = placeholder.substring("qqcapture_".length());
        
        String[] properties = {"_current", "_max", "_progress", "_players", "_time", "_top_", "_participants"};
        
        String templateName = null;
        String property = null;
        String fallback = "";
        
        for (String prop : properties) {
            int index = withoutPrefix.indexOf(prop);
            if (index > 0) {
                templateName = withoutPrefix.substring(0, index);
                property = withoutPrefix.substring(index + 1);
                break;
            }
        }
        
        if (templateName == null || property == null) {
            return placeholder;
        }
        
        int fallbackIndex = property.indexOf("_&");
        if (fallbackIndex > 0) {
            fallback = property.substring(fallbackIndex + 2);
            property = property.substring(0, fallbackIndex);
        }
        
        final String finalTemplateName = templateName;
        CaptureSession session = plugin.getSessionManager().getActiveSessionsMap()
            .values().stream()
            .filter(s -> s.getTemplate().getName().equalsIgnoreCase(finalTemplateName))
            .findFirst()
            .orElse(null);
        
        if (property.startsWith("top_")) {
            return parseTopPlaceholder(finalTemplateName, session, property, fallback);
        }
        
        if (session == null) {
            CaptureSession.SessionSnapshot snapshot = CaptureSession.getCompletedSession(finalTemplateName);
            if (snapshot != null) {
                switch (property) {
                    case "current":
                        return String.valueOf(snapshot.getTotalPoints());
                    case "max":
                        return String.valueOf(snapshot.getTargetPoints());
                    case "progress":
                        return String.format("%.1f", (double) snapshot.getTotalPoints() / snapshot.getTargetPoints() * 100);
                    case "players":
                        return String.valueOf(snapshot.getContributions().size());
                    case "time":
                        long elapsed = System.currentTimeMillis() - snapshot.getEndTime();
                        return formatTime(elapsed, "mm:ss");
                    case "participants":
                        return parseParticipantsPlaceholderFromSnapshot(snapshot, property, fallback);
                    default:
                        return fallback.isEmpty() ? "0" : fallback;
                }
            }
            return fallback.isEmpty() ? "0" : fallback;
        }
        
        switch (property) {
            case "current":
                return String.valueOf(session.getCurrentPoints());
            case "max":
                return String.valueOf(session.getTargetPoints());
            case "progress":
                return String.format("%.1f", (double) session.getCurrentPoints() / session.getTargetPoints() * 100);
            case "players":
                return String.valueOf(session.getPlayers().size());
            case "time":
                long elapsed = System.currentTimeMillis() - session.getStartTime();
                return formatTime(elapsed, session.getTemplate().getTimerFormat());
            case "participants":
                return parseParticipantsPlaceholder(session, property, fallback);
            default:
                return fallback.isEmpty() ? "0" : fallback;
        }
    }
    
    private String parseTopPlaceholder(String templateName, CaptureSession session, String property, String fallback) {
        // ===== ПРОВЕРЯЕМ ХРАНИЛИЩЕ =====
        TopStorageManager storage = plugin.getTopStorageManager();
        List<TopStorageManager.TopEntry> entries = storage.getTop(templateName);
        
        String[] parts = property.split("_");
        if (parts.length < 3) {
            return fallback.isEmpty() ? "0" : fallback;
        }
        
        try {
            int position = Integer.parseInt(parts[1]);
            String type = parts[2];
            
            // === СНАЧАЛА ИЗ ФАЙЛА ===
            if (!entries.isEmpty() && position <= entries.size()) {
                TopStorageManager.TopEntry entry = entries.get(position - 1);
                if (type.equals("name")) {
                    return entry.getPlayerName() != null ? entry.getPlayerName() : fallback;
                } else if (type.equals("value") || type.equals("points")) {
                    return String.valueOf(entry.getPoints());
                }
            }
            
            // === ЕСЛИ НЕТ В ФАЙЛЕ — ИЗ АКТИВНОЙ СЕССИИ ===
            if (session != null && !session.getPlayers().isEmpty()) {
                List<Map.Entry<UUID, PlayerData>> sorted = session.getPlayers().entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue().getContribution(), e1.getValue().getContribution()))
                    .toList();
                
                if (position <= sorted.size()) {
                    Map.Entry<UUID, PlayerData> entry = sorted.get(position - 1);
                    if (type.equals("name")) {
                        OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(entry.getKey());
                        return topPlayer.getName() != null ? topPlayer.getName() : fallback;
                    } else if (type.equals("value") || type.equals("points")) {
                        return String.valueOf(entry.getValue().getContribution());
                    }
                }
            }
            
        } catch (NumberFormatException e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("Invalid top placeholder format: " + property);
            }
        }
        
        return fallback.isEmpty() ? "0" : fallback;
    }
    
    private String parseParticipantsPlaceholder(CaptureSession session, String property, String fallback) {
        String separator = property.substring("participants".length());
        
        if (separator.startsWith("_")) {
            separator = separator.substring(1);
        }
        
        if (separator.isEmpty() || separator.equals("_")) {
            separator = " ";
        }
        
        List<String> names = session.getPlayers().values().stream()
            .map(PlayerData::getPlayerName)
            .filter(name -> name != null && !name.isEmpty())
            .toList();
        
        if (names.isEmpty()) {
            return fallback.isEmpty() ? "" : fallback;
        }
        
        return String.join(separator, names);
    }
    
    private String parseParticipantsPlaceholderFromSnapshot(CaptureSession.SessionSnapshot snapshot, String property, String fallback) {
        String separator = property.substring("participants".length());
        
        if (separator.startsWith("_")) {
            separator = separator.substring(1);
        }
        
        if (separator.isEmpty() || separator.equals("_")) {
            separator = " ";
        }
        
        List<String> names = new ArrayList<>(snapshot.getPlayerNames().values());
        names.removeIf(name -> name == null || name.isEmpty());
        
        if (names.isEmpty()) {
            return fallback.isEmpty() ? "" : fallback;
        }
        
        return String.join(separator, names);
    }
    
    private String formatTime(long millis, String format) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        switch (format) {
            case "HH:mm:ss":
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            case "mm:ss":
                return String.format("%02d:%02d", minutes, seconds);
            case "HH:mm":
                return String.format("%02d:%02d", hours, minutes);
            case "ss":
                return String.format("%02d", seconds);
            default:
                return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
