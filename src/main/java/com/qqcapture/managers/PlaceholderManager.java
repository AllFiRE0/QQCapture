package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
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
        // Player-specific placeholders
        if (placeholder.startsWith("player_")) {
            return parsePlayerPlaceholder(player, placeholder);
        }
        
        // QQCapture specific placeholders
        if (placeholder.startsWith("qqcapture_")) {
            return parseQQCapturePlaceholder(player, placeholder);
        }
        
        // Default placeholders
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
        
        // Try PlaceholderAPI
        if (plugin.getPlaceholderAPIHook() != null && player != null) {
            return plugin.getPlaceholderAPIHook().parsePlaceholders(player, "%" + placeholder + "%");
        }
        
        return "%" + placeholder + "%";
    }
    
    private String parsePlayerPlaceholder(Player player, String placeholder) {
        if (placeholder.startsWith("player_")) {
            String key = placeholder.substring(7);
            
            // Player data from Vault
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
            
            // Player data from active session
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
        // Format: qqcapture_templateName_property_fallback
        // Убираем префикс "qqcapture_"
        String withoutPrefix = placeholder.substring("qqcapture_".length());
        
        // Список возможных свойств (property)
        String[] properties = {"_current", "_max", "_progress", "_players", "_time", "_top_", "_participants"};
        
        String templateName = null;
        String property = null;
        String fallback = "";
        
        // Ищем первое совпадение свойства
        for (String prop : properties) {
            int index = withoutPrefix.indexOf(prop);
            if (index > 0) {
                templateName = withoutPrefix.substring(0, index);
                property = withoutPrefix.substring(index + 1); // убираем первый _
                break;
            }
        }
        
        // Если не нашли свойство - возвращаем как есть
        if (templateName == null || property == null) {
            return placeholder;
        }
        
        // Проверяем наличие fallback в property
        int fallbackIndex = property.indexOf("_&");
        if (fallbackIndex > 0) {
            fallback = property.substring(fallbackIndex + 2);
            property = property.substring(0, fallbackIndex);
        }
        
        // Ищем сессию с этим шаблоном
        // Используем финальную переменную для лямбды
        final String finalTemplateName = templateName;
        CaptureSession session = plugin.getSessionManager().getActiveSessionsMap()
            .values().stream()
            .filter(s -> s.getTemplate().getName().equalsIgnoreCase(finalTemplateName))
            .findFirst()
            .orElse(null);
        
        if (session == null) {
            return fallback.isEmpty() ? "0" : fallback;
        }

        // Обработка top_
        if (property.startsWith("top_")) {
            return parseTopPlaceholder(session, property, fallback);
        }
        
        // Обработка свойства
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
    
    private String parseTopPlaceholder(CaptureSession session, String property, String fallback) {
        // Формат: top_X_name или top_X_value или top_X_points
        // Убираем "top_"
        String withoutTop = property.substring("top_".length());
        
        // Ищем позицию и тип
        String[] parts = withoutTop.split("_", 2);
        if (parts.length < 2) {
            return fallback.isEmpty() ? "0" : fallback;
        }
        
        try {
            int position = Integer.parseInt(parts[0]);
            String type = parts[1];
            
            // Get top players
            List<Map.Entry<UUID, PlayerData>> sorted = session.getPlayers().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getContribution(), e1.getValue().getContribution()))
                .toList();
            
            if (position > sorted.size()) {
                return fallback.isEmpty() ? "0" : fallback;
            }
            
            Map.Entry<UUID, PlayerData> entry = sorted.get(position - 1);
            Player topPlayer = org.bukkit.Bukkit.getPlayer(entry.getKey());
            
            if (topPlayer == null) {
                return fallback.isEmpty() ? "0" : fallback;
            }
            
            if (type.equals("name")) {
                return topPlayer.getName();
            } else if (type.equals("value") || type.equals("points")) {
                return String.valueOf(entry.getValue().getContribution());
            }
            
        } catch (NumberFormatException e) {
            // Invalid position
        }
        
        return fallback.isEmpty() ? "0" : fallback;
    }
    
    private String parseParticipantsPlaceholder(CaptureSession session, String property, String fallback) {
        // Формат: participants_<separator>
        String separator = property.substring("participants".length());
        
        // Если separator начинается с "_", убираем его
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
