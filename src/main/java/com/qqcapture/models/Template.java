package com.qqcapture.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;

import java.util.*;

public class Template {
    private final String name;
    
    // BossBar settings
    private final boolean bossBarEnabled;
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
    
    // ===== ЗОНЫ (НОВОЕ) =====
    private final Map<Integer, Zone> zones;
    
    // Region
    private final String regionName;
    private final String regionFlags;
    
    // Commands
    private final List<String> startCommands;
    private final List<String> tickCommands;
    private final List<String> endCommands;
    
    // Max duration in seconds (0 = no limit)
    private final int maxDuration;
    
    // Top storage settings
    private final boolean topStorageEnabled;
    private final int topStorageDuration;
    private final boolean topAutoClearOnStart;
    
    // ===== ВНУТРЕННИЙ КЛАСС ДЛЯ ЗОНЫ =====
    public static class Zone {
        private final Location pos1;
        private final Location pos2;
        
        public Zone(Location pos1, Location pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
        }
        
        public Location getPos1() { return pos1; }
        public Location getPos2() { return pos2; }
        
        public boolean isInZone(Location loc) {
            if (loc == null || pos1 == null || pos2 == null || pos1.getWorld() == null) {
                return false;
            }
            double minX = Math.min(pos1.getX(), pos2.getX());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());
            
