package com.qqcapture.integration;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QQCapturePlaceholders extends PlaceholderExpansion {
    
    private final QQCapture plugin;

    public QQCapturePlaceholders(QQCapture plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "qqcapture";
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public @Nullable List<String> getPlaceholders() {
        List<String> placeholders = new ArrayList<>();
        
        placeholders.add("%qqcapture_current%");
        placeholders.add("%qqcapture_max%");
        placeholders.add("%qqcapture_progress%");
        placeholders.add("%qqcapture_players%");
        placeholders.add("%qqcapture_template%");
        placeholders.add("%qqcapture_time%");
        
        placeholders.add("%qqcapture_session_contribution%");
        placeholders.add("%qqcapture_session_inzone%");
        placeholders.add("%qqcapture_session_ticks%");
        
        for (int i = 1; i <= 10; i++) {
            placeholders.add("%qqcapture_top_" + i + "_name%");
            placeholders.add("%qqcapture_top_" + i + "_value%");
            placeholders.add("%qqcapture_top_" + i + "_points%");
            
            placeholders.add("%qqcapture_top_" + i + "_name_&6Нет игрока%");
            placeholders.add("%qqcapture_top_" + i + "_value_&c0%");
            placeholders.add("%qqcapture_top_" + i + "_points_&c0%");
        }
        
        placeholders.add("%qqcapture_participants_ %");
        placeholders.add("%qqcapture_participants_,%");
        placeholders.add("%qqcapture_participants_.%");
        placeholders.add("%qqcapture_participants_&e, &f%");
        
        placeholders.add("%qqcapture_current_&c0%");
        placeholders.add("%qqcapture_players_&70%");
        placeholders.add("%qqcapture_progress_&c0.0%");
        
        for (String templateName : plugin.getConfigManager().getTemplateNames()) {
            placeholders.add("%qqcapture_" + templateName + "_current%");
            placeholders.add("%qqcapture_" + templateName + "_max%");
            placeholders.add("%qqcapture_" + templateName + "_progress%");
            placeholders.add("%qqcapture_" + templateName + "_players%");
            placeholders.add("%qqcapture_" + templateName + "_time%");
            
            for (int i = 1; i <= 10; i++) {
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_name%");
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_value%");
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_points%");
                
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_name_&6Нет игрока%");
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_value_&c0%");
            }
            
            placeholders.add("%qqcapture_" + templateName + "_participants_ %");
            placeholders.add("%qqcapture_" + templateName + "_participants_,%");
            placeholders.add("%qqcapture_" + templateName + "_participants_.%");
            
            placeholders.add("%qqcapture_" + templateName + "_current_&c0%");
            placeholders.add("%qqcapture_" + templateName + "_players_&70%");
        }
        
        return placeholders;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }
        
        String withoutPrefix = identifier;
        if (identifier.startsWith("qqcapture_")) {
            withoutPrefix = identifier.substring("qqcapture_".length());
        }
        
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
        
        if (templateName == null) {
            String[] generalProps = {"current", "max", "progress", "players", "template", "time", "session_", "top_", "participants"};
            for (String prop : generalProps) {
                if (withoutPrefix.startsWith(prop)) {
                    property = withoutPrefix;
                    break;
                }
            }
        }
        
        if (property == null) {
            return handleFallback(withoutPrefix);
        }
        
        int fallbackIndex = property.indexOf("_&");
        if (fallbackIndex > 0) {
            fallback = property.substring(fallbackIndex + 2);
            property = property.substring(0, fallbackIndex);
        }
        
        CaptureSession targetSession = null;
        
        if (templateName != null) {
            for (CaptureSession s : plugin.getSessionManager().getActiveSessions()) {
                if (s.getTemplate().getName().equalsIgnoreCase(templateName)) {
                    targetSession = s;
                    break;
                }
            }
        } else {
            targetSession = plugin.getSessionManager().getPlayerSession(player);
        }
        
        if (targetSession == null) {
            return handleFallback(property);
        }
        
        if (property.startsWith("session_")) {
            String[] parts = property.split("_", 2);
            if (parts.length == 2) {
                String key = parts[1];
                PlayerData data = targetSession.getPlayers().get(player.getUniqueId());
                
                if (data == null) {
                    return "";
                }
                
                switch (key) {
                    case "contribution":
                        return String.valueOf(data.getContribution());
                    case "inzone":
                        return String.valueOf(data.isInZone());
                    case "ticks":
                        return String.valueOf(data.getTicksInZone());
                }
            }
        }
        
        switch (property) {
            case "current":
                return String.valueOf(targetSession.getCurrentPoints());
            case "max":
                return String.valueOf(targetSession.getTargetPoints());
            case "progress":
                return String.format("%.1f", (double) targetSession.getCurrentPoints() / targetSession.getTargetPoints() * 100);
            case "players":
                return String.valueOf(targetSession.getPlayers().size());
            case "template":
                return targetSession.getTemplate().getName();
            case "time":
                long elapsed = System.currentTimeMillis() - targetSession.getStartTime();
                return formatTime(elapsed, targetSession.getTemplate().getTimerFormat());
        }
        
        if (property.startsWith("top_")) {
            return parseTopPlaceholder(targetSession, property, fallback);
        }
        
        if (property.startsWith("participants")) {
            return parseParticipantsPlaceholder(targetSession, property, fallback);
        }
        
        return handleFallback(property);
    }
    
    private String handleFallback(String identifier) {
        int fallbackIndex = identifier.indexOf("_&");
        if (fallbackIndex > 0) {
            return identifier.substring(fallbackIndex + 2);
        }
        return "";
    }
    
    private String parseTopPlaceholder(CaptureSession session, String property, String fallback) {
        String withoutTop = property.substring("top_".length());
        
        String[] parts = withoutTop.split("_", 2);
        if (parts.length < 2) {
            return fallback.isEmpty() ? "0" : fallback;
        }
        
        try {
            int position = Integer.parseInt(parts[0]);
            String type = parts[1];
            
            List<Map.Entry<UUID, PlayerData>> sorted = session.getPlayers().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getContribution(), e1.getValue().getContribution()))
                .toList();
            
            if (position > sorted.size()) {
                return fallback.isEmpty() ? "0" : fallback;
            }
            
            Map.Entry<UUID, PlayerData> entry = sorted.get(position - 1);
            OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(entry.getKey());
            
            if (type.equals("name")) {
                return topPlayer.getName() != null ? topPlayer.getName() : fallback;
            } else if (type.equals("value") || type.equals("points")) {
                return String.valueOf(entry.getValue().getContribution());
            }
            
        } catch (NumberFormatException e) {
            // Invalid position
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
