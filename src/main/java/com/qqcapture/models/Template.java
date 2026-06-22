package com.qqcapture.models;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Template {
    private final String name;
    
    // BossBar settings
    private final String bossBarColor;
    private final int bossBarUpdateTicks;
    private final String startText;
    private final String progressText;
    private final String endText;
    private final int segments;
    private final int updateTicks;
    private final int startDelay;
    private final int endDelay;
    private final String timerFormat;
    private final boolean sendOnRejoin;
    private final String bossBarText;
    
    // Conditions
    private final String condition;
    private final String allPlayersCondition;
    private final Map<String, Map<String, String>> rules;
    
    // Permissions
    private final String permission;
    
    // Player limits
    private final int minPlayers;
    private final int maxPlayers;
    
    // Capture settings
    private final int needAmount;
    private final int tickCapture;
    private final double multiplier;
    private final String teamMultiplierType;
    private final double teamMultiplier;
    
    // Region
    private final Location pos1;
    private final Location pos2;
    private final String regionName;
    private final String regionFlags;
    
    // Commands
    private final boolean tickCommand;
    private final List<String> commands;
    
    public Template(String name, ConfigurationSection section) {
        this.name = name;
        
        // BossBar section
        ConfigurationSection bossBarSection = section.getConfigurationSection("bossbar");
        if (bossBarSection != null) {
            this.bossBarColor = bossBarSection.getString("color", "GREEN");
            this.bossBarUpdateTicks = bossBarSection.getInt("обновлять-боссбар-каждые-N-тиков", 20);
            this.startText = bossBarSection.getString("start-text", "<gradient:#00FF00:#55FF55>Ивент начался!</gradient>");
            this.progressText = bossBarSection.getString("progress-text", "<gradient:#FF0000:#FFAA00>Прогресс: %current%/%max%</gradient>");
            this.endText = bossBarSection.getString("end-text", "<gradient:#FF5555:#FF0000>Ивент завершился!</gradient>");
            this.segments = bossBarSection.getInt("segments", 12);
            this.updateTicks = bossBarSection.getInt("update-ticks", 5);
            this.startDelay = bossBarSection.getInt("start-delay", 3);
            this.endDelay = bossBarSection.getInt("end-delay", 3);
            this.timerFormat = bossBarSection.getString("timer-format", "mm:ss");
            this.sendOnRejoin = bossBarSection.getBoolean("отправлять-боссбар-когда-игрок-перезаходит-на-сервер", true);
            this.bossBarText = bossBarSection.getString("текст-боссбара", "Ивент: %current%/%max% (Участников: %players% Групп: %groups%)");
        } else {
            // Default values
            this.bossBarColor = "GREEN";
            this.bossBarUpdateTicks = 20;
            this.startText = "<gradient:#00FF00:#55FF55>Ивент начался!</gradient>";
            this.progressText = "<gradient:#FF0000:#FFAA00>Прогресс: %current%/%max%</gradient>";
            this.endText = "<gradient:#FF5555:#FF0000>Ивент завершился!</gradient>";
            this.segments = 12;
            this.updateTicks = 5;
            this.startDelay = 3;
            this.endDelay = 3;
            this.timerFormat = "mm:ss";
            this.sendOnRejoin = true;
            this.bossBarText = "Ивент: %current%/%max% (Участников: %players% Групп: %groups%)";
        }
        
        // Conditions
        this.condition = section.getString("condition", "");
        this.allPlayersCondition = section.getString("all-players-condition", "");
        
        // Rules
        this.rules = parseRules(section);
        
        // Permission
        this.permission = section.getString("permission", "");
        
        // Player limits
        this.minPlayers = section.getInt("min-players", 2);
        this.maxPlayers = section.getInt("max-players", 30);
        
        // Capture settings
        this.needAmount = section.getInt("need-amount", 100000);
        this.tickCapture = section.getInt("tick-capture", 120);
        this.multiplier = section.getDouble("multiplier", 0.00001);
        this.teamMultiplierType = section.getString("type-team-multiplier", "индивидуально");
        this.teamMultiplier = section.getDouble("team-multiplier", 0.0);
        
        // Region
        this.pos1 = parseLocation(section.getString("pos1", "0,0,0"));
        this.pos2 = parseLocation(section.getString("pos2", "0,-1,0"));
        this.regionName = section.getString("region-name", "");
        this.regionFlags = section.getString("region-flags", "");
        
        // Commands
        this.tickCommand = section.getBoolean("tick-command", false);
        this.commands = section.getStringList("commands");
        if (this.commands == null) {
            this.commands = new ArrayList<>();
        }
    }
    
    private Location parseLocation(String str) {
        try {
            String[] parts = str.split(",");
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                return new Location(null, x, y, z);
            }
        } catch (Exception e) {
            // Return default location
        }
        return new Location(null, 0, -1, 0);
    }
    
    private Map<String, Map<String, String>> parseRules(ConfigurationSection section) {
        Map<String, Map<String, String>> rules = new java.util.HashMap<>();
        ConfigurationSection rulesSection = section.getConfigurationSection("rules");
        
        if (rulesSection != null) {
            for (String key : rulesSection.getKeys(false)) {
                Map<String, String> rule = new java.util.HashMap<>();
                String value = rulesSection.getString(key);
                
                if (value != null && value.contains(":")) {
                    String[] parts = value.split(":", 2);
                    if (parts.length == 2) {
                        rule.put("type", parts[0].trim());
                        rule.put("value", parts[1].trim());
                    }
                }
                rules.put(key, rule);
            }
        }
        
        return rules;
    }
    
    // Getters
    public String getName() { return name; }
    public String getBossBarColor() { return bossBarColor; }
    public int getBossBarUpdateTicks() { return bossBarUpdateTicks; }
    public String getStartText() { return startText; }
    public String getProgressText() { return progressText; }
    public String getEndText() { return endText; }
    public int getSegments() { return segments; }
    public int getUpdateTicks() { return updateTicks; }
    public int getStartDelay() { return startDelay; }
    public int getEndDelay() { return endDelay; }
    public String getTimerFormat() { return timerFormat; }
    public boolean isSendOnRejoin() { return sendOnRejoin; }
    public String getBossBarText() { return bossBarText; }
    public String getCondition() { return condition; }
    public String getAllPlayersCondition() { return allPlayersCondition; }
    public Map<String, Map<String, String>> getRules() { return rules; }
    public String getPermission() { return permission; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getNeedAmount() { return needAmount; }
    public int getTickCapture() { return tickCapture; }
    public double getMultiplier() { return multiplier; }
    public String getTeamMultiplierType() { return teamMultiplierType; }
    public double getTeamMultiplier() { return teamMultiplier; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public String getRegionName() { return regionName; }
    public String getRegionFlags() { return regionFlags; }
    public boolean isTickCommand() { return tickCommand; }
    public List<String> getCommands() { return commands; }
}