            return loc.getX() >= minX && loc.getX() <= maxX &&
                   loc.getY() >= minY && loc.getY() <= maxY &&
                   loc.getZ() >= minZ && loc.getZ() <= maxZ;
        }
    }
    
    private Template(Builder builder) {
        this.name = builder.name;
        this.bossBarEnabled = builder.bossBarEnabled;
        this.bossBarColor = builder.bossBarColor;
        this.bossBarUpdateTicks = builder.bossBarUpdateTicks;
        this.startText = builder.startText;
        this.progressText = builder.progressText;
        this.endText = builder.endText;
        this.segments = builder.segments;
        this.updateTicks = builder.updateTicks;
        this.startDelay = builder.startDelay;
        this.endDelay = builder.endDelay;
        this.timerFormat = builder.timerFormat;
        this.sendOnRejoin = builder.sendOnRejoin;
        this.bossBarText = builder.bossBarText;
        this.condition = builder.condition;
        this.allPlayersCondition = builder.allPlayersCondition;
        this.rules = builder.rules;
        this.permission = builder.permission;
        this.minPlayers = builder.minPlayers;
        this.maxPlayers = builder.maxPlayers;
        this.needAmount = builder.needAmount;
        this.tickCapture = builder.tickCapture;
        this.multiplier = builder.multiplier;
        this.teamMultiplierType = builder.teamMultiplierType;
        this.teamMultiplier = builder.teamMultiplier;
        this.zones = builder.zones;
        this.regionName = builder.regionName;
        this.regionFlags = builder.regionFlags;
        this.startCommands = builder.startCommands;
        this.tickCommands = builder.tickCommands;
        this.endCommands = builder.endCommands;
        this.maxDuration = builder.maxDuration;
        this.topStorageEnabled = builder.topStorageEnabled;
        this.topStorageDuration = builder.topStorageDuration;
        this.topAutoClearOnStart = builder.topAutoClearOnStart;
    }
    
    // Getters
    public String getName() { return name; }
    public boolean isBossBarEnabled() { return bossBarEnabled; }
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
    public Map<Integer, Zone> getZones() { return zones; }
    public String getRegionName() { return regionName; }
    public String getRegionFlags() { return regionFlags; }
    public List<String> getStartCommands() { return startCommands; }
    public List<String> getTickCommands() { return tickCommands; }
    public List<String> getEndCommands() { return endCommands; }
    public int getMaxDuration() { return maxDuration; }
    public boolean isTopStorageEnabled() { return topStorageEnabled; }
    public int getTopStorageDuration() { return topStorageDuration; }
    public boolean isTopAutoClearOnStart() { return topAutoClearOnStart; }
    
    // ===== ПРОВЕРКА В ЛЮБОЙ ЗОНЕ =====
    public boolean isInAnyZone(Location loc) {
        for (Zone zone : zones.values()) {
            if (zone.isInZone(loc)) {
                return true;
            }
        }
        return false;
    }
    
    // ===== ПОЛУЧЕНИЕ ВСЕХ ИГРОКОВ В ЗОНАХ =====
    public List<Player> getPlayersInZones() {
        List<Player> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInAnyZone(player.getLocation())) {
                result.add(player);
            }
        }
        return result;
    }
    
    // Helper methods
    public boolean hasValidPositions() {
        if (zones.isEmpty()) return false;
        for (Zone zone : zones.values()) {
            if (zone.getPos1() == null || zone.getPos2() == null || 
                zone.getPos1().getWorld() == null || zone.getPos2().getWorld() == null) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isRegionEnabled() {
        return regionName != null && !regionName.isEmpty();
    }
    
    public boolean hasStartCommands() {
        return startCommands != null && !startCommands.isEmpty();
    }
    
    public boolean hasTickCommands() {
        return tickCommands != null && !tickCommands.isEmpty();
    }
    
    public boolean hasEndCommands() {
        return endCommands != null && !endCommands.isEmpty();
    }
    
    public boolean hasRules() {
        return rules != null && !rules.isEmpty();
    }
    
    public boolean hasCondition() {
        return condition != null && !condition.isEmpty();
    }
    
    public boolean hasAllPlayersCondition() {
        return allPlayersCondition != null && !allPlayersCondition.isEmpty();
    }
    
    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }
    
    public boolean hasTeamMultiplier() {
        return teamMultiplier > 0;
    }
    
    public boolean hasMultiplier() {
        return multiplier > 0;
    }
    
    public boolean hasMaxDuration() {
        return maxDuration > 0;
    }
    
    @Override
    public String toString() {
        return "Template{" +
               "name='" + name + '\'' +
               ", needAmount=" + needAmount +
               ", minPlayers=" + minPlayers +
               ", maxPlayers=" + maxPlayers +
               ", zones=" + zones.size() +
               ", regionName='" + regionName + '\'' +
               ", startCommands=" + (startCommands != null ? startCommands.size() : 0) +
               ", tickCommands=" + (tickCommands != null ? tickCommands.size() : 0) +
               ", endCommands=" + (endCommands != null ? endCommands.size() : 0) +
               ", maxDuration=" + maxDuration +
               ", topStorageEnabled=" + topStorageEnabled +
               ", topStorageDuration=" + topStorageDuration +
               ", topAutoClearOnStart=" + topAutoClearOnStart +
               '}';
    }
    
    // Builder Pattern
    public static class Builder {
        private final String name;
        private boolean bossBarEnabled = true;
        private String bossBarColor = "GREEN";
        private int bossBarUpdateTicks = 20;
        private String startText = "<gradient:#00FF00:#55FF55>Ивент начался!</gradient>";
        private String progressText = "<gradient:#FF0000:#FFAA00>Прогресс: %current%/%max%</gradient>";
        private String endText = "<gradient:#FF5555:#FF0000>Ивент завершился!</gradient>";
        private int segments = 12;
        private int updateTicks = 5;
        private int startDelay = 3;
        private int endDelay = 3;
        private String timerFormat = "mm:ss";
        private boolean sendOnRejoin = true;
        private String bossBarText = "Ивент: %current%/%max% (Участников: %players% Групп: %groups%)";
        private String condition = "";
        private String allPlayersCondition = "";
        private Map<String, Map<String, String>> rules = new LinkedHashMap<>();
        private String permission = "";
        private int minPlayers = 2;
        private int maxPlayers = 30;
        private int needAmount = 100000;
        private int tickCapture = 120;
        private double multiplier = 0.00001;
        private String teamMultiplierType = "individual";
        private double teamMultiplier = 0.0;
        private Map<Integer, Zone> zones = new LinkedHashMap<>();
        private String regionName = "";
        private String regionFlags = "";
        private List<String> startCommands = new ArrayList<>();
        private List<String> tickCommands = new ArrayList<>();
        private List<String> endCommands = new ArrayList<>();
        private int maxDuration = 0;
        private boolean topStorageEnabled = true;
        private int topStorageDuration = 300;
        private boolean topAutoClearOnStart = true;
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder bossBarEnabled(boolean bossBarEnabled) {
            this.bossBarEnabled = bossBarEnabled;
            return this;
        }
        
        public Builder bossBarColor(String bossBarColor) {
            this.bossBarColor = bossBarColor;
            return this;
        }
        
        public Builder bossBarUpdateTicks(int bossBarUpdateTicks) {
            this.bossBarUpdateTicks = bossBarUpdateTicks;
            return this;
        }
        
        public Builder startText(String startText) {
            this.startText = startText;
            return this;
        }
        
        public Builder progressText(String progressText) {
            this.progressText = progressText;
            return this;
        }
        
        public Builder endText(String endText) {
            this.endText = endText;
            return this;
        }
        
        public Builder segments(int segments) {
            this.segments = segments;
            return this;
        }
        
        public Builder updateTicks(int updateTicks) {
            this.updateTicks = updateTicks;
            return this;
        }
        
        public Builder startDelay(int startDelay) {
            this.startDelay = startDelay;
            return this;
        }
        
        public Builder endDelay(int endDelay) {
            this.endDelay = endDelay;
            return this;
        }
        
        public Builder timerFormat(String timerFormat) {
            this.timerFormat = timerFormat;
            return this;
        }
        
        public Builder sendOnRejoin(boolean sendOnRejoin) {
            this.sendOnRejoin = sendOnRejoin;
            return this;
        }
        
        public Builder bossBarText(String bossBarText) {
            this.bossBarText = bossBarText;
            return this;
        }
        
        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }
        
        public Builder allPlayersCondition(String allPlayersCondition) {
            this.allPlayersCondition = allPlayersCondition;
            return this;
        }
        
        public Builder rules(Map<String, Map<String, String>> rules) {
            this.rules = rules;
            return this;
        }
        
        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }
        
        public Builder minPlayers(int minPlayers) {
            this.minPlayers = minPlayers;
            return this;
        }
        
        public Builder maxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
            return this;
        }
        
        public Builder needAmount(int needAmount) {
            this.needAmount = needAmount;
            return this;
        }
        
        public Builder tickCapture(int tickCapture) {
            this.tickCapture = tickCapture;
            return this;
        }
        
        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }
        
        public Builder teamMultiplierType(String teamMultiplierType) {
            this.teamMultiplierType = teamMultiplierType;
            return this;
        }
        
        public Builder teamMultiplier(double teamMultiplier) {
            this.teamMultiplier = teamMultiplier;
            return this;
        }
        
        public Builder zones(Map<Integer, Zone> zones) {
            this.zones = zones;
            return this;
        }
        
        public Builder regionName(String regionName) {
            this.regionName = regionName;
            return this;
        }
        
        public Builder regionFlags(String regionFlags) {
            this.regionFlags = regionFlags;
            return this;
        }
        
        public Builder startCommands(List<String> startCommands) {
            this.startCommands = startCommands;
            return this;
        }
        
        public Builder tickCommands(List<String> tickCommands) {
            this.tickCommands = tickCommands;
            return this;
        }
        
        public Builder endCommands(List<String> endCommands) {
            this.endCommands = endCommands;
            return this;
        }
        
        public Builder maxDuration(int maxDuration) {
            this.maxDuration = maxDuration;
            return this;
        }
        
        public Builder topStorageEnabled(boolean topStorageEnabled) {
            this.topStorageEnabled = topStorageEnabled;
            return this;
        }
        
        public Builder topStorageDuration(int topStorageDuration) {
            this.topStorageDuration = topStorageDuration;
            return this;
        }
        
        public Builder topAutoClearOnStart(boolean topAutoClearOnStart) {
            this.topAutoClearOnStart = topAutoClearOnStart;
            return this;
        }
        
        public Template build() {
            // Если зоны пустые — создаем дефолтную
            if (zones.isEmpty()) {
                World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                Location defaultPos1 = new Location(defaultWorld, 0, 0, 0);
                Location defaultPos2 = new Location(defaultWorld, 0, -1, 0);
                zones.put(1, new Zone(defaultPos1, defaultPos2));
            }
            
            // Устанавливаем мир для всех зон
            for (Zone zone : zones.values()) {
                if (zone.getPos1() != null && zone.getPos1().getWorld() == null) {
                    World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                    if (defaultWorld != null) {
                        zone.getPos1().setWorld(defaultWorld);
                    }
                }
                if (zone.getPos2() != null && zone.getPos2().getWorld() == null) {
                    World defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                    if (defaultWorld != null) {
                        zone.getPos2().setWorld(defaultWorld);
                    }
                }
            }
            
            return new Template(this);
        }
    }
}
