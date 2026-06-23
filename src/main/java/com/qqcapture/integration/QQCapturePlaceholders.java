package com.qqcapture.integration;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
import com.qqcapture.managers.TopStorageManager;
import com.qqcapture.utils.ColorUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
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
        
        placeholders.add("%qqcapture_player_template%");
        placeholders.add("%qqcapture_player_template_Не в ивенте%");
        
        for (int i = 1; i <= 10; i++) {
            placeholders.add("%qqcapture_top_" + i + "_name%");
            placeholders.add("%qqcapture_top_" + i + "_value%");
            placeholders.add("%qqcapture_top_" + i + "_points%");
            
            placeholders.add("%qqcapture_top_" + i + "_name_Нет игрока%");
            placeholders.add("%qqcapture_top_" + i + "_value_0%");
            placeholders.add("%qqcapture_top_" + i + "_points_0%");
        }
        
        placeholders.add("%qqcapture_participants_ %");
        placeholders.add("%qqcapture_participants_,%");
        placeholders.add("%qqcapture_participants_.%");
        placeholders.add("%qqcapture_participants_, %");
        
        placeholders.add("%qqcapture_current_0%");
        placeholders.add("%qqcapture_players_0%");
        placeholders.add("%qqcapture_progress_0.0%");
        
        for (String templateName : plugin.getConfigManager().getTemplateNames()) {
            placeholders.add("%qqcapture_" + templateName + "_current%");
            placeholders.add("%qqcapture_" + templateName + "_max%");
            placeholders.add("%qqcapture_" + templateName + "_progress%");
            placeholders.add("%qqcapture_" + templateName + "_players%");
            placeholders.add("%qqcapture_" + templateName + "_time%");
            placeholders.add("%qqcapture_" + templateName + "_template%");
            
            placeholders.add("%qqcapture_" + templateName + "_active%");
            placeholders.add("%qqcapture_" + templateName + "_active_нет%");
            placeholders.add("%qqcapture_" + templateName + "_active_&cНЕТ%");
            
            for (int i = 1; i <= 10; i++) {
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_name%");
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_value%");
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_points%");
                
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_name_Нет игрока%");
                placeholders.add("%qqcapture_" + templateName + "_top_" + i + "_value_0%");
            }
            
            placeholders.add("%qqcapture_" + templateName + "_participants_ %");
            placeholders.add("%qqcapture_" + templateName + "_participants_,%");
            placeholders.add("%qqcapture_" + templateName + "_participants_.%");
            
            placeholders.add("%qqcapture_" + templateName + "_current_0%");
            placeholders.add("%qqcapture_" + templateName + "_players_0%");
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
        
        // ===== ИЩЕМ FALLBACK В КОНЦЕ =====
        String fallback = "";
        String mainPart = withoutPrefix;
        int lastUnderscore = withoutPrefix.lastIndexOf("_");
        if (lastUnderscore > 0) {
            fallback = withoutPrefix.substring(lastUnderscore + 1);
            mainPart = withoutPrefix.substring(0, lastUnderscore);
        }
        
        // ===== player_template =====
        if (mainPart.startsWith("player_template")) {
            String fb = "Не в ивенте";
            if (!fallback.isEmpty()) {
                fb = fallback;
            }
            CaptureSession session = plugin.getSessionManager().getPlayerSession(player);
            if (session != null) {
                return session.getTemplate().getName();
            }
            return ColorUtils.colorize(fb);
        }
        
        // ===== active =====
        if (mainPart.endsWith("_active")) {
            String templateName = mainPart.substring(0, mainPart.length() - 7);
            String fb = "no";
            if (!fallback.isEmpty()) {
                fb = fallback;
            }
            
            for (CaptureSession s : plugin.getSessionManager().getActiveSessions()) {
                if (s.getTemplate().getName().equalsIgnoreCase(templateName)) {
                    return "yes";
                }
            }
            return ColorUtils.colorize(fb);
        }
        
        // ===== template без шаблона =====
        if (mainPart.equals("template")) {
            CaptureSession session = plugin.getSessionManager().getPlayerSession(player);
            if (session != null) {
                return session.getTemplate().getName();
            }
            return ColorUtils.colorize("Неизвестно");
        }
        
        // ===== ОСНОВНЫЕ ЗАПОЛНИТЕЛИ =====
        String[] properties = {"_current", "_max", "_progress", "_players", "_time", "_top_", "_participants", "_template"};
        
        String templateName = null;
        String property = null;
        
        for (String prop : properties) {
            int index = mainPart.indexOf(prop);
            if (index > 0) {
                templateName = mainPart.substring(0, index);
                property = mainPart.substring(index + 1);
                break;
            }
        }
        
        if (templateName == null) {
            String[] generalProps = {"current", "max", "progress", "players", "template", "time", "session_", "top_", "participants"};
            for (String prop : generalProps) {
                if (mainPart.startsWith(prop)) {
                    property = mainPart;
                    break;
                }
            }
        }
        
        if (property == null) {
            return fallback.isEmpty() ? "" : ColorUtils.colorize(fallback);
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
        
        // ===== template =====
        if (property.equals("template") && templateName != null) {
            return templateName;
        }
        
        if (property.startsWith("top_")) {
            return parseTopPlaceholder(templateName, targetSession, property, fallback);
        }
        
        if (targetSession == null && templateName != null) {
            CaptureSession.SessionSnapshot snapshot = CaptureSession.getCompletedSession(templateName);
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
                        return formatTime(snapshot.getDuration(), "mm:ss");
                    case "participants":
                        return parseParticipantsPlaceholderFromSnapshot(snapshot, property, fallback);
                    default:
                        return fallback.isEmpty() ? "0" : ColorUtils.colorize(fallback);
                }
            }
            return fallback.isEmpty() ? "0" : ColorUtils.colorize(fallback);
        }
        
        if (targetSession == null) {
            return fallback.isEmpty() ? "0" : ColorUtils.colorize(fallback);
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
            case "participants":
                return parseParticipantsPlaceholder(targetSession, property, fallback);
            default:
                return fallback.isEmpty() ? "0" : ColorUtils.colorize(fallback);
        }
    }
    
    private String parseTopPlaceholder(String templateName, CaptureSession session, String property, String fallback) {
        TopStorageManager storage = plugin.getTopStorageManager();
        List<TopStorageManager.TopEntry> entries = storage.getTop(templateName);
        
        String[] parts = property.split("_");
        if (parts.length < 3) {
            return fallback.isEmpty() ? "0" : ColorUtils.colorize(fallback);
        }
        
        try {
            int position = Integer.parseInt(parts[1]);
            String type = parts[2];
            
            if (!entries.isEmpty() && position <= entries.size()) {
                TopStorageManager.TopEntry entry = entries.get(position - 1);
                if (type.equals("name")) {
                    return entry.getPlayerName() != null ? entry.getPlayerName() : fallback;
                } else if (type.equals("value") || type.equals("points")) {
                    return String.valueOf(entry.getPoints());
                }
            }
            
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
        
        return fallback.isEmpty() ? "0" : ColorUtils.colorize(fallback);
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
            return fallback.isEmpty() ? "" : ColorUtils.colorize(fallback);
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
            return fallback.isEmpty() ? "" : ColorUtils.colorize(fallback);
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