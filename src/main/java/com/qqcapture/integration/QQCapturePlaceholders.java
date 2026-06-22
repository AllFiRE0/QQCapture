package com.qqcapture.integration;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
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
        
        // Основные заполнители
        placeholders.add("%qqcapture_current%");
        placeholders.add("%qqcapture_max%");
        placeholders.add("%qqcapture_progress%");
        placeholders.add("%qqcapture_players%");
        placeholders.add("%qqcapture_template%");
        placeholders.add("%qqcapture_time%");
        
        // Session заполнители
        placeholders.add("%qqcapture_session_contribution%");
        placeholders.add("%qqcapture_session_inzone%");
        placeholders.add("%qqcapture_session_ticks%");
        
        // Топ заполнители (1-10)
        for (int i = 1; i <= 10; i++) {
            placeholders.add("%qqcapture_top_" + i + "_name%");
            placeholders.add("%qqcapture_top_" + i + "_value%");
            placeholders.add("%qqcapture_top_" + i + "_points%");
            
            // С fallback
            placeholders.add("%qqcapture_top_" + i + "_name_&6Нет игрока%");
            placeholders.add("%qqcapture_top_" + i + "_value_&c0%");
            placeholders.add("%qqcapture_top_" + i + "_points_&c0%");
        }
        
        // Участники с разделителями
        placeholders.add("%qqcapture_participants_ %");      // пробел
        placeholders.add("%qqcapture_participants_,%");      // запятая
        placeholders.add("%qqcapture_participants_.%");      // точка
        placeholders.add("%qqcapture_participants_&e, &f%"); // цветной разделитель
        
        // Текущие с fallback
        placeholders.add("%qqcapture_current_&c0%");
        placeholders.add("%qqcapture_players_&70%");
        placeholders.add("%qqcapture_progress_&c0.0%");
        
        // Для каждого шаблона из конфига добавляем специфичные заполнители
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
                
                // С fallback
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
        
        // Полная поддержка всех заполнителей из getPlaceholders()
        // Формат: qqcapture_<параметр>
        // Например: qqcapture_current, qqcapture_top_1_name, qqcapture_zona_zahvata_current
        
        // Проверяем, есть ли активная сессия у игрока
        CaptureSession session = plugin.getSessionManager().getPlayerSession(player);
        
        // Проверяем, не содержит ли identifier имя шаблона
        // Формат: qqcapture_<templateName>_<параметр>
        String templateName = null;
        String actualIdentifier = identifier;
        
        // Проверяем все шаблоны
        for (String name : plugin.getConfigManager().getTemplateNames()) {
            if (identifier.startsWith(name + "_")) {
                templateName = name;
                actualIdentifier = identifier.substring(name.length() + 1);
                break;
            }
        }
        
        // Если указан конкретный шаблон, ищем его сессию
        CaptureSession targetSession = session;
        if (templateName != null) {
            // Ищем сессию с указанным шаблоном
            for (CaptureSession s : plugin.getSessionManager().getActiveSessions()) {
                if (s.getTemplate().getName().equalsIgnoreCase(templateName)) {
                    targetSession = s;
                    break;
                }
            }
        }
        
        // Если нет сессии, возвращаем fallback или пустоту
        if (targetSession == null) {
            return handleFallback(actualIdentifier);
        }
        
        // Обработка session_* заполнителей
        if (actualIdentifier.startsWith("session_")) {
            String[] parts = actualIdentifier.split("_", 2);
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
        
        // Основные заполнители
        switch (actualIdentifier) {
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
        
        // Топ заполнители
        if (actualIdentifier.startsWith("top_")) {
            return parseTopPlaceholder(targetSession, actualIdentifier);
        }
        
        // Участники
        if (actualIdentifier.startsWith("participants")) {
            return parseParticipantsPlaceholder(targetSession, actualIdentifier);
        }
        
        // Если ничего не подошло, пытаемся обработать fallback
        return handleFallback(actualIdentifier);
    }
    
    private String handleFallback(String identifier) {
        // Проверяем, есть ли fallback
        int fallbackIndex = identifier.indexOf("_&");
        if (fallbackIndex > 0) {
            return identifier.substring(fallbackIndex + 2);
        }
        return "";
    }
    
    private String parseTopPlaceholder(CaptureSession session, String identifier) {
        // Проверяем наличие fallback
        String fallback = "";
        String cleanIdentifier = identifier;
        int fallbackIndex = identifier.indexOf("_&");
        if (fallbackIndex > 0) {
            fallback = identifier.substring(fallbackIndex + 2);
            cleanIdentifier = identifier.substring(0, fallbackIndex);
        }
        
        String[] parts = cleanIdentifier.split("_");
        if (parts.length < 3) {
            return fallback.isEmpty() ? "0" : fallback;
        }
        
        try {
            int position = Integer.parseInt(parts[1]);
            String type = parts[2];
            
            // Получаем топ игроков
            List<Map.Entry<UUID, PlayerData>> sorted = session.getPlayers().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getContribution(), e1.getValue().getContribution()))
                .toList();
            
            if (position > sorted.size()) {
                return fallback.isEmpty() ? "0" : fallback;
            }
            
            Map.Entry<UUID, PlayerData> entry = sorted.get(position - 1);
            Player topPlayer = plugin.getServer().getPlayer(entry.getKey());
            
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
    
    private String parseParticipantsPlaceholder(CaptureSession session, String identifier) {
        // Проверяем наличие fallback
        String fallback = "";
        String cleanIdentifier = identifier;
        int fallbackIndex = identifier.indexOf("_&");
        if (fallbackIndex > 0) {
            fallback = identifier.substring(fallbackIndex + 2);
            cleanIdentifier = identifier.substring(0, fallbackIndex);
        }
        
        String[] parts = cleanIdentifier.split("_", 2);
        String separator = parts.length > 1 ? parts[1] : " ";
        
        // Обработка цветных разделителей
        if (separator.startsWith("&") || separator.startsWith("#") || separator.startsWith("<")) {
            // Это цветной разделитель, оставляем как есть
        } else if (separator.isEmpty() || separator.equals("_")) {
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
