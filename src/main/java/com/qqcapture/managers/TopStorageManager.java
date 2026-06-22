package com.qqcapture.managers;

import com.qqcapture.QQCapture;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopStorageManager {
    private final QQCapture plugin;
    private final File topFile;
    private final Map<String, List<TopEntry>> topCache;
    private final Map<String, Long> expiryTime;
    
    public TopStorageManager(QQCapture plugin) {
        this.plugin = plugin;
        this.topFile = new File(plugin.getDataFolder(), "top_data.yml");
        this.topCache = new ConcurrentHashMap<>();
        this.expiryTime = new ConcurrentHashMap<>();
        loadTopData();
    }
    
    public static class TopEntry {
        private final String playerName;
        private final UUID playerUUID;
        private final int points;
        private final long timestamp;
        
        public TopEntry(String playerName, UUID playerUUID, int points) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.points = points;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getPlayerName() { return playerName; }
        public UUID getPlayerUUID() { return playerUUID; }
        public int getPoints() { return points; }
        public long getTimestamp() { return timestamp; }
    }
    
    private boolean isExpired(String templateName) {
        if (!expiryTime.containsKey(templateName)) {
            return false;
        }
        return System.currentTimeMillis() > expiryTime.get(templateName);
    }
    
    public void setExpiry(String templateName, int durationSeconds) {
        if (durationSeconds > 0) {
            expiryTime.put(templateName, System.currentTimeMillis() + (durationSeconds * 1000L));
        } else {
            expiryTime.remove(templateName);
        }
    }
    
    public void saveTop(String templateName, List<TopEntry> entries, int durationSeconds) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(topFile);
        
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (TopEntry entry : entries) {
            Map<String, Object> data = new HashMap<>();
            data.put("playerName", entry.getPlayerName());
            data.put("playerUUID", entry.getPlayerUUID().toString());
            data.put("points", entry.getPoints());
            data.put("timestamp", entry.getTimestamp());
            serialized.add(data);
        }
        
        config.set(templateName + ".entries", serialized);
        config.set(templateName + ".savedAt", System.currentTimeMillis());
        config.set(templateName + ".duration", durationSeconds);
        
        try {
            config.save(topFile);
            topCache.put(templateName, entries);
            setExpiry(templateName, durationSeconds);
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Top data saved for template: " + templateName + 
                    " (" + entries.size() + " entries, expires in " + durationSeconds + "s)");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save top data for template: " + templateName);
            e.printStackTrace();
        }
    }
    
    private void loadTopData() {
        if (!topFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(topFile);
            
            for (String templateName : config.getKeys(false)) {
                int duration = config.getInt(templateName + ".duration", 300);
                long savedAt = config.getLong(templateName + ".savedAt", 0);
                
                if (duration > 0 && System.currentTimeMillis() - savedAt > duration * 1000L) {
                    config.set(templateName, null);
                    config.save(topFile);
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("Top data for " + templateName + " expired and was removed");
                    }
                    continue;
                }
                
                List<Map<?, ?>> rawEntries = config.getMapList(templateName + ".entries");
                List<TopEntry> entries = new ArrayList<>();
                
                for (Map<?, ?> raw : rawEntries) {
                    String playerName = (String) raw.get("playerName");
                    String uuidStr = (String) raw.get("playerUUID");
                    int points = (int) raw.get("points");
                    long timestamp = raw.containsKey("timestamp") ? (long) raw.get("timestamp") : System.currentTimeMillis();
                    
                    UUID uuid = UUID.fromString(uuidStr);
                    entries.add(new TopEntry(playerName, uuid, points));
                }
                
                entries.sort((e1, e2) -> Integer.compare(e2.getPoints(), e1.getPoints()));
                topCache.put(templateName, entries);
                setExpiry(templateName, duration);
                
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Loaded top data for template: " + templateName + 
                        " (" + entries.size() + " entries, expires in " + duration + "s)");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load top data!");
            e.printStackTrace();
        }
    }
    
    public List<TopEntry> getTop(String templateName) {
        if (isExpired(templateName)) {
            clearTop(templateName);
            return Collections.emptyList();
        }
        
        if (topCache.containsKey(templateName)) {
            return topCache.get(templateName);
        }
        
        loadTopData();
        return topCache.getOrDefault(templateName, Collections.emptyList());
    }
    
    public TopEntry getTopPlayer(String templateName, int position) {
        List<TopEntry> entries = getTop(templateName);
        if (position <= 0 || position > entries.size()) {
            return null;
        }
        return entries.get(position - 1);
    }
    
    public void clearTop(String templateName) {
        topCache.remove(templateName);
        expiryTime.remove(templateName);
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(topFile);
        config.set(templateName, null);
        try {
            config.save(topFile);
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Top data cleared for template: " + templateName);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to clear top data for template: " + templateName);
        }
    }
    
    public boolean removePlayerFromTop(String templateName, String playerName) {
        List<TopEntry> entries = getTop(templateName);
        if (entries.isEmpty()) {
            return false;
        }
        
        boolean removed = entries.removeIf(entry -> 
            entry.getPlayerName().equalsIgnoreCase(playerName));
        
        if (removed) {
            int duration = getDuration(templateName);
            saveTop(templateName, entries, duration);
        }
        return removed;
    }
    
    private int getDuration(String templateName) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(topFile);
        return config.getInt(templateName + ".duration", 300);
    }
}
