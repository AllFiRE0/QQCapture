package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import com.qqcapture.models.CaptureSession;
import com.qqcapture.models.Template;
import com.qqcapture.utils.ColorUtils;
import com.qqcapture.utils.TimerUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {
    private final QQCapture plugin;
    private final Map<String, BossBar> activeBossBars;
    private final Map<UUID, String> playerBossBars;
    
    public BossBarManager(QQCapture plugin) {
        this.plugin = plugin;
        this.activeBossBars = new HashMap<>();
        this.playerBossBars = new HashMap<>();
    }
    
    public void showBossBar(Player player, CaptureSession session) {
        String sessionId = session.getSessionId();
        BossBar bossBar = activeBossBars.get(sessionId);
        
        if (bossBar == null) {
            bossBar = createBossBar(session);
            activeBossBars.put(sessionId, bossBar);
        }
        
        bossBar.addPlayer(player);
        playerBossBars.put(player.getUniqueId(), sessionId);
    }
    
    public void hideBossBar(Player player, CaptureSession session) {
        String sessionId = session.getSessionId();
        BossBar bossBar = activeBossBars.get(sessionId);
        
        if (bossBar != null) {
            bossBar.removePlayer(player);
            playerBossBars.remove(player.getUniqueId());
        }
    }
    
    public void updateBossBar(CaptureSession session) {
        String sessionId = session.getSessionId();
        BossBar bossBar = activeBossBars.get(sessionId);
        
        if (bossBar == null) {
            bossBar = createBossBar(session);
            activeBossBars.put(sessionId, bossBar);
        }
        
        Template template = session.getTemplate();
        int current = session.getCurrentPoints();
        int target = session.getTargetPoints();
        double progress = (double) current / target;
        
        // Update boss bar text
        String text = template.getBossBarText();
        text = applyPlaceholders(text, session, null);
        bossBar.setTitle(ColorUtils.colorize(text));
        
        // Update progress
        bossBar.setProgress(Math.min(progress, 1.0));
        
        // Update color based on progress
        if (progress < 0.3) {
            bossBar.setColor(BarColor.RED);
        } else if (progress < 0.6) {
            bossBar.setColor(BarColor.YELLOW);
        } else if (progress < 0.9) {
            bossBar.setColor(BarColor.GREEN);
        } else {
            bossBar.setColor(BarColor.BLUE);
        }
    }
    
    private BossBar createBossBar(CaptureSession session) {
        Template template = session.getTemplate();
        String title = template.getBossBarText();
        title = applyPlaceholders(title, session, null);
        
        BarColor color = getBarColor(template.getBossBarColor());
        BossBar bossBar = Bukkit.createBossBar(
            ColorUtils.colorize(title),
            color,
            BarStyle.SEGMENTED_12
        );
        
        // Set segments
        int segments = template.getSegments();
        bossBar.setStyle(getBarStyle(segments));
        
        return bossBar;
    }
    
    private BarColor getBarColor(String colorName) {
        try {
            return BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }
    
    private BarStyle getBarStyle(int segments) {
        switch (segments) {
            case 1: return BarStyle.SOLID;
            case 6: return BarStyle.SEGMENTED_6;
            case 10: return BarStyle.SEGMENTED_10;
            case 12: return BarStyle.SEGMENTED_12;
            case 20: return BarStyle.SEGMENTED_20;
            default: return BarStyle.SEGMENTED_12;
        }
    }
    
    private String applyPlaceholders(String text, CaptureSession session, Player player) {
        Template template = session.getTemplate();
        
        // Current points
        text = text.replace("%current%", String.valueOf(session.getCurrentPoints()));
        text = text.replace("%max%", String.valueOf(session.getTargetPoints()));
        text = text.replace("%progress%", String.format("%.1f", 
            (double) session.getCurrentPoints() / session.getTargetPoints() * 100));
        
        // Players count
        int playerCount = session.getPlayers().size();
        text = text.replace("%players%", String.valueOf(playerCount));
        
        // Groups count
        int groups = playerCount > 0 ? 1 : 0; // Simplified
        text = text.replace("%groups%", String.valueOf(groups));
        
        // Time
        long elapsed = System.currentTimeMillis() - session.getStartTime();
        text = text.replace("%time%", TimerUtils.formatTime(elapsed, template.getTimerFormat()));
        
        // Template name
        text = text.replace("%template%", template.getName());
        
        // Player specific placeholders
        if (player != null) {
            text = text.replace("%player%", player.getName());
            // Add more player-specific placeholders
        }
        
        // Process custom placeholders through PlaceholderAPI if available
        if (plugin.getPlaceholderAPIHook() != null && player != null) {
            text = plugin.getPlaceholderAPIHook().parsePlaceholders(player, text);
        }
        
        return text;
    }
    
    public void removeBossBar(String sessionId) {
        BossBar bossBar = activeBossBars.remove(sessionId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
    
    public void clearAllBossBars() {
        for (BossBar bossBar : activeBossBars.values()) {
            bossBar.removeAll();
        }
        activeBossBars.clear();
        playerBossBars.clear();
    }
    
    public BossBar getBossBar(String sessionId) {
        return activeBossBars.get(sessionId);
    }
}
