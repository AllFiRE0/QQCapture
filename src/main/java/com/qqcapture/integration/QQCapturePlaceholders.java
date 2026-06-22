package com.qqcapture.integration;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

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
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }
        
        // Check if player is in session
        CaptureSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session == null) {
            return "";
        }
        
        // Parse identifier
        if (identifier.startsWith("session_")) {
            String[] parts = identifier.split("_", 2);
            if (parts.length == 2) {
                String key = parts[1];
                PlayerData data = session.getPlayers().get(player.getUniqueId());
                
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
        
        // Session info
        if (identifier.equals("current")) {
            return String.valueOf(session.getCurrentPoints());
        } else if (identifier.equals("max")) {
            return String.valueOf(session.getTargetPoints());
        } else if (identifier.equals("progress")) {
            return String.format("%.1f", (double) session.getCurrentPoints() / session.getTargetPoints() * 100);
        } else if (identifier.equals("players")) {
            return String.valueOf(session.getPlayers().size());
        } else if (identifier.equals("template")) {
            return session.getTemplate().getName();
        } else if (identifier.startsWith("top_")) {
            return parseTopPlaceholder(session, identifier);
        }
        
        return null;
    }
    
    private String parseTopPlaceholder(CaptureSession session, String identifier) {
        String[] parts = identifier.split("_");
        if (parts.length < 3) {
            return "";
        }
        
        try {
            int position = Integer.parseInt(parts[1]);
            String type = parts[2];
            
            // Get top players
            List<Map.Entry<UUID, PlayerData>> sorted = session.getPlayers().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getContribution(), e1.getValue().getContribution()))
                .toList();
            
            if (position > sorted.size()) {
                return "";
            }
            
            Map.Entry<UUID, PlayerData> entry = sorted.get(position - 1);
            Player topPlayer = plugin.getServer().getPlayer(entry.getKey());
            
            if (topPlayer == null) {
                return "";
            }
            
            if (type.equals("name")) {
                return topPlayer.getName();
            } else if (type.equals("value")) {
                return String.valueOf(entry.getValue().getContribution());
            } else if (type.equals("points")) {
                return String.valueOf(entry.getValue().getContribution());
            }
            
        } catch (NumberFormatException e) {
            // Invalid position
        }
        
        return "";
    }
}
